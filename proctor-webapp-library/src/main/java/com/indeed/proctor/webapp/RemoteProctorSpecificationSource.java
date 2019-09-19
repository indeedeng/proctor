package com.indeed.proctor.webapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
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
import com.indeed.proctor.webapp.model.SpecificationResult;
import com.indeed.proctor.webapp.util.VarExportedVariablesDeserializer;
import com.indeed.proctor.webapp.util.threads.LogOnUncaughtExceptionHandler;
import com.indeed.util.core.DataLoadingTimerTask;
import com.indeed.util.core.Pair;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
    private static final Logger LOGGER = Logger.getLogger(RemoteProctorSpecificationSource.class);

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
                        .newBuilder(version)
                        .build(Collections.emptyList())
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
                .filter(e -> e.getValue().isSuccess())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        // not actually nullable because of filter
                        e -> e.getValue().getSpecificationResult().getSpecifications()
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
                .filter(e -> e.getValue().isSuccess())
                .filter(e -> e.getValue().getSpecificationResult()
                        .getSpecifications()
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
                .filter(RemoteSpecificationResult::isSuccess)
                .map(r -> r.getSpecificationResult().getSpecifications())
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
        final Set<AppVersion> skippedAppVersions = Sets.newLinkedHashSet();

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
        while (!futures.isEmpty()) {
            try {
                Thread.sleep(10);
            } catch (final InterruptedException e) {
                LOGGER.error("Oh heavens", e);
            }
            for (final Iterator<Map.Entry<AppVersion, Future<RemoteSpecificationResult>>> iterator =
                 futures.entrySet().iterator(); iterator.hasNext(); ) {
                final Map.Entry<AppVersion, Future<RemoteSpecificationResult>> entry = iterator.next();
                final AppVersion appVersion = entry.getKey();
                final Future<RemoteSpecificationResult> future = entry.getValue();
                if (future.isDone()) {
                    iterator.remove();
                    try {
                        final RemoteSpecificationResult result = future.get();
                        allResults.put(appVersion, result);
                        if (result.isSkipped()) {
                            skippedAppVersions.add(result.getVersion());
                            appVersionsToCheck.remove(result.getVersion());
                        } else if (result.isSuccess()) {
                            appVersionsToCheck.remove(result.getVersion());
                        }

                    } catch (final InterruptedException e) {
                        LOGGER.error("Interrupted getting " + appVersion, e);
                    } catch (final ExecutionException e) {
                        final Throwable cause = e.getCause();
                        LOGGER.error("Unable to fetch " + appVersion, cause);
                    }
                }
            }
        }

        applicationMapByEnvironment.put(environment, allResults.build());

        // TODO (parker) 9/6/12 - Fail if we do not have 1 specification for each <Application>.<Version>
        // should we update the cache?
        if (!appVersionsToCheck.isEmpty()) {
            LOGGER.warn("Failed to load any specification for the following AppVersions: "
                    + StringUtils.join(appVersionsToCheck, ','));
        }

        if (!skippedAppVersions.isEmpty()) {
            LOGGER.info("Skipped checking specification for the following AppVersions "
                    + "(endpoints for specs returned 404): "
                    + StringUtils.join(skippedAppVersions, ','));
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
        // TODO (parker) 2/7/13 - priority queue them based on AUS datacenter, US DC, etc
        final LinkedList<ProctorClientApplication> remaining = Lists.newLinkedList(clients);

        final RemoteSpecificationResult.Builder results = RemoteSpecificationResult.newBuilder(version);
        while (remaining.peek() != null) {
            final ProctorClientApplication client = remaining.poll();
            // really stupid method of pinging 1 of the applications.
            final Pair<Integer, SpecificationResult> result = fetchSpecification(client);
            final int statusCode = result.getFirst();
            final SpecificationResult specificationResult = result.getSecond();
            if (specificationResult.isFailed()) {
                if (statusCode == HttpStatus.SC_NOT_FOUND) {
                    LOGGER.info("App " + version + " endpoints for spec returned 404 - skipping");
                    results.skipped(client, specificationResult);
                    // skipping rest of clients as none of them is expected to provide the endpoint
                    break;
                }

                LOGGER.debug("Failed to read specification of " + version
                        + " : " + specificationResult.getError());
                results.failed(client, specificationResult);
            } else {
                results.success(client, specificationResult);
                break;
            }
        }
        return results.build(remaining);
    }

    /**
     * try visiting private/v or private/proctor/specification to get specification
     */
    private Pair<Integer, SpecificationResult> fetchSpecification(
            final ProctorClientApplication client
    ) {
        // This URL is where we expose variables by ViewExportedVariablesServlet
        final String varExportUrl = client.getBaseApplicationUrl()
                + "/private/v?ns=JsonProctorLoaderFactory";

        final Pair<Integer, SpecificationResult> specFromVarExport = fetchSpecificationFromUrl(
                varExportUrl,
                RemoteProctorSpecificationSource::parseExportedVariables
        );

        // Use var export version to support multiple specification
        if (specFromVarExport.getSecond().getSpecifications().asSet().size() > 1) {
            return specFromVarExport;
        }

        // This URL is where we expose specification by ViewProctorSpecificationServlet
        final String viewSpecUrl = client.getBaseApplicationUrl()
                + "/private/proctor/specification";
        final Pair<Integer, SpecificationResult> specFromViewSpec = fetchSpecificationFromUrl(
                viewSpecUrl,
                RemoteProctorSpecificationSource::parseSpecificationFromServlet
        );

        // Use specification servlet version to allow users to choose any spec.
        if (!specFromViewSpec.getSecond().isFailed()) {
            return specFromViewSpec;
        }

        // Fallback to the first version.
        return specFromVarExport;
    }

    private Pair<Integer, SpecificationResult> fetchSpecificationFromUrl(
            final String urlString,
            final SpecificationParser parser
    ) {
        try {
            final HttpGet httpGet = new HttpGet(urlString);
            return this.httpClient.execute(httpGet, (r) -> {
                try (InputStream inputStream = r.getEntity().getContent()) {
                    return new Pair<>(
                            r.getStatusLine().getStatusCode(),
                            parser.parse(inputStream)
                    );
                }
            });
        } catch (final Throwable e) {
            return createErrorResult(e);
        }
    }

    private static Pair<Integer, SpecificationResult> createErrorResult(final Throwable t) {
        final int statusCode;
        if (t instanceof HttpResponseException) {
            statusCode = ((HttpResponseException) t).getStatusCode();
        } else {
            statusCode = -1;
        }

        return new Pair<>(statusCode, SpecificationResult.failed(t.getMessage()));
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

    @VisibleForTesting
    static SpecificationResult parseExportedVariables(
            final InputStream inputStream
    ) throws IOException {
        final Properties exportedVariables = VarExportedVariablesDeserializer
                .deserialize(inputStream);

        final Set<ProctorSpecification> specifications = new HashSet<>();
        for (final String key : exportedVariables.stringPropertyNames()) {
            if (!key.startsWith("specification")) {
                continue;
            }
            final String json = exportedVariables.getProperty(key);
            final ProctorSpecification specification =
                    OBJECT_MAPPER.readValue(json, ProctorSpecification.class);
            specifications.add(specification);
        }
        if (specifications.isEmpty()) {
            return SpecificationResult.failed(
                    "no specification is found in exported variables"
            );
        }
        return SpecificationResult.success(specifications);
    }

    private static SpecificationResult parseSpecificationFromServlet(
            final InputStream inputStream
    ) throws IOException {
        final com.indeed.proctor.common.SpecificationResult specificationResult =
                OBJECT_MAPPER.readValue(inputStream,
                        com.indeed.proctor.common.SpecificationResult.class
                );

        if (specificationResult.getSpecification() == null) {
            return SpecificationResult.failed(
                    Strings.nullToEmpty(specificationResult.getError())
            );
        }

        return SpecificationResult.success(
                Collections.singleton(
                        specificationResult.getSpecification()
                )
        );
    }

    interface SpecificationParser {
        SpecificationResult parse(final InputStream inputStream) throws IOException;
    }
}
