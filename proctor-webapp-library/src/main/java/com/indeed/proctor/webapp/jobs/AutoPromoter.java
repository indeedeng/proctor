package com.indeed.proctor.webapp.jobs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.indeed.proctor.common.EnvironmentVersion;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.util.TestDefinitionUtil;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.indeed.proctor.webapp.db.Environment.PRODUCTION;
import static com.indeed.proctor.webapp.db.Environment.QA;
import static com.indeed.proctor.webapp.db.Environment.WORKING;
import static com.indeed.proctor.webapp.jobs.AllocationUtil.generateAllocationRangeMap;
import static com.indeed.proctor.webapp.util.NumberUtil.equalsWithinTolerance;

/**
 * Extracted autopromote logic from EditAndPromoteJob
 */
class AutoPromoter {
    private static final Logger LOGGER = Logger.getLogger(AutoPromoter.class);
    private final EditAndPromoteJob editAndPromoteJob;

    AutoPromoter(final EditAndPromoteJob editAndPromoteJob) {
        this.editAndPromoteJob = editAndPromoteJob;
    }


    /**
     * Attempts to promote {@code test} and logs the result.
     */
    public void maybeAutoPromote(
            final String testName,
            final String username,
            final String password,
            final String author,
            final TestDefinition testDefinitionToUpdate,
            final String previousRevision,
            final Environment autopromoteTarget,
            final Map<String, String[]> requestParameterMap,
            final BackgroundJob<Void> job,
            final ProctorStore trunkStore,
            final String qaRevision,
            final String prodRevision,
            final TestDefinition existingTestDefinition
    ) throws AutoPromoteException {
        final boolean isAutopromote = autopromoteTarget != WORKING;
        if (!isAutopromote) {
            job.log("Not auto-promote because it wasn't requested by user.");
            return;
        }

        try {
            switch (autopromoteTarget) {
                case QA:
                    final String currentRevision = getCurrentVersion(testName, trunkStore).getRevision();
                    doPromoteTestToEnvironment(QA, testName, username, password, author, null,
                            requestParameterMap, job, currentRevision, qaRevision, false);
                    break;
                case PRODUCTION:
                    doPromoteTestToQaAndProd(testName, username, password, author, testDefinitionToUpdate, previousRevision,
                            requestParameterMap, job, trunkStore, qaRevision, prodRevision, existingTestDefinition);
                    break;
            }
        } catch (final Exception exception) {
            throw new AutoPromoteException("Test Creation/Edit succeeded. However, the test was not promoted automatically to QA/Production.", exception);
        }
    }

    /**
     * Promote a test to QA and Prod if it's allocation-only change or it's 100% inactive test
     */
    @VisibleForTesting
    void doPromoteTestToQaAndProd(
            final String testName,
            final String username,
            final String password,
            final String author,
            final TestDefinition testDefinitionToUpdate,
            final String previousRevision,
            final Map<String, String[]> requestParameterMap,
            final BackgroundJob<Void> job,
            final ProctorStore trunkStore,
            final String qaRevision,
            final String prodRevision,
            final TestDefinition existingTestDefinition
    ) throws Exception {
        if (existingTestDefinition == null) {
            if (!isAllInactiveTest(testDefinitionToUpdate)) {
                throw new IllegalArgumentException("auto-promote is prevented because there is no existing test definition and the test is not 100% inactive.");
            }

            final String currentRevision = getCurrentVersion(testName, trunkStore).getRevision();
            doPromoteNewlyCreatedInactiveTestToQaAndProd(testName, username, password, author,
                    requestParameterMap, job, currentRevision);
        } else {
            final String currentRevision = getCurrentVersionIfDirectlyFollowing(testName, previousRevision, trunkStore).getRevision();
            doPromoteExistingTestToQaAndProd(testName, username, password, author, testDefinitionToUpdate,
                    requestParameterMap, job, currentRevision, qaRevision, prodRevision, existingTestDefinition);
        }
    }

