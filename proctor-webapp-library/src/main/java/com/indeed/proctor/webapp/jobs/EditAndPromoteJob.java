package com.indeed.proctor.webapp.jobs;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.indeed.proctor.common.EnvironmentVersion;
import com.indeed.proctor.common.IncompatibleTestMatrixException;
import com.indeed.proctor.common.ProctorPromoter;
import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.common.Serializers;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.store.ChangeMetadata;
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
import com.indeed.proctor.webapp.util.AllocationIdUtil;
import com.indeed.proctor.webapp.util.EncodingUtil;
import com.indeed.proctor.webapp.util.TestDefinitionUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.indeed.proctor.webapp.db.Environment.PRODUCTION;
import static com.indeed.proctor.webapp.db.Environment.QA;
import static com.indeed.proctor.webapp.db.Environment.WORKING;
import static com.indeed.proctor.webapp.jobs.AllocationUtil.generateAllocationRangeMap;
import static com.indeed.proctor.webapp.util.AllocationIdUtil.ALLOCATION_ID_COMPARATOR;
import static com.indeed.proctor.webapp.util.NumberUtil.equalsWithinTolerance;

//Todo: Separate EditAndPromoteJob to EditJob and PromoteJob
@Component
public class EditAndPromoteJob extends AbstractJob {
    private static final Logger LOGGER = Logger.getLogger(EditAndPromoteJob.class);
    private static final Pattern ALPHA_NUMERIC_END_RESTRICTION_JAVA_IDENTIFIER_PATTERN = Pattern.compile("^([a-z_][a-z0-9_]+)?[a-z_]+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ALPHA_NUMERIC_JAVA_IDENTIFIER_PATTERN = Pattern.compile("^[a-z_][a-z0-9_]*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALID_TEST_NAME_PATTERN = ALPHA_NUMERIC_END_RESTRICTION_JAVA_IDENTIFIER_PATTERN;
    private static final Pattern VALID_BUCKET_NAME_PATTERN = ALPHA_NUMERIC_JAVA_IDENTIFIER_PATTERN;

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
    private final AutoPromoter autoPromoter;

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
        this.autoPromoter = new AutoPromoter(this);
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

    public BackgroundJob<Void> doEdit(
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
        final BackgroundJob<Void> backgroundJob = jobFactory.createBackgroundJob(
                createJobTitle(testName, username, author, isCreate, autopromoteTarget),
                author,
                createJobType(isCreate, autopromoteTarget),
                job -> {
                    try {
                        if (StringUtils.isBlank(testDefinitionJson)) {
                            throw new IllegalArgumentException("No new test definition given");
                        }
                        job.logWithTiming("Parsing test definition json", "parsing");
                        final TestDefinition testDefinitionToUpdate = parseTestDefinition(testDefinitionJson);
                        job.logWithTiming("Finished parsing test definition json", "parsing");
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
        if (StringUtils.isBlank(td.getRule())) {
            td.setRule(null);
        }
        for (final Allocation ac : td.getAllocations()) {
            if (StringUtils.isBlank(ac.getRule())) {
                ac.setRule(null);
            }
        }
        return td;
    }

    public BackgroundJob<Void> doEdit(
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

        final BackgroundJob<Void> backgroundJob = jobFactory.createBackgroundJob(
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
            final BackgroundJob<Void> job
    ) throws Exception {
        final Environment theEnvironment = WORKING; // only allow editing of TRUNK!
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
        job.log("(scm) Success: getting history for '" + testName + "'");

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
            testDefinitionToUpdate.setVersion(EnvironmentVersion.UNKNOWN_VERSION);
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
            if (!preDefinitionCreateChanges.isEmpty()) {
                job.logWithTiming("Executing pre create extension tasks.", "preCreateExtension");
                for (final PreDefinitionCreateChange preDefinitionCreateChange : preDefinitionCreateChanges) {
                    preDefinitionCreateChange.preCreate(testDefinitionToUpdate, requestParameterMap, logger);
                }
                job.log("Finished pre create extension tasks.");
            }
        } else {
            if (!preDefinitionEditChanges.isEmpty()) {
                job.logWithTiming("Executing pre edit extension tasks.", "preEditExtension");
                for (final PreDefinitionEditChange preDefinitionEditChange : preDefinitionEditChanges) {
                    preDefinitionEditChange.preEdit(existingTestDefinition, testDefinitionToUpdate,
                            requestParameterMap, logger);
                }
                job.log("Finished pre edit extension tasks.");
            }
        }

        // Change definition
        final ChangeMetadata changeMeataData = ChangeMetadata.builder()
                .setUsername(username)
                .setPassword(password)
                .setAuthor(author)
                .setComment(commentFormatter.formatFullComment(nonEmptyComment, requestParameterMap))
                .build();
        if (existingTestDefinition == null) {
            job.logWithTiming("(scm) adding test definition in trunk", "scmAdd");
            trunkStore.addTestDefinition(
                    changeMeataData,
                    testName,
                    testDefinitionToUpdate,
                    Collections.emptyMap());
            job.log("(scm) Success: adding test definition in trunk");
        } else {
            job.logWithTiming("(scm) updating test definition in trunk", "scmUpdate");
            trunkStore.updateTestDefinition(
                    changeMeataData,
                    previousRevision, testName, testDefinitionToUpdate, Collections.emptyMap());
            job.log("(scm) Success: updating test definition in trunk");
        }

        // PostDefinitionEdit
        if (isCreate) {
            if (!postDefinitionCreateChanges.isEmpty()) {
                job.logWithTiming("Executing post create extension tasks.", "postCreateExtension");
                for (final PostDefinitionCreateChange postDefinitionCreateChange : postDefinitionCreateChanges) {
                    postDefinitionCreateChange.postCreate(testDefinitionToUpdate, requestParameterMap, logger);
                }
                job.log("Finished post create extension tasks.");
            }
        } else {
            if (!postDefinitionEditChanges.isEmpty()) {
                job.logWithTiming("Executing post edit extension tasks.", "postEditExtension");
                for (final PostDefinitionEditChange postDefinitionEditChange : postDefinitionEditChanges) {
                    postDefinitionEditChange.postEdit(existingTestDefinition, testDefinitionToUpdate,
                            requestParameterMap, logger);
                }
                job.log("Finished post edit extension tasks.");
            }
        }

        // Autopromote if necessary
        autoPromoter.maybeAutoPromote(testName, username, password, author, testDefinitionToUpdate, previousRevision,
                autopromoteTarget, requestParameterMap, job, trunkStore, qaRevision, prodRevision, existingTestDefinition);

        job.logComplete();
        job.addUrl("/proctor/definition/" + EncodingUtil.urlEncodeUtf8(testName) + "?branch=" + theEnvironment.getName(), "View Result");
        return true;
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
    private Optional<String> getMaxUsedAllocationIdForTest(final String testName) {
        // Use trunk store
        final ProctorStore trunkStore = determineStoreFromEnvironment(WORKING);
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
    private static void handleAllocationIdsForNewTest(final TestDefinition testDefinition) {
        for (int i = 0; i < testDefinition.getAllocations().size(); i++) {
            final Allocation allocation = testDefinition.getAllocations().get(i);
            allocation.setId(AllocationIdUtil.generateAllocationId(i, 1));
        }
    }


    private static void validateBasicInformation(
            final TestDefinition definition,
            final BackgroundJob<Void> backgroundJob
    ) throws IllegalArgumentException {
        if (StringUtils.isBlank(definition.getDescription())) {
            throw new IllegalArgumentException("Description is required.");
        }
        if (StringUtils.isBlank(definition.getSalt())) {
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

    private static void validateAllocationsAndBuckets(
            final TestDefinition definition,
            final BackgroundJob<Void> backgroundJob
    ) throws IllegalArgumentException {
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
                if (integerDoubleEntry.getKey() > 0 && totalBucketAllocation > 0 && !equalsWithinTolerance(totalBucketAllocation, totalControlBucketAllocation)) {
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

    private static String formatDefaultUpdateComment(final String testName, final String comment) {
        if (Strings.isNullOrEmpty(comment)) {
            return String.format("Updating A/B test %s", testName);
        }
        return comment;
    }

    private static String formatDefaultCreateComment(final String testName, final String comment) {
        if (Strings.isNullOrEmpty(comment)) {
            return String.format("Creating A/B test %s", testName);
        }
        return comment;
    }

    private static String createJobTitle(
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
        final StringBuilder messageBuilder = new StringBuilder(40);

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

    public BackgroundJob<Void> doPromote(
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
        final BackgroundJob<Void> backgroundJob = jobFactory.createBackgroundJob(
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

    private final static Multimap<Environment, Environment> PROMOTE_ACTIONS = ImmutableMultimap.<Environment, Environment>builder()
            .put(WORKING, QA)
            .put(WORKING, PRODUCTION)
            .put(QA, PRODUCTION)
            .build();

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
            final BackgroundJob<Void> job,
            final boolean isAutopromote
    ) throws Exception {
        final Map<String, String> metadata = Collections.emptyMap();
        validateUsernamePassword(username, password);

        // TODO (parker) 9/5/12 - Verify that promoting to the destination branch won't cause issues
        final TestDefinition testDefinition = TestDefinitionUtil.getTestDefinitionTryCached(
                determineStoreFromEnvironment(source),
                source,
                testName,
                srcRevision);
        //            if (d == null) {
        //                return "could not find " + testName + " on " + source + " with revision " + srcRevision;
        //            }
        job.logWithTiming("Validating Matrix.", "matrixCheck");
        final MatrixChecker.CheckMatrixResult result = matrixChecker.checkMatrix(destination, testName, testDefinition);
        if (!result.isValid()) {
            throw new IllegalArgumentException(String.format("Test Promotion not compatible, errors: %s", String.join("\n", result.getErrors())));
        }
        job.logWithTiming("Success: Validating Matrix.", "matrixCheck");

        // PreDefinitionPromoteChanges
        final DefinitionChangeLogger logger = new BackgroundJobLogger(job);
        if (!preDefinitionPromoteChanges.isEmpty()) {
            job.logWithTiming("Executing pre promote extension tasks.", "prePromoteExtension");
            for (final PreDefinitionPromoteChange preDefinitionPromoteChange : preDefinitionPromoteChanges) {
                preDefinitionPromoteChange.prePromote(testDefinition, requestParameterMap, source, destination,
                        isAutopromote, logger);
            }
            job.log("Finished pre promote extension tasks.");
        }

        // Promote Change
        job.logWithTiming("Promoting experiment", "promote");
        if (!PROMOTE_ACTIONS.containsEntry(source, destination)) {
            throw new IllegalArgumentException("Invalid combination of source and destination: source=" + source + " dest=" + destination);
        }
        try {
            job.log(String.format("(scm) promote %s %1.7s (%s to %s)", testName, srcRevision, source.getName(), destination.getName()));
            promoter.promote(testName, source, srcRevision, destination, destRevision, username, password, author, metadata);
            job.log(String.format("(scm) Successfully promoted %s %1.7s (%s to %s)", testName, srcRevision, source.getName(), destination.getName()));
        } catch (final Exception t) {
            Throwables.propagateIfInstanceOf(t, ProctorPromoter.TestPromotionException.class);
            Throwables.propagateIfInstanceOf(t, StoreException.TestUpdateException.class);
            throw new RuntimeException(t);
        }

        // PostDefinitionPromoteChanges
        if (!postDefinitionPromoteChanges.isEmpty()) {
            job.logWithTiming("Executing post promote extension tasks.", "postPromoteExtension");
            for (final PostDefinitionPromoteChange postDefinitionPromoteChange : postDefinitionPromoteChanges) {
                postDefinitionPromoteChange.postPromote(requestParameterMap, source, destination, isAutopromote,
                        logger);
            }
            job.log("Finished post promote extension tasks.");
        }

        job.log(String.format("Promoted %s from %s (%1.7s) to %s (%1.7s)", testName, source.getName(), srcRevision, destination.getName(), destRevision));
        job.addUrl("/proctor/definition/" + EncodingUtil.urlEncodeUtf8(testName) + "?branch=" + destination.getName(), "view " + testName + " on " + destination.getName());
        return true;

    }

}
