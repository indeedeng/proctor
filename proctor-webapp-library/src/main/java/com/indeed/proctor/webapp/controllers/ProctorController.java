package com.indeed.proctor.webapp.controllers;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.indeed.proctor.common.ProctorLoadResult;
import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.common.Serializers;
import com.indeed.proctor.common.TestSpecification;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.proctor.common.model.TestMatrixDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.ProctorSpecificationSource;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.model.AppVersion;
import com.indeed.proctor.webapp.model.RemoteSpecificationResult;
import com.indeed.proctor.webapp.model.SessionViewModel;
import com.indeed.proctor.webapp.model.WebappConfiguration;
import com.indeed.proctor.webapp.util.threads.LogOnUncaughtExceptionHandler;
import com.indeed.proctor.webapp.views.JsonView;
import io.swagger.annotations.ApiOperation;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiFunction;

@Controller
@RequestMapping({"/", "/proctor"})
public class ProctorController extends AbstractController {
    private static final Logger LOGGER = Logger.getLogger(ProctorController.class);

    private static final long FALLBACK_UPDATED_TIME = 0L;

    private final ObjectMapper objectMapper = Serializers.strict();
    private final int verificationTimeout;
    private final ExecutorService executor;
    private final ProctorSpecificationSource specificationSource;

    private static enum View {
        MATRIX_LIST("matrix/list"),
        MATRIX_USAGE("matrix/usage"),
        MATRIX_COMPATIBILITY("matrix/compatibility"),
        ERROR("error"),;

