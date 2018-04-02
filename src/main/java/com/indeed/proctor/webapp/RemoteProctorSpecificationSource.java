package com.indeed.proctor.webapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
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
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/**
 * @author parker
 */
public class RemoteProctorSpecificationSource extends DataLoadingTimerTask implements ProctorSpecificationSource {
    private static final Logger LOGGER = Logger.getLogger(RemoteProctorSpecificationSource.class);

    private static final ObjectMapper OBJECT_MAPPER = Serializers.strict();

    @Autowired(required = false)
    private ProctorClientSource clientSource = new DefaultClientSource();

    private final int httpTimeout;
    private final ExecutorService httpExecutor;

    private volatile Map<Environment, ImmutableMap<AppVersion, RemoteSpecificationResult>> cache_ = Maps.newConcurrentMap();

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
        final ImmutableMap<AppVersion, RemoteSpecificationResult> results = cache_.get(environment);
        if (results != null && results.containsKey(version)) {
            return results.get(version);
        }
        return RemoteSpecificationResult.newBuilder(version).build(Collections.<ProctorClientApplication>emptyList());
    }

    @Override
    public Map<AppVersion, RemoteSpecificationResult> loadAllSpecifications(Environment environment) {
        final ImmutableMap<AppVersion, RemoteSpecificationResult> cache = cache_.get(environment);
        if (cache == null) {
            return Collections.emptyMap();
        } else {
            return cache;
        }
    }

    @Override
    public Map<AppVersion, ProctorSpecification> loadAllSuccessfulSpecifications(Environment environment) {
        final ImmutableMap<AppVersion, RemoteSpecificationResult> cache = cache_.get(environment);
        if (cache == null) {
            return Collections.emptyMap();
        } else {
            final Map<AppVersion, RemoteSpecificationResult> success = Maps.filterEntries(cache, new Predicate<Map.Entry<AppVersion, RemoteSpecificationResult>>() {
                @Override
                public boolean apply(@Nullable Map.Entry<AppVersion, RemoteSpecificationResult> input) {
                    return input.getValue().isSuccess();
                }
            });
            return Maps.transformValues(success, new Function<RemoteSpecificationResult, ProctorSpecification>() {
                @Override
                public ProctorSpecification apply(@Nullable RemoteSpecificationResult input) {
                    return input.getSpecificationResult().getSpecification();
                }
            });
        }
    }

    @Override
    public Set<AppVersion> activeClients(final Environment environment, final String testName) {
        final Map<AppVersion, RemoteSpecificationResult> specifications = cache_.get(environment);
        if (specifications == null) {
            return Collections.emptySet();
        }
        final Set<AppVersion> clients = Sets.newHashSet();
        final ConsumableTestDefinition testDefinition = getCurrentConsumableTestDefinition(environment, testName);
        for (Map.Entry<AppVersion, RemoteSpecificationResult> entry : specifications.entrySet()) {
            final AppVersion version = entry.getKey();
            final RemoteSpecificationResult rr = entry.getValue();
            if (rr.isSuccess()) {
                final ProctorSpecification specification = rr.getSpecificationResult().getSpecification();
                final DynamicFilters filters = specification.getDynamicFilters();
                if (containsTest(specification, testName) || willResolveTest(filters, testName, testDefinition)) {
                    clients.add(version);
                }
            }
        }
        return clients;
    }

    @Override
    public Set<String> activeTests(final Environment environment) {
        final Map<AppVersion, RemoteSpecificationResult> specifications = cache_.get(environment);
        if (specifications == null) {
            return Collections.emptySet();
        }

        final TestMatrixArtifact testMatrixArtifact = getCurrentTestMatrixArtifact(environment);
        Preconditions.checkNotNull(testMatrixArtifact, "Failed to get the current test matrix artifact");
        final Map<String, ConsumableTestDefinition> definedTests = testMatrixArtifact.getTests();

        final Set<String> tests = Sets.newHashSet();
        for (final Map.Entry<AppVersion, RemoteSpecificationResult> entry : specifications.entrySet()) {
            final RemoteSpecificationResult remoteResult = entry.getValue();
            if (remoteResult.isSuccess()) {
                final ProctorSpecification specification = remoteResult.getSpecificationResult().getSpecification();
                final Set<String> requiredTests = specification.getTests().keySet();
                final Set<String> dynamicTests = specification.getDynamicFilters()
                        .determineTests(definedTests, requiredTests);
                tests.addAll(requiredTests);
                tests.addAll(dynamicTests);
            }
        }
        return tests;
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
        boolean devSuccess = refreshInternalCache(Environment.WORKING);
        boolean qaSuccess = refreshInternalCache(Environment.QA);
        boolean productionSuccess = refreshInternalCache(Environment.PRODUCTION);
        return devSuccess && qaSuccess && productionSuccess;
    }

    private boolean refreshInternalCache(Environment environment) {
        LOGGER.info("Refreshing internal list of ProctorSpecifications");

        final List<ProctorClientApplication> clients = clientSource.loadClients(environment);

        final Map<AppVersion, Future<RemoteSpecificationResult>> futures = Maps.newLinkedHashMap();

        final ImmutableMap.Builder<AppVersion, RemoteSpecificationResult> allResults = ImmutableMap.builder();
        final Set<AppVersion> appVersionsToCheck = Sets.newLinkedHashSet();
        final Set<AppVersion> skippedAppVersions = Sets.newLinkedHashSet();

        // Accumulate all clients that have equivalent AppVersion (APPLICATION_COMPARATOR)
        final ImmutableListMultimap.Builder<AppVersion, ProctorClientApplication> builder = ImmutableListMultimap.builder();

        for (final ProctorClientApplication client : clients) {
            final AppVersion appVersion = new AppVersion(client.getApplication(), client.getVersion());
            builder.put(appVersion, client);
        }
        final ImmutableListMultimap<AppVersion, ProctorClientApplication> apps = builder.build();

        for (final AppVersion appVersion : apps.keySet()) {
            appVersionsToCheck.add(appVersion);
            final List<ProctorClientApplication> callableClients = apps.get(appVersion);
            assert callableClients.size() > 0;
            futures.put(appVersion, httpExecutor.submit(new Callable<RemoteSpecificationResult>() {
                @Override
                public RemoteSpecificationResult call() throws Exception {
                    return internalGet(appVersion, callableClients, httpTimeout);
                }
            }));

        }
        while (!futures.isEmpty()) {
            try {
                Thread.sleep(10);
            } catch (final InterruptedException e) {
                LOGGER.error("Oh heavens", e);
            }
            for (final Iterator<Map.Entry<AppVersion, Future<RemoteSpecificationResult>>> iterator = futures.entrySet().iterator(); iterator.hasNext(); ) {
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

        synchronized (cache_) {
            cache_.put(environment, allResults.build());
        }

        // TODO (parker) 9/6/12 - Fail if we do not have 1 specification for each <Application>.<Version>
        // should we update the cache?
        if (!appVersionsToCheck.isEmpty()) {
            LOGGER.warn("Failed to load any specification for the following AppVersions: " + Joiner.on(",").join(appVersionsToCheck));
        }

        if (!skippedAppVersions.isEmpty()) {
            LOGGER.info("Skipped checking specification for the following AppVersions (/private/proctor/specification returned 404): " + Joiner.on(",").join(skippedAppVersions));
        }

        return appVersionsToCheck.isEmpty();
    }

    public void shutdown() {
        httpExecutor.shutdownNow();
    }

    private static RemoteSpecificationResult internalGet(final AppVersion version, final List<ProctorClientApplication> clients, final int timeout) {
        // TODO (parker) 2/7/13 - priority queue them based on AUS datacenter, US DC, etc
        final LinkedList<ProctorClientApplication> remaining = Lists.newLinkedList(clients);

        final RemoteSpecificationResult.Builder results = RemoteSpecificationResult.newBuilder(version);
        while (remaining.peek() != null) {
            final ProctorClientApplication client = remaining.poll();
            // really stupid method of pinging 1 of the applications.
            final Pair<Integer, SpecificationResult> result = fetchSpecification(client, timeout);
            final int statusCode = result.getFirst();
            final SpecificationResult specificationResult = result.getSecond();
            if (fetchSpecificationFailed(specificationResult)) {
                if (statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    LOGGER.info("Client " + client.getBaseApplicationUrl() + " /private/proctor/specification returned 404 - skipping");
                    results.skipped(client, specificationResult);
                    break;
                }

                // Don't yell too load, the error is handled
                LOGGER.info("Failed to read specification from: " + client.getBaseApplicationUrl() + " : " + specificationResult.getError());
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
            return fetchSpecificationFromExportedVaraible(client, timeout);
        }
        return apiSpec;
    }

    // @Nonnull
    private static Pair<Integer, SpecificationResult> fetchSpecification(final String urlString, final int timeout, final SpecificationParser parser) {
        int statusCode = -1;
        InputStream inputStream = null;
        try {
            final URL url = new URL(urlString);
            LOGGER.debug("Trying to read specification from " + url.toString() + " using timeout " + timeout + " ms");
            final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(timeout);
            urlConnection.setConnectTimeout(timeout);

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
    private static boolean willResolveTest(final DynamicFilters filters, final String testName, final ConsumableTestDefinition testDefinition) {
        return (filters != null) && StringUtils.isNotEmpty(testName) && (testDefinition != null) &&
                filters.determineTests(ImmutableMap.of(testName, testDefinition), Collections.emptySet()).contains(testName);
    }

    @Nullable
    private ConsumableTestDefinition getCurrentConsumableTestDefinition(final Environment environment, final String testName) {
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
    private TestMatrixArtifact getCurrentTestMatrixArtifact(final Environment environment) {
        try {
            final TestMatrixVersion testMatrix = proctorReaderMap.get(environment).getCurrentTestMatrix();
            if (testMatrix != null) {
                return ProctorUtils.convertToConsumableArtifact(testMatrix);
            }
        } catch (final StoreException e) {
            LOGGER.warn("Failed to get current test matrix artifact in " + environment, e);
        }
        return null;
    }

    private static Pair<Integer, SpecificationResult> fetchSpecificationFromApi(final ProctorClientApplication client, final int timeout) {
        /**
         * This needs to be moved to a separate checker class implementing some interface
         **/
        final String apiUrl = client.getBaseApplicationUrl() + "/private/proctor/specification";
        return fetchSpecification(apiUrl, timeout, new SpecificationParser() {
            @Override
            public SpecificationResult parse(final InputStream inputStream) throws IOException {
                return OBJECT_MAPPER.readValue(inputStream, SpecificationResult.class);
            }
        });
    }

    private static Pair<Integer, SpecificationResult> fetchSpecificationFromExportedVaraible(final ProctorClientApplication client, final int timeout) {
        /**
         * This needs to be moved to a separate checker class implementing some interface
         **/
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
