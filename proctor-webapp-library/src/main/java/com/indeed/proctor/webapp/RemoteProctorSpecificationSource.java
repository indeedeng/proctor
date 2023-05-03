package com.indeed.proctor.webapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.common.Serializers;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.ProctorReader;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.model.AppVersion;
import com.indeed.proctor.webapp.model.ProctorClientApplication;
import com.indeed.proctor.webapp.model.ProctorSpecifications;
import com.indeed.proctor.webapp.model.RemoteSpecificationResult;
import com.indeed.proctor.webapp.util.VarExportedVariablesDeserializer;
import com.indeed.proctor.webapp.util.threads.LogOnUncaughtExceptionHandler;
import com.indeed.util.core.DataLoadingTimerTask;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

/**
 * Regularly reloads specifications from applications where proctor client is deployed.
 */
public class RemoteProctorSpecificationSource extends DataLoadingTimerTask implements ProctorSpecificationSource {
    private static final Logger LOGGER = LogManager.getLogger(RemoteProctorSpecificationSource.class);

    private static final ObjectMapper OBJECT_MAPPER = Serializers.strict();

    @Autowired(required = false)
    private final ProctorClientSource clientSource = new DefaultClientSource();

    private final ExecutorService httpExecutor;

    private final Map<Environment, ImmutableMap<AppVersion, RemoteSpecificationResult>> applicationMapByEnvironment =
            Maps.newConcurrentMap();

    private final Map<Environment, ProctorReader> proctorReaderMap;

    private final HttpClient httpClient;

    public RemoteProctorSpecificationSource(final int httpTimeoutMillis,
                                            final int executorThreads,
                                            final ProctorReader trunk,
                                            final ProctorReader qa,
                                            final ProctorReader production) {
        super(RemoteProctorSpecificationSource.class.getSimpleName());
        Preconditions.checkArgument(httpTimeoutMillis > 0, "httpTimeoutMillis > 0");

        this.httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(
                        RequestConfig.custom()
                                .setConnectTimeout(httpTimeoutMillis)
                                .setSocketTimeout(httpTimeoutMillis)
                                .build()
                )
                .disableCookieManagement() // to make it stateless
                .build();

        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("proctor-specification-source-Thread-%d")
                .setUncaughtExceptionHandler(new LogOnUncaughtExceptionHandler())
                .build();
        this.httpExecutor = Executors.newFixedThreadPool(executorThreads, threadFactory);
        this.proctorReaderMap = ImmutableMap.of(
                Environment.WORKING, trunk,
                Environment.QA, qa,
                Environment.PRODUCTION, production
        );
    }

    @Override
    public RemoteSpecificationResult getRemoteResult(final Environment environment,
                                                     final AppVersion version) {
        return Optional.ofNullable(applicationMapByEnvironment.get(environment))
                .map(applicationMap -> applicationMap.get(version))
                .orElseGet(() -> RemoteSpecificationResult
                        .failures(version, Collections.emptyMap())
                );
    }

    @Override
    public Map<AppVersion, RemoteSpecificationResult> loadAllSpecifications(final Environment environment) {
        final ImmutableMap<AppVersion, RemoteSpecificationResult> applicationsMap =
                applicationMapByEnvironment.get(environment);
        if (applicationsMap == null) {
            return Collections.emptyMap();
        }

        return applicationsMap;
    }