        private final String name;
        private View(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @Autowired
    public ProctorController(final WebappConfiguration configuration,
                             @Qualifier("trunk") final ProctorStore trunkStore,
                             @Qualifier("qa") final ProctorStore qaStore,
                             @Qualifier("production") final ProctorStore productionStore,
            @Value("${verify.http.timeout:1000}") final int verificationTimeout,
            @Value("${verify.executor.threads:10}") final int executorThreads,
            final ProctorSpecificationSource specificationSource) {
        super(configuration, trunkStore, qaStore, productionStore);
        this.verificationTimeout = verificationTimeout;
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("proctor-verifiers-Thread-%d")
                .setUncaughtExceptionHandler(new LogOnUncaughtExceptionHandler())
                .build();
        this.executor = Executors.newFixedThreadPool(executorThreads, threadFactory);
        this.specificationSource = specificationSource;
    }

    /**
     * TODO: this should be the default screen at /
     */
    // not a @ApiOperation because it produces HTML
    @RequestMapping(value="/", method=RequestMethod.GET)
    public String viewTestMatrix(final String branch,
                                 final Model model,
                                 @RequestParam(defaultValue="50") final Integer testsPerPage) {
        final Environment which = determineEnvironmentFromParameter(branch);

        boolean emptyClients = true;
        for (final Environment environment : Environment.values()) {
            emptyClients &= specificationSource.loadAllSpecifications(environment).keySet().isEmpty();
        }
        model.addAttribute("emptyClients", emptyClients);
        model.addAttribute("testsPerPage", testsPerPage);
        return getArtifactForView(model, which, View.MATRIX_LIST);
    }

    @ApiOperation(value = "Proctor test matrix", response = TestMatrixArtifact.class)
    @RequestMapping(value="/matrix/raw", method=RequestMethod.GET)
    public JsonView viewRawTestMatrix(final String branch, final Model model) {
        final Environment which = determineEnvironmentFromParameter(branch);
        final TestMatrixVersion testMatrixVersion = getCurrentMatrix(which);
        final TestMatrixArtifact testMatrixArtifact = ProctorUtils.convertToConsumableArtifact(testMatrixVersion);
        return new JsonView(testMatrixArtifact);
    }

    // not a @ApiOperation because it produces HTML
    @RequestMapping(value="/usage", method=RequestMethod.GET)
    public String viewMatrixUsage(final Model model) {
        // treemap for sorted iteration by test name
        final Map<String, CompatibilityRow> tests = Maps.newTreeMap();


        final TestMatrixVersion devMatrix = getCurrentMatrix(Environment.WORKING);
        populateTestUsageViewModel(Environment.WORKING, devMatrix, tests, Environment.WORKING);

        final TestMatrixVersion qaMatrix = getCurrentMatrix(Environment.QA);
        populateTestUsageViewModel(Environment.QA, qaMatrix, tests, Environment.QA);

        final TestMatrixVersion productionMatrix = getCurrentMatrix(Environment.PRODUCTION);
        populateTestUsageViewModel(Environment.PRODUCTION, productionMatrix, tests, Environment.PRODUCTION);

        model.addAttribute("tests", tests);
        model.addAttribute("devMatrix", devMatrix);
        model.addAttribute("qaMatrix", qaMatrix);
        model.addAttribute("productionMatrix", productionMatrix);
        model.addAttribute("session",
                           SessionViewModel.builder()
                               .setUseCompiledCSS(getConfiguration().isUseCompiledCSS())
                               .setUseCompiledJavaScript(getConfiguration().isUseCompiledJavaScript())
                               // todo get the appropriate js compile / non-compile url
                           .build());

        return View.MATRIX_USAGE.getName();
    }

    @ApiOperation(value = "Proctor test specification", response = TestMatrixArtifact.class)
    @RequestMapping(value = "/specification", method = RequestMethod.GET)
    public JsonView viewProctorSpecification(final String branch,
                                             final String app,
                                             final String version) throws IOException {
        final Environment environment = determineEnvironmentFromParameter(branch);
        final AppVersion appVersion = new AppVersion(app, version);
        final RemoteSpecificationResult spec = specificationSource.getRemoteResult(environment, appVersion);

        return new JsonView(spec);
    }
    private void populateTestUsageViewModel(final Environment matrixEnvironment,
                                            final TestMatrixVersion matrix,
                                            final Map<String, CompatibilityRow> tests,
                                            final Environment environment) {
        final TestMatrixArtifact artifact = ProctorUtils.convertToConsumableArtifact(matrix);
        final Map<String, ConsumableTestDefinition> definedTests = artifact.getTests();

        final Map<AppVersion, ProctorSpecification> clients = specificationSource.loadAllSuccessfulSpecifications(environment);
        // sort the apps (probably should sort the Map.Entry, but this is good enough for now
        final SortedSet<AppVersion> versions = Sets.newTreeSet(clients.keySet());

        for (final AppVersion version : versions) {
            final ProctorSpecification specification = clients.get(version);

            final Map<String, TestSpecification> requiredTests = specification.getTests();
            final Set<String> dynamicTests = specification.getDynamicFilters().determineTests(definedTests, requiredTests.keySet());

            for (final Map.Entry<String, TestSpecification> testEntry : requiredTests.entrySet()) {
                final String testName = testEntry.getKey();
                final TestSpecification testSpecification = testEntry.getValue();

                tests.computeIfAbsent(testName, k -> new CompatibilityRow())
                        .addVersion(
                                environment,
                                CompatibleSpecificationResult.fromRequiredTest(
                                        matrixEnvironment,
                                        version,
                                        artifact,
                                        testName,
                                        testSpecification
                                )
                        );
            }

            for (final String testName : dynamicTests) {
                tests.computeIfAbsent(testName, k -> new CompatibilityRow())
                        .addVersion(
                                environment,
                                CompatibleSpecificationResult.fromDynamicTest(
                                        matrixEnvironment,
                                        version,
                                        artifact,
                                        testName
                                )
                        );
            }
        }

        // for each of the tests in the matrix, make sure there is an entry in the usageViewModel
        for (final String testName : definedTests.keySet()) {
            tests.computeIfAbsent(testName, k -> new CompatibilityRow());
        }
    }

    // not a @ApiOperation because it produces HTML
    @RequestMapping(value="/compatibility", method=RequestMethod.GET)
    public String viewMatrixCompatibility(final Model model) {
        final Map<Environment, CompatibilityRow> compatibilityMap = Maps.newLinkedHashMap();

        populateCompabilityRow(compatibilityMap, Environment.WORKING);
        populateCompabilityRow(compatibilityMap, Environment.QA);
        populateCompabilityRow(compatibilityMap, Environment.PRODUCTION);

        model.addAttribute("compatibilityMap", compatibilityMap);
        model.addAttribute("session",
                           SessionViewModel.builder()
                               .setUseCompiledCSS(getConfiguration().isUseCompiledCSS())
                               .setUseCompiledJavaScript(getConfiguration().isUseCompiledJavaScript())
                               // todo get the appropriate js compile / non-compile url
                           .build());
        return View.MATRIX_COMPATIBILITY.getName();
    }

    private void populateCompabilityRow(final Map<Environment, CompatibilityRow> rows, final Environment rowEnv) {
        final CompatibilityRow row = new CompatibilityRow();
        rows.put(rowEnv, row);
        final TestMatrixVersion matrix = getCurrentMatrix(rowEnv);
        final TestMatrixArtifact artifact = ProctorUtils.convertToConsumableArtifact(matrix);
        populateSingleCompabilityColumn(rowEnv, artifact, row, Environment.WORKING);
        populateSingleCompabilityColumn(rowEnv, artifact, row, Environment.QA);
        populateSingleCompabilityColumn(rowEnv, artifact, row, Environment.PRODUCTION);
    }

    /**
     * We want a compatibility matrix of
     *
     * TRUNK-MATRIX:
     *      [DEV-WEBAPPS]:
     *          (web-app-1): compatible?
     *      [QA-WEBAPPS]:
     *          (web-app-1): compatible?
     *      [PRODUCTION-WEBAPPS]:
     *          (web-app-1): compatible?
     *
     * QA-MATRIX:
     *      [DEV-WEBAPPS]:
     *          (web-app-1): compatible?
     *      [QA-WEBAPPS]:
     *          (web-app-1): compatible?
     *      [PRODUCTION-WEBAPPS]:
     *          (web-app-1): compatible?
     *
     * PRODUCTION-MATRIX:
     *      [DEV-WEBAPPS]:
     *          (web-app-1): compatible?
     *      [QA-WEBAPPS]:
     *          (web-app-1): compatible?
     *      [PRODUCTION-WEBAPPS]:
     *          (web-app-1): compatible?
     *
     * @param artifact
     * @param row
     * @param webappEnvironment
     */
    private void populateSingleCompabilityColumn(
        final Environment artifactEnvironment,
        final TestMatrixArtifact artifact,
        final CompatibilityRow row,
        final Environment webappEnvironment) {
            final Map<AppVersion, RemoteSpecificationResult> clients = specificationSource.loadAllSpecifications(webappEnvironment);
            // sort the apps (probably should sort the Map.Entry, but this is good enough for now
            final SortedSet<AppVersion> versions = Sets.newTreeSet(clients.keySet());
            for(final AppVersion version : versions) {
                final RemoteSpecificationResult remoteResult = clients.get(version);
                if (remoteResult.isSkipped()) {
                    continue;
                }

                final CompatibleSpecificationResult result;
                if (remoteResult.isSuccess()) {
                    result = CompatibleSpecificationResult.fromProctorSpecification(
                            artifactEnvironment,
                            version,
                            artifact,
                            remoteResult.getSpecificationResult().getSpecification()
                    );
                } else {
                    final String error = "Failed to load a proctor specification from "
                            + Joiner.on(", ").join(Iterables.transform(remoteResult.getFailures().keySet(), Functions.toStringFunction()));
                    result = new CompatibleSpecificationResult(version, false, error, Collections.emptySet());
                }

                row.addVersion(webappEnvironment, result);
            }
    }

    /**
     * represents a row in a compatibility matrix.
     * Contains the list of web-apps + compatibility for each environment
     *
     * For test-name by web-app break down, the compatibility should be of that web-app with a specific test + specification
     *
     * For the {matrix} by web-app break down, the compatibility should be for the web-app specification with entire matrix.
     *
     */
    public static class CompatibilityRow {
        /* all of thse should refer to dev web apps */
        final List<CompatibleSpecificationResult> dev;

        /* all of thse should refer to dev web apps */
        final List<CompatibleSpecificationResult> qa;

        /* all of thse should refer to dev web apps */
        final List<CompatibleSpecificationResult> production;


        public CompatibilityRow() {
            this.dev = Lists.newArrayList();
            this.qa = Lists.newArrayList();
            this.production = Lists.newArrayList();
        }

        public void addVersion(Environment environment, CompatibleSpecificationResult v) {
            switch (environment) {
                case WORKING:
                    dev.add(v);
                    break;
                case QA:
                    qa.add(v);
                    break;
                case PRODUCTION:
                    production.add(v);
                    break;
            }
        }

        public List<CompatibleSpecificationResult> getDev() {
            return dev;
        }

        public List<CompatibleSpecificationResult> getQa() {
            return qa;
        }

        public List<CompatibleSpecificationResult> getProduction() {
            return production;
        }
    }

    @Nullable
    private Date getUpdatedDate(final Map<String, List<Revision>> allHistories, final String testName) {
        if (allHistories == null) {
            return null;
        }
        final List<Revision> revisions = allHistories.get(testName);
        if ((revisions == null) || revisions.isEmpty()) {
            LOGGER.error(testName + " does't have any revision in allHistories.");
            return null;
        }
        return revisions.get(0).getDate();
    }

    private String getArtifactForView(final Model model, final Environment branch, final View view) {
        final TestMatrixVersion testMatrix = getCurrentMatrix(branch);
        final TestMatrixDefinition testMatrixDefinition;
        if (testMatrix == null) {
            testMatrixDefinition = new TestMatrixDefinition();
        } else {
            testMatrixDefinition = testMatrix.getTestMatrixDefinition();
        }

        model.addAttribute("branch", branch);
        model.addAttribute("session",
                           SessionViewModel.builder()
                               .setUseCompiledCSS(getConfiguration().isUseCompiledCSS())
                               .setUseCompiledJavaScript(getConfiguration().isUseCompiledJavaScript())
                                   // todo get the appropriate js compile / non-compile url
                               .build());
        model.addAttribute("testMatrixVersion", testMatrix);

        final Set<String> testNames = testMatrixDefinition.getTests().keySet();
        final Map<String, List<Revision>> allHistories = getAllHistories(branch);
        final Map<String, Long> updatedTimeMap = FluentIterable.from(testNames).toMap(new Function<String, Long>() {
            @Override
            public Long apply(final String testName) {
                final Date updatedDate = getUpdatedDate(allHistories, testName);
                if (updatedDate != null) {
                    return updatedDate.getTime();
                } else {
                    return FALLBACK_UPDATED_TIME;
                }
            }
        });
        model.addAttribute("updatedTimeMap", updatedTimeMap);

        final String errorMessage = "Apparently not impossible exception generating JSON";
        try {
            final String testMatrixJson = objectMapper.writer(new MinimalPrettyPrinter()).writeValueAsString(testMatrixDefinition);
            model.addAttribute("testMatrixDefinition", testMatrixJson);

            final Map<String, Map<String, String>> colors = Maps.newHashMap();
            for (final Entry<String, TestDefinition> entry : testMatrixDefinition.getTests().entrySet()) {
                final Map<String, String> testColors = Maps.newHashMap();
                for (final TestBucket bucket : entry.getValue().getBuckets()) {
                    final long hashedBucketName = Hashing.md5().newHasher().putString(bucket.getName(), Charsets.UTF_8).hash().asLong();
                    final int color = ((int) (hashedBucketName & 0x00FFFFFFL)) | 0x00808080; //  convert a hash of the bucket to a color, but keep it light
                    testColors.put(bucket.getName(), Integer.toHexString(color));
                }
                colors.put(entry.getKey(), testColors);
            }
            model.addAttribute("colors", colors);

            return view.getName();
        } catch (final JsonGenerationException e) {
            LOGGER.error(errorMessage, e);
            model.addAttribute("exception", toString(e));
        } catch (final JsonMappingException e) {
            LOGGER.error(errorMessage, e);
            model.addAttribute("exception", toString(e));
        } catch (final IOException e) {
            LOGGER.error(errorMessage, e);
            model.addAttribute("exception", toString(e));
        }
        model.addAttribute("error", errorMessage);
        return View.ERROR.getName();
    }

    private Map<String, List<Revision>> getAllHistories(final Environment branch) {
        try {
            return determineStoreFromEnvironment(branch).getAllHistories();
        } catch (final StoreException e) {
            LOGGER.error("Failed to get all histories from proctor store of " + branch, e);
            return null;
        }
    }

    private static String toString(final Throwable t) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }

