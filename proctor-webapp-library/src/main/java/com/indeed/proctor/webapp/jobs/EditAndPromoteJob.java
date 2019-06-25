package com.indeed.proctor.webapp.jobs;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.EnvironmentVersion;
import com.indeed.proctor.common.IncompatibleTestMatrixException;
import com.indeed.proctor.common.ProctorPromoter;
import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.common.Serializers;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.store.GitNoAuthorizationException;
import com.indeed.proctor.store.GitNoDevelperAccessLevelException;
import com.indeed.proctor.store.GitNoMasterAccessLevelException;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.extensions.BackgroundJobLogger;
import com.indeed.proctor.webapp.extensions.DefinitionChangeLogger;
import com.indeed.proctor.webapp.extensions.PostDefinitionCreateChange;
import com.indeed.proctor.webapp.extensions.PostDefinitionEditChange;
import com.indeed.proctor.webapp.extensions.PostDefinitionPromoteChange;
import com.indeed.proctor.webapp.extensions.PreDefinitionCreateChange;
import com.indeed.proctor.webapp.extensions.PreDefinitionEditChange;
import com.indeed.proctor.webapp.extensions.PreDefinitionPromoteChange;
import com.indeed.proctor.webapp.model.RevisionDefinition;
import com.indeed.proctor.webapp.util.EncodingUtil;
import com.indeed.proctor.webapp.util.AllocationIdUtil;
import com.indeed.proctor.webapp.util.TestDefinitionUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.indeed.proctor.webapp.util.AllocationIdUtil.ALLOCATION_ID_COMPARATOR;