    /**
     * Promote a test to QA and Prod without checking changes
     * This promotion logic is only for test creation
     */
    @VisibleForTesting
    void doPromoteNewlyCreatedInactiveTestToQaAndProd(
            final String testName,
            final String username,
            final String password,
            final String author,
            final Map<String, String[]> requestParameterMap,
            final BackgroundJob<Void> job,
            final String currentRevision
    ) throws Exception {
        try {
            doPromoteTestToEnvironment(QA, testName, username, password, author, null,
                    requestParameterMap, job, currentRevision, EnvironmentVersion.UNKNOWN_REVISION, false);
        } catch (final Exception e) {
            job.log("previous revision changes prevented auto-promote to PRODUCTION");
            throw e;
        }

        editAndPromoteJob.doPromoteInternal(testName, username, password, author, WORKING,
                currentRevision, PRODUCTION, EnvironmentVersion.UNKNOWN_REVISION, requestParameterMap, job, true);
    }

    /**
     * Promote a test to QA and Prod
     * If it's not allocation-only change, it doesn't promote
     */
    @VisibleForTesting
    void doPromoteExistingTestToQaAndProd(
            final String testName,
            final String username,
            final String password,
            final String author,
            final TestDefinition testDefinitionToUpdate,
            final Map<String, String[]> requestParameterMap,
            final BackgroundJob<Void> job,
            final String currentRevision,
            final String qaRevision,
            final String prodRevision,
            final TestDefinition existingTestDefinition
    ) throws Exception {
        Preconditions.checkArgument(existingTestDefinition != null);

        if (!isAllocationOnlyChange(existingTestDefinition, testDefinitionToUpdate)) {
            throw new IllegalArgumentException("auto-promote is prevented because it isn't an allocation-only change.");
        }

        job.log("allocation only change, checking against other branches for auto-promote capability for test " +
                testName + "\nat QA revision " + qaRevision + " and PRODUCTION revision " + prodRevision);

        doPromoteTestToEnvironment(QA, testName, username, password, author, testDefinitionToUpdate,
                requestParameterMap, job, currentRevision, qaRevision, true);

        doPromoteTestToEnvironment(PRODUCTION, testName, username, password, author, testDefinitionToUpdate,
                requestParameterMap, job, currentRevision, prodRevision, true);
    }

    /**
     * Promote a test to QA or Prod
     *
     * @param targetEnv target environment. Environment.QA orEnvironment.PRODUCTION
     * @param testName the test name to promote
     * @param username the username of the committer
     * @param password the password of the committer
     * @param author the author name who requests this promotion
     * @param testDefinitionToUpdate for checking allocation only change condition.
     *                              This can be null iff verifyAllocationOnly is false.
     * @param requestParameterMap
     * @param job the edit job which runs this edit process
     * @param currentRevision the revision of the source environment
     * @param targetRevision the target revision to get the target test definition to check allocation only change condition.
     *                      This can be EnvironmentVersion.UNKNOWN_REVISION iff verifyAllocationOnly is false.
     * @param verifyAllocationOnlyChange a flag to check allocation only change.
     * @throws Exception
     */
    @VisibleForTesting
    void doPromoteTestToEnvironment(
            final Environment targetEnv,
            final String testName,
            final String username,
            final String password,
            final String author,
            @Nullable final TestDefinition testDefinitionToUpdate,
            final Map<String, String[]> requestParameterMap,
            final BackgroundJob job,
            final String currentRevision,
            final String targetRevision,
            final boolean verifyAllocationOnlyChange
    ) throws Exception {
        // targetRevision is required only for verification of the allocation only change condition
        if (verifyAllocationOnlyChange && targetRevision.equals(EnvironmentVersion.UNKNOWN_REVISION)) {
            throw new IllegalArgumentException(targetEnv.getName() + " revision is unknown");
        }
        if (!QA.equals(targetEnv) && !PRODUCTION.equals(targetEnv)) {
            throw new IllegalArgumentException("Promotion target environment " + targetEnv.getName() + " is invalid.");
        }

        final TestDefinition targetTestDefinition = TestDefinitionUtil.getTestDefinitionTryCached(
                editAndPromoteJob.determineStoreFromEnvironment(targetEnv),
                targetEnv,
                testName,
                targetRevision
        );

        if (verifyAllocationOnlyChange) {
            if (testDefinitionToUpdate == null) {
                // This should never happen
                LOGGER.error("Failed to verify if the test change is allocation only change due to lack of test definition to update.");
                LOGGER.error(String.format("testName: %s,targetEnv: %s", testName, targetEnv.getName()));
                throw new IllegalStateException("Bug: Failed to verify if the test change is allocation only change due to lack of test definition to update.");
            }
            if (!isAllocationOnlyChange(targetTestDefinition, testDefinitionToUpdate)) {
                throw new IllegalArgumentException("Not auto-promote to " + targetEnv.getName() + " because it isn't an allocation-only change.");
            }
        }

        job.log("auto-promote changes to " + targetEnv.getName());
        try {
            editAndPromoteJob.doPromoteInternal(testName, username, password, author, WORKING, currentRevision,
                    targetEnv, targetRevision, requestParameterMap, job, true);
        } catch (final Exception e) {
            job.log("Error while (or after) promoting the test to " + targetEnv.getName());
            throw e;
        }
    }