    public static class CompatibleSpecificationResult {
        private final AppVersion appVersion;
        private final boolean isCompatible;
        private final String error;
        private final Set<String> dynamicTests;

        public CompatibleSpecificationResult(final AppVersion version,
                                             final boolean compatible,
                                             final String error,
                                             final Set<String> dynamicTest) {
            this.appVersion = version;
            isCompatible = compatible;
            this.error = error;
            this.dynamicTests = dynamicTest;
        }

        public AppVersion getAppVersion() {
            return appVersion;
        }

        public boolean isCompatible() {
            return isCompatible;
        }

        public String getError() {
            return error;
        }

        public boolean isDynamicTest(final String testName) {
            return dynamicTests.contains(testName);
        }

        @Override
        public String toString() {
            return appVersion.toString();
        }

        public String toShortString() {
            return appVersion.toShortString();
        }

        /**
         * Construct a instance from a single required test with a specification
         */
        static CompatibleSpecificationResult fromRequiredTest(
                final Environment matrixEnvironment,
                final AppVersion version,
                final TestMatrixArtifact artifact,
                final String testName,
                final TestSpecification specification
        ) {
            final Map<String, TestSpecification> requiredTests = Collections.singletonMap(testName, specification);
            final Set<String> dynamicTests = Collections.emptySet();
            return fromTests(
                    matrixEnvironment,
                    version,
                    artifact,
                    requiredTests,
                    dynamicTests,
                    (matrixSource, plr) ->
                            "test " + testName + " is required by specification but invalid for " + matrixSource
            );
        }

