package com.indeed.proctor.webapp.jobs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.indeed.proctor.common.IncompatibleTestMatrixException;
import com.indeed.proctor.common.ProctorLoadResult;
import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.common.RuleEvaluator;
import com.indeed.proctor.common.TestSpecification;
import com.indeed.proctor.common.el.LibraryFunctionMapper;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.proctor.common.model.TestMatrixDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.webapp.ProctorSpecificationSource;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.model.AppVersion;
import com.indeed.proctor.webapp.model.WebappConfiguration;
import com.indeed.proctor.webapp.util.threads.LogOnUncaughtExceptionHandler;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

@Component
public class MatrixChecker {
    private static final Logger LOGGER = Logger.getLogger(MatrixChecker.class);
    private static final LibraryFunctionMapper FUNCTION_MAPPER = RuleEvaluator.defaultFunctionMapperBuilder().build();

    private final ProctorSpecificationSource specificationSource;
    private final ExecutorService verifierExecutor;

    @Autowired
    public MatrixChecker(
            final ProctorSpecificationSource specificationSource,
            final WebappConfiguration configuration
    ) {
        this.specificationSource = specificationSource;
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("proctor-verifiers-Thread-%d")
                .setUncaughtExceptionHandler(new LogOnUncaughtExceptionHandler())
                .build();
        this.verifierExecutor = Executors.newFixedThreadPool(configuration.getVerifyExecutorThreads(), threadFactory);
    }

    public CheckMatrixResult checkMatrix(final Environment checkAgainst,
                                         final String testName,
                                         final TestDefinition potential) {
        final TestMatrixVersion tmv = new TestMatrixVersion();
        tmv.setAuthor("author");
        tmv.setVersion("");
        tmv.setDescription("fake matrix for validation of " + testName);
        tmv.setPublished(new Date());

        final TestMatrixDefinition tmd = new TestMatrixDefinition();
        // The potential test definition will be null for test deletions
        if (potential != null) {
            tmd.setTests(ImmutableMap.of(testName, potential));
        }
        tmv.setTestMatrixDefinition(tmd);

        final TestMatrixArtifact artifact = ProctorUtils.convertToConsumableArtifact(tmv);
        // Verify
        final Map<AppVersion, Future<ProctorLoadResult>> futures = Maps.newLinkedHashMap();

        specificationSource.loadAllSuccessfulSpecifications(checkAgainst)
                .forEach((appVersion, specification) -> futures.put(appVersion, verifierExecutor.submit(() -> {
                    LOGGER.info("Verifying artifact against : cached "
                            + appVersion + " for " + testName);
                    return verify(
                            specification,
                            artifact,
                            testName,
                            appVersion.toString()
                    );
                })));

        final ImmutableList.Builder<String> errorsBuilder = ImmutableList.builder();
        while (!futures.isEmpty()) {
            try {
                Thread.sleep(10);
            } catch (final InterruptedException e) {
                LOGGER.error("Oh heavens", e);
            }
            for (final Iterator<Map.Entry<AppVersion, Future<ProctorLoadResult>>> iterator = futures.entrySet().iterator(); iterator.hasNext(); ) {
                final Map.Entry<AppVersion, Future<ProctorLoadResult>> entry = iterator.next();
                final AppVersion version = entry.getKey();
                final Future<ProctorLoadResult> future = entry.getValue();
                if (future.isDone()) {
                    iterator.remove();
                    try {
                        final ProctorLoadResult proctorLoadResult = future.get();
                        if (proctorLoadResult.hasInvalidTests()) {
                            errorsBuilder.add(getErrorMessage(version, proctorLoadResult));
                        }
                    } catch (final InterruptedException e) {
                        errorsBuilder.add(version.toString() + " failed. " + e.getMessage());
                        LOGGER.error("Interrupted getting " + version, e);
                    } catch (final ExecutionException e) {
                        final Throwable cause = e.getCause();
                        errorsBuilder.add(version.toString() + " failed. " + cause.getMessage());
                        LOGGER.error("Unable to verify " + version, cause);
                    }
                }
            }
        }

        final ImmutableList<String> errors = errorsBuilder.build();
        final boolean greatSuccess = errors.isEmpty();

        return new CheckMatrixResult(greatSuccess, errors);
    }

    public static class CheckMatrixResult {
        final boolean isValid;
        final List<String> errors;

        private CheckMatrixResult(final boolean valid, final List<String> errors) {
            isValid = valid;
            this.errors = errors;
        }

        public boolean isValid() {
            return isValid;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    @VisibleForTesting
    static String getErrorMessage(final AppVersion appVersion, final ProctorLoadResult proctorLoadResult) {
        final Map<String, IncompatibleTestMatrixException> testsWithErrors = proctorLoadResult.getTestErrorMap();
        final Set<String> missingTests = proctorLoadResult.getMissingTests();

        // We expect at most one test to have a problem because we limited the verification to a single test
        if (!testsWithErrors.isEmpty()) {
            final String testName = testsWithErrors.keySet().iterator().next();
            final String errorMessage = testsWithErrors.get(testName).getMessage();
            return String.format("%s cannot load test '%s': %s", appVersion, testName, errorMessage);
        } else if (!missingTests.isEmpty()) {
            return String.format("%s requires test '%s'", appVersion, missingTests.iterator().next());
        } else {
            return "";
        }
    }

    private ProctorLoadResult verify(final ProctorSpecification spec,
                                     final TestMatrixArtifact testMatrix,
                                     final String testName,
                                     final String matrixSource) {
        final Map<String, TestSpecification> requiredTests =
                Optional.ofNullable(spec.getTests())
                        .map(map -> map.get(testName))
                        .map(s -> (Map<String, TestSpecification>) ImmutableMap.of(testName, s))
                        .orElse(Collections.emptyMap());

        return ProctorUtils.verify(
                testMatrix,
                matrixSource,
                requiredTests,
                FUNCTION_MAPPER,
                ProctorUtils.convertContextToTestableMap(spec.getProvidedContext())
        );
    }
}