//Todo: Separate EditAndPromoteJob to EditJob and PromoteJob
@Component
public class EditAndPromoteJob extends AbstractJob {
    private static final Logger LOGGER = Logger.getLogger(EditAndPromoteJob.class);
    private static final Pattern ALPHA_NUMERIC_END_RESTRICTION_JAVA_IDENTIFIER_PATTERN = Pattern.compile("^([a-z_][a-z0-9_]+)?[a-z_]+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALPHA_NUMERIC_JAVA_IDENTIFIER_PATTERN = Pattern.compile("^[a-z_][a-z0-9_]*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALID_TEST_NAME_PATTERN = ALPHA_NUMERIC_END_RESTRICTION_JAVA_IDENTIFIER_PATTERN;
    private static final Pattern VALID_BUCKET_NAME_PATTERN = ALPHA_NUMERIC_JAVA_IDENTIFIER_PATTERN;
    private static final double TOLERANCE = 1E-6;

    private static final ObjectMapper OBJECT_MAPPER = Serializers.strict();

    private List<PreDefinitionEditChange> preDefinitionEditChanges = Collections.emptyList();
    private List<PostDefinitionEditChange> postDefinitionEditChanges = Collections.emptyList();
    private List<PreDefinitionCreateChange> preDefinitionCreateChanges = Collections.emptyList();
    private List<PostDefinitionCreateChange> postDefinitionCreateChanges = Collections.emptyList();
    private List<PreDefinitionPromoteChange> preDefinitionPromoteChanges = Collections.emptyList();
    private List<PostDefinitionPromoteChange> postDefinitionPromoteChanges = Collections.emptyList();

    private final BackgroundJobFactory jobFactory;
    private final BackgroundJobManager jobManager;
    private final ProctorPromoter promoter;
    private final CommentFormatter commentFormatter;
    private final MatrixChecker matrixChecker;

    @Autowired
    public EditAndPromoteJob(
            @Qualifier("trunk") final ProctorStore trunkStore,
            @Qualifier("qa") final ProctorStore qaStore,
            @Qualifier("production") final ProctorStore productionStore,
            final BackgroundJobManager jobManager,
            final BackgroundJobFactory jobFactory,
            final ProctorPromoter promoter,
            final CommentFormatter commentFormatter,
            final MatrixChecker matrixChecker
    ) {
        super(trunkStore, qaStore, productionStore);
        this.jobManager = jobManager;
        this.jobFactory = jobFactory;
        this.promoter = promoter;
        this.commentFormatter = commentFormatter;
        this.matrixChecker = matrixChecker;
    }

    @Autowired(required = false)
    public void setDefinitionEditChanges(
            final List<PreDefinitionEditChange> preDefinitionEditChanges,
            final List<PostDefinitionEditChange> postDefinitionEditChanges
    ) {
        this.preDefinitionEditChanges = preDefinitionEditChanges;
        this.postDefinitionEditChanges = postDefinitionEditChanges;
    }

    @Autowired(required = false)
    public void setDefinitionCreateChanges(
            final List<PreDefinitionCreateChange> preDefinitionCreateChanges,
            final List<PostDefinitionCreateChange> postDefinitionCreateChanges
    ) {
        this.preDefinitionCreateChanges = preDefinitionCreateChanges;
        this.postDefinitionCreateChanges = postDefinitionCreateChanges;
    }

    @Autowired(required = false)
    public void setDefinitionPromoteChanges(
            final List<PreDefinitionPromoteChange> preDefinitionPromoteChanges,
            final List<PostDefinitionPromoteChange> postDefinitionPromoteChanges
    ) {
        this.preDefinitionPromoteChanges = preDefinitionPromoteChanges;
        this.postDefinitionPromoteChanges = postDefinitionPromoteChanges;
    }

    public BackgroundJob doEdit(
            final String testName,
            final String username,
            final String password,
            final String author,
            final boolean isCreate,
            final String comment,
            final String testDefinitionJson,
            final String previousRevision,
            final Environment autopromoteTarget,
            final Map<String, String[]> requestParameterMap
    ) {
        final BackgroundJob<Object> backgroundJob = jobFactory.createBackgroundJob(
                createJobTitle(testName, username, author, isCreate, autopromoteTarget),
                author,
                createJobType(isCreate, autopromoteTarget),
                job -> {
                    try {
                        if (CharMatcher.WHITESPACE.matchesAllOf(Strings.nullToEmpty(testDefinitionJson))) {
                            throw new IllegalArgumentException("No new test definition given");
                        }
                        job.logWithTiming("Parsing test definition json", "parsing");
                        final TestDefinition testDefinitionToUpdate = parseTestDefinition(testDefinitionJson);
                        doEditInternal(testName, username, password, author, isCreate, comment, testDefinitionToUpdate, previousRevision, autopromoteTarget, requestParameterMap, job);
                    } catch (final GitNoAuthorizationException | GitNoDevelperAccessLevelException | IllegalArgumentException | IncompatibleTestMatrixException exp) {
                        job.logFailedJob(exp);
                        LOGGER.info("Edit Failed: " + job.getTitle(), exp);
                    } catch (final Exception exp) {
                        job.logFailedJob(exp);
                        LOGGER.error("Edit Failed: " + job.getTitle(), exp);
                    }
                    return null;
                }
        );
        jobManager.submit(backgroundJob);
        return backgroundJob;
    }

    private static TestDefinition parseTestDefinition(final String testDefinition) throws IOException, JsonParseException, JsonMappingException {
        final TestDefinition td = OBJECT_MAPPER.readValue(testDefinition, TestDefinition.class);
        // Until (PROC-72) is resolved, all of the 'empty' rules should get saved as NULL rules.
        if (CharMatcher.WHITESPACE.matchesAllOf(Strings.nullToEmpty(td.getRule()))) {
            td.setRule(null);
        }
        for (final Allocation ac : td.getAllocations()) {
            if (CharMatcher.WHITESPACE.matchesAllOf(Strings.nullToEmpty(ac.getRule()))) {
                ac.setRule(null);
            }
        }
        return td;
    }

    public BackgroundJob doEdit(
            final String testName,
            final String username,
            final String password,
            final String author,
            final boolean isCreate,
            final String comment,
            final TestDefinition testDefinitionToUpdate,
            final String previousRevision,
            final Environment autopromoteTarget,
            final Map<String, String[]> requestParameterMap
    ) {

        final BackgroundJob<Object> backgroundJob = jobFactory.createBackgroundJob(
                createJobTitle(testName, username, author, isCreate, autopromoteTarget),
                author,
                createJobType(isCreate, autopromoteTarget),
                job -> {
                    try {
                        doEditInternal(testName, username, password, author, isCreate, comment, testDefinitionToUpdate, previousRevision, autopromoteTarget, requestParameterMap, job);
                    } catch (final GitNoAuthorizationException | GitNoDevelperAccessLevelException | IllegalArgumentException | IncompatibleTestMatrixException exp) {
                        job.logFailedJob(exp);
                        LOGGER.info("Edit Failed: " + job.getTitle(), exp);
                    } catch (Exception exp) {
                        job.logFailedJob(exp);
                        LOGGER.error("Edit Failed: " + job.getTitle(), exp);
                    }
                    return null;
                }
        );
        jobManager.submit(backgroundJob);
        return backgroundJob;
    }

    /**
     * Creates/updates test with given {@code testName} in trunk store to new state from {@code testDefinitionToUpdate}.
     * {@code isCreate} is set if this is a new test definition. {@code autopromoteTarget} is set if the users requested the
     * test be autopromoted to the environment.
     * @param testName the test name to promote
     * @param username the username of the committer
     * @param password the password of the committer
     * @param author the author name who requests this promotion
     * @param isCreate the flag whether this job is for a test creation
     * @param comment the comment to be added in the commit comment
     * @param testDefinitionToUpdate the TestDefinition to update
     * @param previousRevision
     * @param autopromoteTarget the environment to promote
     * @param requestParameterMap
     * @param job the edit job which runs this edit process
     * @return true, else throws an exception.
     * @throws Exception
     */
    private Boolean doEditInternal(
            final String testName,
            final String username,
            final String password,
            final String author,
            final boolean isCreate,
            final String comment,
            final TestDefinition testDefinitionToUpdate,
            final String previousRevision,
            final Environment autopromoteTarget,
            final Map<String, String[]> requestParameterMap,
            final BackgroundJob job
    ) throws Exception {
        final Environment theEnvironment = Environment.WORKING; // only allow editing of TRUNK!
        final ProctorStore trunkStore = determineStoreFromEnvironment(theEnvironment);
        final EnvironmentVersion environmentVersion = promoter.getEnvironmentVersion(testName);
        final String qaRevision = environmentVersion == null ? EnvironmentVersion.UNKNOWN_REVISION : environmentVersion.getQaRevision();
        final String prodRevision = environmentVersion == null ? EnvironmentVersion.UNKNOWN_REVISION : environmentVersion.getProductionRevision();
        final String nonEmptyComment = isCreate ? formatDefaultCreateComment(testName, comment)
                : formatDefaultUpdateComment(testName, comment);

        validateUsernamePassword(username, password);
        validateComment(nonEmptyComment);

        if (previousRevision.length() > 0) {
            job.logWithTiming("(scm) getting history for '" + testName + "'", "getHistory");
            final List<Revision> history = TestDefinitionUtil.getTestHistory(trunkStore, testName, 1);
            if (history.size() > 0) {
                final Revision prevVersion = history.get(0);
                if (!prevVersion.getRevision().equals(previousRevision)) {
                    throw new IllegalArgumentException("Test has been updated since " + previousRevision + " currently at " + prevVersion.getRevision());
                }
            }
        } else {
            // check that the test name is valid
            if (!isValidTestName(testName)) {
                throw new IllegalArgumentException("Test Name must be alpha-numeric underscore and not start/end with a number, found: '" + testName + "'");
            }
        }

        job.logWithTiming("(scm) loading existing test definition for '" + testName + "'", "loadDefinition");
        // Getting the TestDefinition via currentTestMatrix instead of trunkStore.getTestDefinition because the test
        final TestDefinition existingTestDefinition = trunkStore.getCurrentTestMatrix()
                .getTestMatrixDefinition()
                .getTests()
                .entrySet()
                .stream()
                .filter(map -> testName.equalsIgnoreCase(map.getKey()))
                .map(Map.Entry::getValue)
                .findAny()
                .orElse(null);
        if (previousRevision.length() <= 0 && existingTestDefinition != null) {
            throw new IllegalArgumentException("Current tests exists with name : '" + testName + "'");
        }

        if (testDefinitionToUpdate == null) {
            throw new IllegalArgumentException("Test to update is null");
        }

        if (testDefinitionToUpdate.getTestType() == null && existingTestDefinition != null) {
            testDefinitionToUpdate.setTestType(existingTestDefinition.getTestType());
        }
        if (isCreate) {
            testDefinitionToUpdate.setVersion("-1");
            handleAllocationIdsForNewTest(testDefinitionToUpdate);
        } else if (existingTestDefinition != null) {
            testDefinitionToUpdate.setVersion(existingTestDefinition.getVersion());
            handleAllocationIdsForEditTest(testName, existingTestDefinition, testDefinitionToUpdate);
        }
        job.logWithTiming("verifying test definition and buckets", "Verify");
        validateBasicInformation(testDefinitionToUpdate, job);

        final ConsumableTestDefinition consumableTestDefinition = ProctorUtils.convertToConsumableTestDefinition(testDefinitionToUpdate);
        ProctorUtils.verifyInternallyConsistentDefinition(testName, "edit", consumableTestDefinition);

        //PreDefinitionEdit
        final DefinitionChangeLogger logger = new BackgroundJobLogger(job);
        if (isCreate) {
            job.logWithTiming("Executing pre create extension tasks.", "preCreateExtension");
            for (final PreDefinitionCreateChange preDefinitionCreateChange : preDefinitionCreateChanges) {
                preDefinitionCreateChange.preCreate(testDefinitionToUpdate, requestParameterMap, logger);
            }
        } else {
            job.logWithTiming("Executing pre edit extension tasks.", "preEditExtension");
            for (final PreDefinitionEditChange preDefinitionEditChange : preDefinitionEditChanges) {
                preDefinitionEditChange.preEdit(existingTestDefinition, testDefinitionToUpdate,
                        requestParameterMap, logger);
            }
        }

        final String fullComment = commentFormatter.formatFullComment(nonEmptyComment, requestParameterMap);

        //Change definition
        final Map<String, String> metadata = Collections.emptyMap();
        if (existingTestDefinition == null) {
            job.logWithTiming("(scm) adding test definition", "scmAdd");
            trunkStore.addTestDefinition(username, password, author, testName, testDefinitionToUpdate, metadata, fullComment);
        } else {
            job.logWithTiming("(scm) updating test definition", "scmUpdate");
            trunkStore.updateTestDefinition(username, password, author, previousRevision, testName, testDefinitionToUpdate, metadata, fullComment);
        }

        //PostDefinitionEdit
        if (isCreate) {
            job.logWithTiming("Executing post create extension tasks.", "postCreateExtension");
            for (final PostDefinitionCreateChange postDefinitionCreateChange : postDefinitionCreateChanges) {
                postDefinitionCreateChange.postCreate(testDefinitionToUpdate, requestParameterMap, logger);
            }
        } else {
            job.logWithTiming("Executing post edit extension tasks.", "postEditExtension");
            for (final PostDefinitionEditChange postDefinitionEditChange : postDefinitionEditChanges) {
                postDefinitionEditChange.postEdit(existingTestDefinition, testDefinitionToUpdate,
                        requestParameterMap, logger);
            }
        }

        //Autopromote if necessary
        maybeAutoPromote(testName, username, password, author, testDefinitionToUpdate, previousRevision,
                autopromoteTarget, requestParameterMap, job, trunkStore, qaRevision, prodRevision, existingTestDefinition);

        job.logComplete();
        job.addUrl("/proctor/definition/" + EncodingUtil.urlEncodeUtf8(testName) + "?branch=" + theEnvironment.getName(), "View Result");
        return true;
    }

    /**
     * Attempts to promote {@code test} and logs the result.
     */
    private void maybeAutoPromote(
            final String testName,
            final String username,
            final String password,
            final String author,
            final TestDefinition testDefinitionToUpdate,
            final String previousRevision,
            final Environment autopromoteTarget,
            final Map<String, String[]> requestParameterMap,
            final BackgroundJob job,
            final ProctorStore trunkStore,
            final String qaRevision,
            final String prodRevision,
            final TestDefinition existingTestDefinition
    ) throws Exception {
        final boolean isAutopromote = autopromoteTarget != Environment.WORKING;
        if (!isAutopromote) {
            job.log("Not auto-promote because it wasn't requested by user.");
            return;
        }

        switch (autopromoteTarget) {
            case QA:
                final String currentRevision = getCurrentVersion(testName, trunkStore).getRevision();
                doPromoteTestToEnvironment(Environment.QA, testName, username, password, author, null,
                        requestParameterMap, job, currentRevision, qaRevision, false);
                break;
            case PRODUCTION:
                doPromoteTestToQaAndProd(testName, username, password, author, testDefinitionToUpdate, previousRevision,
                        requestParameterMap, job, trunkStore, qaRevision, prodRevision, existingTestDefinition);
                break;
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
            final BackgroundJob job,
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
            final BackgroundJob job,
            final String currentRevision
    ) throws Exception {
        try {
            doPromoteTestToEnvironment(Environment.QA, testName, username, password, author, null,
                    requestParameterMap, job, currentRevision, EnvironmentVersion.UNKNOWN_REVISION, false);
        } catch (final Exception e) {
            job.log("previous revision changes prevented auto-promote to PRODUCTION");
            throw e;
        }

        doPromoteInternal(testName, username, password, author, Environment.WORKING,
                currentRevision, Environment.PRODUCTION, EnvironmentVersion.UNKNOWN_REVISION, requestParameterMap, job, true);
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
            final BackgroundJob job,
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

        doPromoteTestToEnvironment(Environment.QA, testName, username, password, author, testDefinitionToUpdate,
                requestParameterMap, job, currentRevision, qaRevision, true);

        doPromoteTestToEnvironment(Environment.PRODUCTION, testName, username, password, author, testDefinitionToUpdate,
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

        final TestDefinition targetTestDefinition = TestDefinitionUtil.getTestDefinition(
                determineStoreFromEnvironment(targetEnv),
                promoter,
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

        switch (targetEnv) {
            case QA:
            case PRODUCTION:
                job.log("auto-promote changes to " + targetEnv.getName());

                try {
                    doPromoteInternal(testName, username, password, author, Environment.WORKING, currentRevision,
                            targetEnv, targetRevision, requestParameterMap, job, true);
                } catch (final Exception e) {
                    job.log("Failed to promote the test to " + targetEnv.getName());
                    throw e;
                }
                break;
            default:
                throw new IllegalArgumentException("Promotion target environment " + targetEnv.getName() + " is invalid.");
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

    /**
     * @param testName the proctor test name
     * @param previous test definition before edit
     * @param current  test definition after edit
     */
    private void handleAllocationIdsForEditTest(final String testName, final TestDefinition previous, final TestDefinition current) {
        // Update allocation id if necessary
        final Set<Allocation> outdatedAllocations = AllocationIdUtil.getOutdatedAllocations(previous, current);
        for (final Allocation allocation : outdatedAllocations) {
            allocation.setId(AllocationIdUtil.getNextVersionOfAllocationId(allocation.getId()));
        }

        /*
         * Check whether has new allocations.
         * Doing this check to avoid getMaxUsedAllocationIdForTest when unnecessary because getMaxUsedAllocationIdForTest takes time
         */
        final boolean needNewAllocId = current.getAllocations().stream().anyMatch(
                x -> StringUtils.isEmpty(x.getId())
        );
        // Generate new allocation id if any allocation id is empty
        if (needNewAllocId) {
            // Get the max allocation id ever used from test definition history, including deleted allocations in the format like "#Z1"
            final Optional<String> maxAllocId = getMaxUsedAllocationIdForTest(testName);
            // Convert maxAllocId to base 10 integer, so that we can easily increment it
            int maxAllocIdInt = maxAllocId.isPresent() ? AllocationIdUtil.convertBase26ToDecimal(AllocationIdUtil.getAllocationName(maxAllocId.get()).toCharArray()) : -1;
            for (final Allocation allocation : current.getAllocations()) {
                // Only generate for new allocation
                if (StringUtils.isEmpty(allocation.getId())) {
                    allocation.setId(AllocationIdUtil.generateAllocationId(++maxAllocIdInt, 1));
                }
            }
        }
    }

    /**
     * @param testName the proctor test name
     * @return the max allocation id ever used in the format like "#Z1"
     */
    @Nullable
    private Optional<String> getMaxUsedAllocationIdForTest(final String testName) {
        // Use trunk store
        final ProctorStore trunkStore = determineStoreFromEnvironment(Environment.WORKING);
        final List<RevisionDefinition> revisionDefinitions = TestDefinitionUtil.makeRevisionDefinitionList(trunkStore, testName, null, true);
        return getMaxAllocationId(revisionDefinitions);
    }

    static Optional<String> getMaxAllocationId(final List<RevisionDefinition> revisionDefinitions) {
        return revisionDefinitions.stream().map(RevisionDefinition::getDefinition)
                .filter(Objects::nonNull)
                .flatMap(x -> x.getAllocations().stream())
                .map(Allocation::getId)
                .filter(StringUtils::isNotEmpty)
                .distinct()
                .max(ALLOCATION_ID_COMPARATOR);
    }

    /**
     * @param testDefinition the test definition to generate allocation ids for
     */
    private void handleAllocationIdsForNewTest(final TestDefinition testDefinition) {
        for (int i = 0; i < testDefinition.getAllocations().size(); i++) {
            final Allocation allocation = testDefinition.getAllocations().get(i);
            allocation.setId(AllocationIdUtil.generateAllocationId(i, 1));
        }
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
                .filter(bucket -> bucket.getValue() > TOLERANCE)
                .filter(bucket -> bucket.getKey() != -1)
                .anyMatch(bucket -> existingAllocRangeMap.getOrDefault(bucket.getKey(), 0.0) < TOLERANCE);
    }

    private static Map<Integer, Double> generateAllocationRangeMap(final List<Range> ranges) {
        final Map<Integer, Double> bucketToTotalAllocationMap = new HashMap<>();
        for (final Range range : ranges) {
            final int bucketVal = range.getBucketValue();
            double sum = range.getLength();
            final Double allocationValue = bucketToTotalAllocationMap.get(bucketVal);
            if (allocationValue != null) {
                sum += allocationValue;
            }
            bucketToTotalAllocationMap.put(bucketVal, sum);
        }
        return bucketToTotalAllocationMap;
    }

    private void validateBasicInformation(
            final TestDefinition definition,
            final BackgroundJob backgroundJob
    ) throws IllegalArgumentException {
        if (CharMatcher.WHITESPACE.matchesAllOf(Strings.nullToEmpty(definition.getDescription()))) {
            throw new IllegalArgumentException("Description is required.");
        }
        if (CharMatcher.WHITESPACE.matchesAllOf(Strings.nullToEmpty(definition.getSalt()))) {
            throw new IllegalArgumentException("Salt is required.");
        }
        if (definition.getTestType() == null) {
            throw new IllegalArgumentException("TestType is required.");
        }

        if (definition.getBuckets().isEmpty()) {
            throw new IllegalArgumentException("Buckets cannot be empty.");
        }

        if (definition.getAllocations().isEmpty()) {
            throw new IllegalArgumentException("Allocations cannot be empty.");
        }

        validateAllocationsAndBuckets(definition, backgroundJob);
    }

    private void validateAllocationsAndBuckets(final TestDefinition definition, final BackgroundJob backgroundJob) throws IllegalArgumentException {
        final Allocation allocation = definition.getAllocations().get(0);
        final List<Range> ranges = allocation.getRanges();
        final TestType testType = definition.getTestType();
        final int controlBucketValue = 0;

        final Map<Integer, Double> totalTestAllocationMap = generateAllocationRangeMap(ranges);

        final boolean hasControlBucket = totalTestAllocationMap.containsKey(controlBucketValue);
        /* The number of buckets with allocation greater than zero */
        int numActiveBuckets = 0;

        for (final Map.Entry<Integer, Double> integerDoubleEntry : totalTestAllocationMap.entrySet()) {
            final double totalBucketAllocation = integerDoubleEntry.getValue();
            if (totalBucketAllocation > 0) {
                numActiveBuckets++;
            }
        }

        /* if there are 2 buckets with positive allocations, test and control buckets
            should be the same size
        */
        if (numActiveBuckets > 1 && hasControlBucket) {
            final double totalControlBucketAllocation = totalTestAllocationMap.get(controlBucketValue);
            for (final Map.Entry<Integer, Double> integerDoubleEntry : totalTestAllocationMap.entrySet()) {
                final double totalBucketAllocation = integerDoubleEntry.getValue();
                if (totalBucketAllocation > 0) {
                    numActiveBuckets++;
                }
                final double difference = totalBucketAllocation - totalControlBucketAllocation;
                if (integerDoubleEntry.getKey() > 0 && totalBucketAllocation > 0 && Math.abs(difference) >= TOLERANCE) {
                    backgroundJob.log("WARNING: Positive bucket total allocation size not same as control bucket total allocation size. \nBucket #" + integerDoubleEntry.getKey() + "=" + totalBucketAllocation + ", Zero Bucket=" + totalControlBucketAllocation);
                }
            }
        }

        /* If there are 2 buckets with positive allocations, one should be control */
        if (numActiveBuckets > 1 && !hasControlBucket) {
            backgroundJob.log("WARNING: You should have a zero bucket (control).");
        }

        for (final TestBucket bucket : definition.getBuckets()) {
            if (testType == TestType.PAGE && bucket.getValue() < 0) {
                throw new IllegalArgumentException("PAGE tests cannot contain negative buckets.");
            }
        }

        for (final TestBucket bucket : definition.getBuckets()) {
            final String name = bucket.getName();
            if (!isValidBucketName(name)) {
                throw new IllegalArgumentException("Bucket name must be alpha-numeric underscore and not start with a number, found: '" + name + "'");
            }
        }
    }

    static boolean isValidTestName(final String testName) {
        final Matcher m = VALID_TEST_NAME_PATTERN.matcher(testName);
        return m.matches();
    }

    static boolean isValidBucketName(final String bucketName) {
        final Matcher m = VALID_BUCKET_NAME_PATTERN.matcher(bucketName);
        return m.matches();
    }

    private String formatDefaultUpdateComment(final String testName, final String comment) {
        if (Strings.isNullOrEmpty(comment)) {
            return String.format("Updating A/B test %s", testName);
        }
        return comment;
    }

    private String formatDefaultCreateComment(final String testName, final String comment) {
        if (Strings.isNullOrEmpty(comment)) {
            return String.format("Creating A/B test %s", testName);
        }
        return comment;
    }

    private String createJobTitle(
            final String testName,
            final String username,
            final String author,
            final boolean isCreate,
            final Environment autopromoteTarget
    ) {
        return String.format(
                "(username:%s author:%s) %s %s",
                username, author, createJobTitleString(isCreate, autopromoteTarget), testName
        );
    }

    private static String createJobTitleString(final boolean isCreate, final Environment autopromoteTarget) {
        final StringBuilder messageBuilder = new StringBuilder();

        if (isCreate) {
            messageBuilder.append("Creating");
        } else {
            messageBuilder.append("Editing");
        }

        switch (autopromoteTarget) {
            case QA:
                messageBuilder.append(" and promoting to QA");
                break;
            case PRODUCTION:
                messageBuilder.append(" and promoting to QA and Prod");
                break;
            default:
                break;
        }

        return messageBuilder.toString();
    }

    @VisibleForTesting
    static BackgroundJob.JobType createJobType(
            final boolean isCreate,
            final Environment autopromoteTarget
    ) {
        if (isCreate) {
            switch (autopromoteTarget) {
                case QA:
                    return BackgroundJob.JobType.TEST_CREATION_PROMOTION_QA;
                case PRODUCTION:
                    return BackgroundJob.JobType.TEST_CREATION_PROMOTION;
                default:
                    return BackgroundJob.JobType.TEST_CREATION;
            }
        }

        switch (autopromoteTarget) {
            case QA:
                return BackgroundJob.JobType.TEST_EDIT_PROMOTION_QA;
            case PRODUCTION:
                return BackgroundJob.JobType.TEST_EDIT_PROMOTION;
            default:
                return BackgroundJob.JobType.TEST_EDIT;
        }
    }

    public BackgroundJob doPromote(
            final String testName,
            final String username,
            final String password,
            final String author,
            final Environment source,
            final String srcRevision,
            final Environment destination,
            final String destRevision,
            final Map<String, String[]> requestParameterMap
    ) {
        final BackgroundJob<Object> backgroundJob = jobFactory.createBackgroundJob(
                String.format("(username:%s author:%s) promoting %s %s %1.7s to %s", username, author, testName, source, srcRevision, destination),
                author,
                BackgroundJob.JobType.TEST_PROMOTION,
                job -> {
                    /*
                        Valid permutations:
                        TRUNK -> QA
                        TRUNK -> PRODUCTION
                        QA -> PRODUCTION
                     */
                    try {
                        doPromoteInternal(testName, username, password, author, source, srcRevision, destination, destRevision, requestParameterMap, job, false);
                    } catch (final GitNoAuthorizationException | GitNoMasterAccessLevelException | GitNoDevelperAccessLevelException | IllegalArgumentException exp) {
                        job.logFailedJob(exp);
                        LOGGER.info("Promotion Failed: " + job.getTitle(), exp);
                    } catch (final Exception exp) {
                        job.logFailedJob(exp);
                        LOGGER.error("Promotion Failed: " + job.getTitle(), exp);
                    }
                    return null;
                }
        );
        jobManager.submit(backgroundJob);
        return backgroundJob;
    }

    @VisibleForTesting
    Boolean doPromoteInternal(
            final String testName,
            final String username,
            final String password,
            final String author,
            final Environment source,
            final String srcRevision,
            final Environment destination,
            final String destRevision,
            final Map<String, String[]> requestParameterMap,
            final BackgroundJob job,
            final boolean isAutopromote
    ) throws Exception {
        final Map<String, String> metadata = Collections.emptyMap();
        validateUsernamePassword(username, password);

        // TODO (parker) 9/5/12 - Verify that promoting to the destination branch won't cause issues
        final TestDefinition testDefintion = TestDefinitionUtil.getTestDefinition(determineStoreFromEnvironment(source), promoter, source, testName, srcRevision);
        //            if (d == null) {
        //                return "could not find " + testName + " on " + source + " with revision " + srcRevision;
        //            }
        job.logWithTiming("Validating Matrix.", "matrixCheck");
        final MatrixChecker.CheckMatrixResult result = matrixChecker.checkMatrix(destination, testName, testDefintion);
        if (!result.isValid()) {
            throw new IllegalArgumentException(String.format("Test Promotion not compatible, errors: %s", Joiner.on("\n").join(result.getErrors())));
        } else {
            final Map<Environment, PromoteAction> actions = PROMOTE_ACTIONS.get(source);
            if (actions == null || !actions.containsKey(destination)) {
                throw new IllegalArgumentException("Invalid combination of source and destination: source=" + source + " dest=" + destination);
            }
            final PromoteAction action = actions.get(destination);

            //PreDefinitionPromoteChanges
            job.logWithTiming("Executing pre promote extension tasks.", "prePromoteExtension");
            final DefinitionChangeLogger logger = new BackgroundJobLogger(job);
            for (final PreDefinitionPromoteChange preDefinitionPromoteChange : preDefinitionPromoteChanges) {
                preDefinitionPromoteChange.prePromote(testDefintion, requestParameterMap, source, destination,
                        isAutopromote, logger);
            }

            //Promote Change
            job.logWithTiming("Promoting experiment", "promote");
            final boolean success = action.promoteTest(job, testName, srcRevision, destRevision, username, password, author, metadata);

            //PostDefinitionPromoteChanges
            job.logWithTiming("Executing post promote extension tasks.", "postPromoteExtension");
            for (final PostDefinitionPromoteChange postDefinitionPromoteChange : postDefinitionPromoteChanges) {
                postDefinitionPromoteChange.postPromote(requestParameterMap, source, destination, isAutopromote,
                        logger);
            }

            job.log(String.format("Promoted %s from %s (%1.7s) to %s (%1.7s)", testName, source.getName(), srcRevision, destination.getName(), destRevision));
            job.addUrl("/proctor/definition/" + EncodingUtil.urlEncodeUtf8(testName) + "?branch=" + destination.getName(), "view " + testName + " on " + destination.getName());
            return success;
        }
    }

    private interface PromoteAction {
        Environment getSource();

        Environment getDestination();

        boolean promoteTest(
                final BackgroundJob job,
                final String testName,
                final String srcRevision,
                final String destRevision,
                final String username,
                final String password,
                final String author,
                final Map<String, String> metadata
        ) throws IllegalArgumentException, ProctorPromoter.TestPromotionException, StoreException.TestUpdateException;
    }

    private abstract class PromoteActionBase implements PromoteAction {
        final Environment src;
        final Environment destination;

        protected PromoteActionBase(final Environment src,
                                    final Environment destination
        ) {
            this.destination = destination;
            this.src = src;
        }

        @Override
        public boolean promoteTest(
                final BackgroundJob job,
                final String testName,
                final String srcRevision,
                final String destRevision,
                final String username,
                final String password,
                final String author,
                final Map<String, String> metadata
        ) throws IllegalArgumentException, ProctorPromoter.TestPromotionException, StoreException.TestUpdateException, StoreException.TestUpdateException {
            try {
                doPromotion(job, testName, srcRevision, destRevision, username, password, author, metadata);
                return true;
            } catch (final Exception t) {
                Throwables.propagateIfInstanceOf(t, ProctorPromoter.TestPromotionException.class);
                Throwables.propagateIfInstanceOf(t, StoreException.TestUpdateException.class);
                throw Throwables.propagate(t);
            }
        }

        @Override
        public final Environment getSource() {
            return src;
        }

        @Override
        public final Environment getDestination() {
            return destination;
        }

        abstract void doPromotion(BackgroundJob job, String testName, String srcRevision, String destRevision,
                                  String username, String password, String author, Map<String, String> metadata)
                throws ProctorPromoter.TestPromotionException, StoreException;
    }

    private final PromoteAction TRUNK_TO_QA = new PromoteActionBase(Environment.WORKING,
            Environment.QA) {
        @Override
        void doPromotion(
                final BackgroundJob job,
                final String testName,
                final String srcRevision,
                final String destRevision,
                final String username,
                final String password,
                final String author,
                final Map<String, String> metadata
        ) throws ProctorPromoter.TestPromotionException, StoreException {
            job.log(String.format("(scm) promote %s %1.7s (trunk to qa)", testName, srcRevision));
            promoter.promoteTrunkToQa(testName, srcRevision, destRevision, username, password, author, metadata);
        }
    };

    private final PromoteAction TRUNK_TO_PRODUCTION = new PromoteActionBase(Environment.WORKING,
            Environment.PRODUCTION) {
        @Override
        void doPromotion(
                final BackgroundJob job,
                final String testName,
                final String srcRevision,
                final String destRevision,
                final String username,
                final String password,
                final String author,
                final Map<String, String> metadata
        ) throws ProctorPromoter.TestPromotionException, StoreException {
            job.log(String.format("(scm) promote %s %1.7s (trunk to production)", testName, srcRevision));
            promoter.promoteTrunkToProduction(testName, srcRevision, destRevision, username, password, author, metadata);
        }
    };

    private final PromoteAction QA_TO_PRODUCTION = new PromoteActionBase(Environment.QA,
            Environment.PRODUCTION) {
        @Override
        void doPromotion(
                final BackgroundJob job,
                final String testName,
                final String srcRevision,
                final String destRevision,
                final String username,
                final String password,
                final String author,
                final Map<String, String> metadata
        ) throws ProctorPromoter.TestPromotionException, StoreException {
            job.log(String.format("(scm) promote %s %1.7s (qa to production)", testName, srcRevision));
            promoter.promoteQaToProduction(testName, srcRevision, destRevision, username, password, author, metadata);
        }
    };

    private final Map<Environment, Map<Environment, PromoteAction>> PROMOTE_ACTIONS = ImmutableMap.<Environment, Map<Environment, PromoteAction>>builder()
            .put(Environment.WORKING, ImmutableMap.of(
                    Environment.QA, TRUNK_TO_QA,
                    Environment.PRODUCTION, TRUNK_TO_PRODUCTION))
            .put(Environment.QA, ImmutableMap.of(
                    Environment.PRODUCTION, QA_TO_PRODUCTION))
            .build();
}