        /**
         * Construct a instance from a single dynamic test without a specification
         */
        static CompatibleSpecificationResult fromDynamicTest(
                final Environment matrixEnvironment,
                final AppVersion version,
                final TestMatrixArtifact artifact,
                final String testName
        ) {
            final Map<String, TestSpecification> requiredTests = Collections.emptyMap();
            final Set<String> dynamicTests = Collections.singleton(testName);
            return fromTests(
                    matrixEnvironment,
                    version,
                    artifact,
                    requiredTests,
                    dynamicTests,
                    (matrixSource, plr) ->
                            "test " + testName + " is matched in filters but invalid for " + matrixSource
            );
        }

        /**
         * Construct a instance from a proctor specification of all tests for a client
         */
        static CompatibleSpecificationResult fromProctorSpecification(
                final Environment artifactEnvironment,
                final AppVersion version,
                final TestMatrixArtifact artifact,
                final ProctorSpecification specification
        ) {
            final Map<String, TestSpecification> requiredTests = specification.getTests();
            final Set<String> dynamicTests = specification.getDynamicFilters().determineTests(artifact.getTests(), requiredTests.keySet());
            return fromTests(
                    artifactEnvironment,
                    version,
                    artifact,
                    requiredTests,
                    dynamicTests,
                    (matrixSource, plr) ->
                            String.format("Incompatible: Tests Missing: %s Invalid Tests: %s for %s",
                                    plr.getMissingTests(), plr.getTestsWithErrors(), matrixSource)
            );
        }

        private static CompatibleSpecificationResult fromTests(
                final Environment environment,
                final AppVersion version,
                final TestMatrixArtifact artifact,
                final Map<String, TestSpecification> requiredTests,
                final Set<String> dynamicTests,
                final BiFunction<String, ProctorLoadResult, String> errorMessageFunction
        ) {
            final String matrixSource = environment.getName() + " r" + artifact.getAudit().getVersion();
            final ProctorLoadResult plr = ProctorUtils.verify(artifact, matrixSource, requiredTests, dynamicTests);
            final boolean compatible = !plr.hasInvalidTests();
            final String error = (compatible ? "" : errorMessageFunction.apply(matrixSource, plr));
            return new CompatibleSpecificationResult(version, compatible, error, dynamicTests);
        }
    }
}