    @Override
    public Map<AppVersion, ProctorSpecifications> loadAllSuccessfulSpecifications(final Environment environment) {
        final ImmutableMap<AppVersion, RemoteSpecificationResult> applicationsMap =
                applicationMapByEnvironment.get(environment);
        if (applicationsMap == null) {
            return Collections.emptyMap();
        }

        return applicationsMap.entrySet().stream()
                .filter(e -> e.getValue().getSpecifications() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().getSpecifications()
                ));
    }

    @Override
    public Set<AppVersion> activeClients(final Environment environment, final String testName) {
        final Map<AppVersion, RemoteSpecificationResult> applicationsMap =
                applicationMapByEnvironment.get(environment);
        if (applicationsMap == null) {
            return Collections.emptySet();
        }

        final ConsumableTestDefinition testDefinition = getCurrentConsumableTestDefinition(environment, testName);
        if (testDefinition == null) {
            return Collections.emptySet();
        }

        final Map<String, ConsumableTestDefinition> singleTestMatrix
                = ImmutableMap.of(testName, testDefinition);

        return applicationsMap.entrySet().stream()
                .filter(e -> e.getValue().getSpecifications() != null)
                .filter(e -> e.getValue().getSpecifications()
                        .getResolvedTests(singleTestMatrix)
                        .contains(testName)
                )
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> activeTests(final Environment environment) {
        final Map<AppVersion, RemoteSpecificationResult> applicationsMap =
                applicationMapByEnvironment.get(environment);
        if (applicationsMap == null) {
            return Collections.emptySet();
        }

        final TestMatrixArtifact testMatrixArtifact = getCurrentTestMatrixArtifact(environment);
        Preconditions.checkNotNull(testMatrixArtifact,
                "Failed to get the current test matrix artifact from Envirronment " + environment);
        final Map<String, ConsumableTestDefinition> definedTests = testMatrixArtifact.getTests();

        return applicationsMap.values().stream()
                .map(RemoteSpecificationResult::getSpecifications)
                .filter(Objects::nonNull)
                .flatMap(s -> s.getResolvedTests(definedTests).stream())
                .collect(Collectors.toSet());
    }

    @Override
    public boolean load() {
        final boolean success = refreshInternalCache();
        if (success) {
            setDataVersion(new Date().toString());
        }
        return success;
    }

    private boolean refreshInternalCache() {
        // TODO (parker) 9/6/12 - run all of these in parallel instead of in series
        final boolean devSuccess = refreshInternalCache(Environment.WORKING);
        final boolean qaSuccess = refreshInternalCache(Environment.QA);
        final boolean productionSuccess = refreshInternalCache(Environment.PRODUCTION);
        return devSuccess && qaSuccess && productionSuccess;
    }

    private boolean refreshInternalCache(final Environment environment) {
        LOGGER.info("Refreshing internal list of ProctorSpecifications for environment " + environment);

        final List<ProctorClientApplication> clients = clientSource.loadClients(environment);

        final Map<AppVersion, Future<RemoteSpecificationResult>> futures = Maps.newLinkedHashMap();

        final ImmutableMap.Builder<AppVersion, RemoteSpecificationResult> allResults = ImmutableMap.builder();
        final Set<AppVersion> appVersionsToCheck = Sets.newLinkedHashSet();

        // Accumulate all clients that have equivalent AppVersion (APPLICATION_COMPARATOR)
        final ImmutableListMultimap.Builder<AppVersion, ProctorClientApplication> builder =
                ImmutableListMultimap.builder();

        for (final ProctorClientApplication client : clients) {
            final AppVersion appVersion = new AppVersion(client.getApplication(), client.getVersion());
            builder.put(appVersion, client);
        }
        final ImmutableListMultimap<AppVersion, ProctorClientApplication> apps = builder.build();

        for (final AppVersion appVersion : apps.keySet()) {
            appVersionsToCheck.add(appVersion);
            final List<ProctorClientApplication> callableClients = apps.get(appVersion);
            assert !callableClients.isEmpty();
            futures.put(appVersion, httpExecutor.submit(
                    () -> internalGet(appVersion, callableClients)));

        }
        futures.forEach((version, future) -> {
            try {
                final RemoteSpecificationResult result = future.get();
                allResults.put(version, result);
                if (result.isSuccess()) {
                    appVersionsToCheck.remove(result.getVersion());
                }
            } catch (final InterruptedException e) {
                LOGGER.error("Interrupted getting " + version, e);
            } catch (final ExecutionException e) {
                final Throwable cause = e.getCause();
                LOGGER.error("Unable to fetch " + version, cause);
            }
        });

        applicationMapByEnvironment.put(environment, allResults.build());

        // TODO (parker) 9/6/12 - Fail if we do not have 1 specification for each <Application>.<Version>
        // should we update the cache?
        if (!appVersionsToCheck.isEmpty()) {
            LOGGER.warn("Failed to load any specification for the following AppVersions: "
                    + StringUtils.join(appVersionsToCheck, ','));
        }

        LOGGER.info("Finish refreshing internal list of ProctorSpecifications for " + environment);
        return appVersionsToCheck.isEmpty();
    }

    public void shutdown() {
        httpExecutor.shutdownNow();
    }

    /**
     * Fetch a specification from a list of instances (for a single application and version)
     * It iterates instances until it success to read specification or find it doesn't expose endpoint for spec.
     */
    private RemoteSpecificationResult internalGet(
            final AppVersion version,
            final List<ProctorClientApplication> clients
    ) {
        // ImmutableMap does not handle duplicate keys - use a HashMap for building instead
        final Map<ProctorClientApplication, Throwable> failures = new HashMap<>();
        for (final ProctorClientApplication client : clients) {
            try {
                final ProctorSpecifications specifications = fetchSpecification(client);
                return RemoteSpecificationResult.success(version, client, specifications);
            } catch (final IOException e) {
                failures.put(client, e);
            }
        }
        if (!failures.isEmpty()) {
            LOGGER.info("Failed to get specifications from " + version,
                    Iterables.getFirst(failures.values(), null)
            );
        }
        return RemoteSpecificationResult.failures(version, failures);
    }

    /**
     * try visiting private/v or private/proctor/specification to get specification
     * The specifications are used by proctor webapp to validate test edit/promotion
     * or to detect usage of test definitions.
     * <p>
     * Applications are expected to expose their specifications via either of these endpoints
     * <p>
     * * /private/proctor/specification
     * by com.indeed.proctor.consumer.ViewProctorSpecificationServlet
     * <p>
     * * /private/v?ns=JsonProctorLoaderFactory
     * by com.indeed.util.varexport.servlet.ViewExportedVariablesServlet
     * The variable is exposed in
     * com.indeed.proctor.common.JsonProctorLoaderFactory
     */
    private ProctorSpecifications fetchSpecification(
            final ProctorClientApplication client
    ) throws IOException {
        // This URL is where we expose variables by ViewExportedVariablesServlet
        final String varExportUrl = client.getBaseApplicationUrl()
                + "/private/v?ns=JsonProctorLoaderFactory";

        final ProctorSpecifications specFromVarExport = parseExportedVariables(
                fetchContentFromUrl(varExportUrl),
                client
        );

        // Use this spec if the var export contains multiple specifications
        // because the other legacy endpoint contains only single.
        //
        // Otherwise, check the legacy endpoint first where a client choose
        // what specification to expose.
        if (specFromVarExport.asSet().size() > 1) {
            return specFromVarExport;
        }

        // This URL is where we expose specification by ViewProctorSpecificationServlet
        final String viewSpecUrl = client.getBaseApplicationUrl()
                + "/private/proctor/specification";
        try {
            return parseSpecificationFromServlet(
                    fetchContentFromUrl(viewSpecUrl),
                    client
            );
        } catch (final IOException e) {
            // Fallback to the first version.
            return specFromVarExport;
        }
    }

    private String fetchContentFromUrl(
            final String urlString
    ) throws IOException {
        final HttpGet httpGet = new HttpGet(urlString);
        return this.httpClient.execute(httpGet, r -> {
            try (InputStream inputStream = r.getEntity().getContent()) {
                return IOUtils.toString(inputStream, "UTF-8");
            }
        });
    }

    @Nullable
    private ConsumableTestDefinition getCurrentConsumableTestDefinition(
            final Environment environment,
            final String testName
    ) {
        try {
            final TestDefinition testDefinition = proctorReaderMap.get(environment).getCurrentTestDefinition(testName);
            if (testDefinition != null) {
                return ProctorUtils.convertToConsumableTestDefinition(testDefinition);
            }
        } catch (final StoreException e) {
            LOGGER.warn("Failed to get current consumable test definition for " + testName + " in " + environment, e);
        }
        return null;
    }

    @Nullable
    private TestMatrixArtifact getCurrentTestMatrixArtifact(
            final Environment environment
    ) {
        try {
            final TestMatrixVersion testMatrix =
                    proctorReaderMap.get(environment).getCurrentTestMatrix();
            if (testMatrix != null) {
                return ProctorUtils.convertToConsumableArtifact(testMatrix);
            }
        } catch (final StoreException e) {
            LOGGER.warn("Failed to get current test matrix artifact in " + environment, e);
        }
        return null;
    }

    /**
     * Parse generated string from
     * com.indeed.util.varexport.servlet.ViewExportedVariablesServlet
     * for variables in the namespace of JsonProctorLoaderFactory
     * <p>
     * JsonProctorLoaderFactory is expected to expose specification variable
     * with the variable name
     * - specification (earlier than 1.7.5)
     * or
     * - specification-file_name.json (1.7.5 or later)
     *
     * <p>
     * Example string is
     * <p>
     * specification-spec.json={"providedContext"\:{"country"\:"String"}},"tests"\:{},"dynamicFilters"\:[]}
     * exporter-start-time=2019-09-18T02\:28\:24CDT
     */
    @VisibleForTesting
    static ProctorSpecifications parseExportedVariables(
            final String content,
            final ProctorClientApplication client
    ) throws IOException {
        final Properties exportedVariables = VarExportedVariablesDeserializer
                .deserialize(content);

        final Set<ProctorSpecification> specifications = new HashSet<>();
        for (final String key : exportedVariables.stringPropertyNames()) {
            // JsonProctorLoaderFactory is expected to expose json string of specifications
            // with "specification" or "specification-file_name.json"
            if (!key.startsWith("specification")) {
                continue;
            }
            final String json = exportedVariables.getProperty(key);
            try {
                final ProctorSpecification specification =
                        OBJECT_MAPPER.readValue(json, ProctorSpecification.class);
                specifications.add(specification);
            } catch (final IOException e) {
                LOGGER.error("Failed to parse variable " + key
                        + ": " + json + " from " + client, e);
            }
        }
        if (specifications.isEmpty()) {
            throw new IOException(
                    "no specification can be parsed from exported variables from "
                            + client
            );
        }
        return new ProctorSpecifications(specifications);
    }

    private static ProctorSpecifications parseSpecificationFromServlet(
            final String jsonString,
            final ProctorClientApplication client
    ) throws IOException {
        final com.indeed.proctor.common.SpecificationResult specificationResult =
                OBJECT_MAPPER.readValue(jsonString,
                        com.indeed.proctor.common.SpecificationResult.class
                );

        if (specificationResult.getSpecification() == null) {
            throw new IOException(
                    "Failed to fetch from servlet: "
                            + specificationResult.getError()
                            + " from " + client
            );
        }

        return new ProctorSpecifications(
                Collections.singleton(
                        specificationResult.getSpecification()
                )
        );
    }
}
