package com.indeed.proctor.webapp.jobs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import com.indeed.proctor.webapp.model.ProctorSpecifications;
import com.indeed.proctor.webapp.model.RemoteSpecificationResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class MatrixChecker {
    private static final Logger LOGGER = LogManager.getLogger(MatrixChecker.class);
    private static final LibraryFunctionMapper FUNCTION_MAPPER = RuleEvaluator.defaultFunctionMapperBuilder().build();

    private final ProctorSpecificationSource specificationSource;

    @Autowired
    public MatrixChecker(
            final ProctorSpecificationSource specificationSource
    ) {
        this.specificationSource = specificationSource;
    }

    /**
     * Checks new definition is compatible with specifications of applications
     * running the test in the environment to check.
     * <p>
     * This method is useful to validate promote/delete operations
     * before making actual change on proctor store
     *
     * @param targetEnvironment environment to check against
     * @param targetTestName    name of the test to check
     * @param newDefinition     new definition to check. Pass null to check "delete".
     * @return
     */
    public CheckMatrixResult checkMatrix(
            final Environment targetEnvironment,
            final String targetTestName,
            @Nullable final TestDefinition newDefinition
    ) {
        final TestMatrixVersion tmv = new TestMatrixVersion();
        tmv.setAuthor("author");
        tmv.setVersion("");
        tmv.setDescription("fake matrix for validation of " + targetTestName);
        tmv.setPublished(new Date());

        final TestMatrixDefinition tmd = new TestMatrixDefinition();
        // The potential test definition will be null for test deletions
        if (newDefinition != null) {
            tmd.setTests(ImmutableMap.of(targetTestName, newDefinition));
        }
        tmv.setTestMatrixDefinition(tmd);

        final TestMatrixArtifact artifact = ProctorUtils.convertToConsumableArtifact(tmv);

        // Verify
        final ImmutableList.Builder<String> errorsBuilder = ImmutableList.builder();
        final Set<AppVersion> clients = specificationSource.activeClients(targetEnvironment, targetTestName);
        for (final AppVersion client : clients) {
            LOGGER.info("Verifying artifact against : cached " + client + " for " + targetTestName);
            final RemoteSpecificationResult result = specificationSource
                    .getRemoteResult(targetEnvironment, client);
            final ProctorSpecifications specifications = result.getSpecifications();

            if (specifications == null) {
                LOGGER.error(
                        "Unexpectedly " + client + " returned null specifications"
                                + ". Skipping validation."
                );
                continue;
            }

            for (final ProctorSpecification specification : specifications.asSet()) {
                final String error = verifyAndReturnError(
                        specification,
                        artifact,
                        targetTestName,
                        client
                );
                if (error != null) {
                    errorsBuilder.add(error);
                }
            }
        }

        final ImmutableList<String> errors = errorsBuilder.build();
        final boolean greatSuccess = errors.isEmpty();

        return new CheckMatrixResult(greatSuccess, errors);
    }

    @Nullable
    private String verifyAndReturnError(
            final ProctorSpecification specification,
            final TestMatrixArtifact artifact,
            final String testName,
            final AppVersion appVersion
    ) {
        try {
            final ProctorLoadResult result = verify(
                    specification,
                    artifact,
                    testName,
                    appVersion.toString()
            );
            if (result.hasInvalidTests()) {
                return getErrorMessage(appVersion, result);
            }
            return null;
        } catch (final Exception e) {
            LOGGER.error("Unable to verify " + appVersion, e);
            return appVersion.toString() + " failed. " + e.getMessage();
        }
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

    private ProctorLoadResult verify(
            final ProctorSpecification spec,
            final TestMatrixArtifact testMatrix,
            final String testName,
            final String matrixSource
    ) {
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