    @VisibleForTesting
    static boolean isAllInactiveTest(final TestDefinition testDefinition) {
        final List<Allocation> allocations = testDefinition.getAllocations();
        final Map<Integer, TestBucket> valueToBucket = testDefinition.getBuckets()
                .stream()
                .collect(Collectors.toMap(TestBucket::getValue, Function.identity()));

        for (final Allocation allocation : allocations) {
            for (final Range range : allocation.getRanges()) {
                final TestBucket bucket = valueToBucket.get(range.getBucketValue());
                if (!isInactiveBucket(bucket) && range.getLength() > 0.0) {
                    return false;
                }
            }
        }

        return true;
    }



    private static boolean isInactiveBucket(final TestBucket bucket) {
        // Proctor does not define inactive buckets,
        // so we only assume a bucket is the inactive group
        // if it has value value -1 and one of the 2 typical names "inactive" or "disabled".
        // See further discussion in the ticket. https://bugs.indeed.com/browse/PROW-518
        return bucket.getValue() == -1 &&
                ("inactive".equalsIgnoreCase(bucket.getName()) || "disabled".equalsIgnoreCase(bucket.getName()));
    }

    /**
     * Get current revision of the test which has been updated in the edit job.
     * This method assumes that the test is updated exactly once after {@code previousRevision}
     * @param testName the name of the test
     * @param previousRevision the latest revision before updating this test
     * @param store the ProctorStore for the environment
     * @return Gets the current revision of {@code testName} to autopromote. {@code previousRevision} is used to check
     * for any modification since this edit process began.
     * @throws IllegalStateException the number of history is less than 2 or {@code previousRevision} is not the same
     * as the revision of the second latest history.
     */
    @Nonnull
    private static Revision getCurrentVersionIfDirectlyFollowing(
            final String testName,
            final String previousRevision,
            final ProctorStore store
    ) {
        final List<Revision> histories = TestDefinitionUtil.getTestHistory(store, testName, 2);

        if (histories.size() < 2) {
            throw new IllegalStateException("Test history should have at least 2 versions for edit and promote. " +
                    "Actually only has " + histories.size() + " versions");
        }
        if (!histories.get(1).getRevision().equals(previousRevision)) {
            throw new IllegalStateException("The passed previous revision was " + previousRevision +
                    " but the previous revision from the history is " + histories.get(1) +
                    ". Failed to find the version for autopromote.");
        }
        return histories.get(0);
    }

    /**
     * Get current revision of the test.
     * This method is used after saving the new test.
     * @param testName the name of the test
     * @param store the ProctorStore for the environment
     * @return the current revision of the test to autopromote.
     * @throws IllegalStateException no history exists
     */
    @Nonnull
    private static Revision getCurrentVersion(final String testName, final ProctorStore store) {
        final List<Revision> histories = TestDefinitionUtil.getTestHistory(store, testName, 1);

        if (histories.size() == 0) {
            throw new IllegalStateException("Test hasn't been created. Failed to find the version for autopromote.");
        }
        return histories.get(0);
    }

