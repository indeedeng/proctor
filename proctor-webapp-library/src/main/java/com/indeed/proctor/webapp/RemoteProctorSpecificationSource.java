package com.indeed.proctor.webapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.common.Serializers;
import com.indeed.proctor.common.SpecificationResult;
import com.indeed.proctor.common.dynamic.DynamicFilters;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.ProctorReader;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.model.AppVersion;
import com.indeed.proctor.webapp.model.ProctorClientApplication;
import com.indeed.proctor.webapp.model.RemoteSpecificationResult;
import com.indeed.proctor.webapp.util.threads.LogOnUncaughtExceptionHandler;
import com.indeed.util.core.DataLoadingTimerTask;
import com.indeed.util.core.Pair;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private final int httpTimeout;
    private final ExecutorService httpExecutor;

    private final Map<Environment, ImmutableMap<AppVersion, RemoteSpecificationResult>> applicationMapByEnvironment =
            Maps.newConcurrentMap();

    private final Map<Environment, ProctorReader> proctorReaderMap;

    public RemoteProctorSpecificationSource(final int httpTimeout,
                                            final int executorThreads,
                                            final ProctorReader trunk,
                                            final ProctorReader qa,
                                            final ProctorReader production) {
        super(RemoteProctorSpecificationSource.class.getSimpleName());
        this.httpTimeout = httpTimeout;
        Preconditions.checkArgument(httpTimeout > 0, "verificationTimeout > 0");
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
    public Map<AppVersion, ProctorSpecification> loadAllSuccessfulSpecifications(final Environment environment) {
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
                        e -> e.getValue().getSpecificationResult().getSpecification()
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

        return applicationsMap.entrySet().stream()
                .filter(e -> e.getValue().isSuccess())
                .filter(e -> {
                    final ProctorSpecification specification = e.getValue().getSpecificationResult().getSpecification();
                    final DynamicFilters filters = specification.getDynamicFilters();
                    return (containsTest(specification, testName) || willResolveTest(filters, testName, testDefinition));
                })
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
                .map(r -> r.getSpecificationResult().getSpecification())
                .map(s -> {
                    final Set<String> requiredTests = s.getTests().keySet();
                    return ImmutableSet.<String>builder()
                            .addAll(requiredTests)
                            .addAll(s.getDynamicFilters().determineTests(definedTests, requiredTests))
                            .build();
                })
                .flatMap(Collection::stream)
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
                    () -> internalGet(appVersion, callableClients, httpTimeout)));

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
    private static RemoteSpecificationResult internalGet(
            final AppVersion version,
            final List<ProctorClientApplication> clients,
            final int timeoutMillis
    ) {
        // TODO (parker) 2/7/13 - priority queue them based on AUS datacenter, US DC, etc
        final LinkedList<ProctorClientApplication> remaining = Lists.newLinkedList(clients);

        final RemoteSpecificationResult.Builder results = RemoteSpecificationResult.newBuilder(version);
        while (remaining.peek() != null) {
            final ProctorClientApplication client = remaining.poll();
            // really stupid method of pinging 1 of the applications.
            final Pair<Integer, SpecificationResult> result = fetchSpecification(client, timeoutMillis);
            final int statusCode = result.getFirst();
            final SpecificationResult specificationResult = result.getSecond();
            if (fetchSpecificationFailed(specificationResult)) {
                if (statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    LOGGER.info("Client " + client.getBaseApplicationUrl() + " endpoints for spec returned 404 - skipping");
                    results.skipped(client, specificationResult);
                    // skipping rest of clients as none of them is expected to provide the endpoint
                    break;
                }

                // Don't yell too load, the error is handled
                LOGGER.debug("Failed to read specification from: " + client.getBaseApplicationUrl()
                        + " : " + statusCode + ", " + specificationResult.getError());
                results.failed(client, specificationResult);
            } else {
                results.success(client, specificationResult);
                break;
            }
        }
        return results.build(remaining);
    }

    /**
     * Firstly try visiting specification API end point. If it's not exported, fetch from private/v instead.
     *
     * @param client
     * @param timeout
     * @return
     */
    private static Pair<Integer, SpecificationResult> fetchSpecification(final ProctorClientApplication client, final int timeout) {

        final Pair<Integer, SpecificationResult> apiSpec = fetchSpecificationFromApi(client, timeout);
        if (fetchSpecificationFailed(apiSpec.getSecond())) {
            return fetchSpecificationFromExportedVariable(client, timeout);
        }
        return apiSpec;
    }

    private static Pair<Integer, SpecificationResult> fetchSpecification(
            final String urlString,
            final int timeoutMillis,
            final SpecificationParser parser
    ) {
        int statusCode = -1;
        InputStream inputStream = null;
        try {
            final URL url = new URL(urlString);
            final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(timeoutMillis);
            urlConnection.setConnectTimeout(timeoutMillis);

            statusCode = urlConnection.getResponseCode();
            inputStream = urlConnection.getInputStream();
            //  map from testName => list of bucket names
            final SpecificationResult result = parser.parse(inputStream);
            return new Pair<>(statusCode, result);
        } catch (final Throwable e) {
            final SpecificationResult errorResult = createErrorResult(e);
            return new Pair<>(statusCode, errorResult);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (final IOException e) {
                LOGGER.error("Unable to close stream to " + urlString, e);
            }
        }
    }

    private static SpecificationResult createErrorResult(final Throwable t) {
        final SpecificationResult result = new SpecificationResult();
        final StringWriter sw = new StringWriter();
        final PrintWriter writer = new PrintWriter(sw);
        t.printStackTrace(writer);

        result.setError(t.getMessage());
        result.setException(sw.toString());
        return result;
    }

    // Check if this test is defined in a specification by a client.
    private static boolean containsTest(final ProctorSpecification specification, final String testName) {
        return specification.getTests().containsKey(testName);
    }

    // Check if this test will be resolved by defined filters in a client.
    private static boolean willResolveTest(
            final DynamicFilters filters,
            final String testName,
            final ConsumableTestDefinition testDefinition
    ) {
        return (filters != null)
                && StringUtils.isNotEmpty(testName)
                && (testDefinition != null)
                && filters.determineTests(
                ImmutableMap.of(testName, testDefinition), Collections.emptySet()).contains(testName);
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

    private static Pair<Integer, SpecificationResult> fetchSpecificationFromApi(
            final ProctorClientApplication client,
            final int timeoutMillis
    ) {
        /*
         * This needs to be moved to a separate checker class implementing some interface
         */
        final String apiUrl = client.getBaseApplicationUrl() + "/private/proctor/specification";
        return fetchSpecification(apiUrl, timeoutMillis, new SpecificationParser() {
            @Override
            public SpecificationResult parse(final InputStream inputStream) throws IOException {
                return OBJECT_MAPPER.readValue(inputStream, SpecificationResult.class);
            }
        });
    }

    private static Pair<Integer, SpecificationResult> fetchSpecificationFromExportedVariable(
            final ProctorClientApplication client,
            final int timeout
    ) {
        /*
         * This needs to be moved to a separate checker class implementing some interface
         */
        final String urlStr = client.getBaseApplicationUrl() + "/private/v?ns=JsonProctorLoaderFactory&v=specification";
        return fetchSpecification(urlStr, timeout, EXPORTED_VARIABLE_PARSER);
    }

    @VisibleForTesting
    static final SpecificationParser EXPORTED_VARIABLE_PARSER = new SpecificationParser() {
        @Override
        public SpecificationResult parse(final InputStream inputStream) throws IOException {
            final String json = IOUtils.toString(inputStream).replace("\\:", ":").trim();
            final ProctorSpecification proctorSpecification = OBJECT_MAPPER.readValue(json, ProctorSpecification.class);
            final SpecificationResult specificationResult = new SpecificationResult();
            specificationResult.setSpecification(proctorSpecification);
            return specificationResult;
        }
    };

    interface SpecificationParser {
        SpecificationResult parse(final InputStream inputStream) throws IOException;
    }

    private static boolean fetchSpecificationFailed(final SpecificationResult result) {
        return result.getSpecification() == null;
    }

}