    static boolean isAllocationOnlyChange(final TestDefinition existingTestDefinition, final TestDefinition testDefinitionToUpdate) {
        final List<Allocation> existingAllocations = existingTestDefinition.getAllocations();
        final List<Allocation> allocationsToUpdate = testDefinitionToUpdate.getAllocations();
        final boolean nullRule = existingTestDefinition.getRule() == null;
        if (nullRule && testDefinitionToUpdate.getRule() != null) {
            return false;
        } else if (!nullRule && !existingTestDefinition.getRule().equals(testDefinitionToUpdate.getRule())) {
            return false;
        }
        if (!existingTestDefinition.getConstants().equals(testDefinitionToUpdate.getConstants())
                || !existingTestDefinition.getSpecialConstants().equals(testDefinitionToUpdate.getSpecialConstants())
                || !existingTestDefinition.getTestType().equals(testDefinitionToUpdate.getTestType())
                || !existingTestDefinition.getSalt().equals(testDefinitionToUpdate.getSalt())
                || !existingTestDefinition.getBuckets().equals(testDefinitionToUpdate.getBuckets())
                || existingAllocations.size() != allocationsToUpdate.size()) {
            return false;
        }

        /*
         * TestBucket .equals() override only checks name equality
         * loop below compares each attribute of a TestBucket
         */
        for (int i = 0; i < existingTestDefinition.getBuckets().size(); i++) {
            final TestBucket bucketOne = existingTestDefinition.getBuckets().get(i);
            final TestBucket bucketTwo = testDefinitionToUpdate.getBuckets().get(i);
            if (bucketOne == null) {
                if (bucketTwo != null) {
                    return false;
                }
            } else if (bucketTwo == null) {
                return false;
            } else {
                if (bucketOne.getValue() != bucketTwo.getValue()) {
                    return false;
                }
                final Payload payloadOne = bucketOne.getPayload();
                final Payload payloadTwo = bucketTwo.getPayload();
                if (payloadOne == null) {
                    if (payloadTwo != null) {
                        return false;
                    }
                } else if (!payloadOne.equals(payloadTwo)) {
                    return false;
                }
                if (bucketOne.getDescription() == null) {
                    if (bucketTwo.getDescription() != null) {
                        return false;
                    }
                } else if (!bucketOne.getDescription().equals(bucketTwo.getDescription())) {
                    return false;
                }
            }
        }

        /*
         * Comparing everything in an allocation except the lengths
         */
        for (int i = 0; i < existingAllocations.size(); i++) {
            final List<Range> existingAllocationRanges = existingAllocations.get(i).getRanges();
            final List<Range> allocationToUpdateRanges = allocationsToUpdate.get(i).getRanges();
            if (existingAllocations.get(i).getRule() == null && allocationsToUpdate.get(i).getRule() != null) {
                return false;
            } else if (existingAllocations.get(i).getRule() != null && !existingAllocations.get(i).getRule().equals(allocationsToUpdate.get(i).getRule())) {
                return false;
            }
            if (isNewBucketAdded(existingAllocationRanges, allocationToUpdateRanges)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isNewBucketAdded(
            final List<Range> existingAllocationRanges,
            final List<Range> allocationToUpdateRanges
    ) {
        final Map<Integer, Double> existingAllocRangeMap = generateAllocationRangeMap(existingAllocationRanges);
        final Map<Integer, Double> allocToUpdateRangeMap = generateAllocationRangeMap(allocationToUpdateRanges);
        return allocToUpdateRangeMap.entrySet().stream()
                .filter(bucket -> !equalsWithinTolerance(0.0, bucket.getValue()))
                .filter(bucket -> bucket.getKey() != -1)
                .anyMatch(bucket -> equalsWithinTolerance(
                        0.0,
                        existingAllocRangeMap.getOrDefault(bucket.getKey(), 0.0)));
    }

    static class AutoPromoteException extends Exception {
        AutoPromoteException(final String message) {
            super(message);
        }

        AutoPromoteException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
