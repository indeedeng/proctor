package com.indeed.proctor.webapp.jobs;

import com.google.common.base.Strings;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.store.ChangeMetadata;
import com.indeed.proctor.store.GitNoAuthorizationException;
import com.indeed.proctor.store.GitNoDevelperAccessLevelException;
import com.indeed.proctor.store.GitNoMasterAccessLevelException;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.extensions.BackgroundJobLogger;
import com.indeed.proctor.webapp.extensions.DefinitionChangeLogger;
import com.indeed.proctor.webapp.extensions.PostDefinitionDeleteChange;
import com.indeed.proctor.webapp.extensions.PreDefinitionDeleteChange;
import com.indeed.proctor.webapp.util.EncodingUtil;
import com.indeed.proctor.webapp.util.TestDefinitionUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
public class DeleteJob extends AbstractJob {
    private static final Logger LOGGER = LogManager.getLogger(DeleteJob.class);
    private List<PreDefinitionDeleteChange> preDefinitionDeleteChanges = Collections.emptyList();
    private List<PostDefinitionDeleteChange> postDefinitionDeleteChanges = Collections.emptyList();

    private final BackgroundJobManager jobManager;
    private final BackgroundJobFactory jobFactory;
    private final CommentFormatter commentFormatter;
    private final MatrixChecker matrixChecker;

    @Autowired
    public DeleteJob(
            @Qualifier("trunk") final ProctorStore trunkStore,
            @Qualifier("qa") final ProctorStore qaStore,
            @Qualifier("production") final ProctorStore productionStore,
            final BackgroundJobManager jobManager,
            final BackgroundJobFactory jobFactory,
            final CommentFormatter commentFormatter,
            final MatrixChecker matrixChecker) {
        super(trunkStore, qaStore, productionStore);
        this.jobManager = jobManager;
        this.jobFactory = jobFactory;
        this.commentFormatter = commentFormatter;
        this.matrixChecker = matrixChecker;
    }

    @Autowired(required = false)
    public void setDefinitionDeleteChanges(
            final List<PreDefinitionDeleteChange> preDefinitionDeleteChanges,
            final List<PostDefinitionDeleteChange> postDefinitionDeleteChanges) {
        this.preDefinitionDeleteChanges = preDefinitionDeleteChanges;
        this.postDefinitionDeleteChanges = postDefinitionDeleteChanges;
    }

    public BackgroundJob<Void> doDelete(
            final String testName,
            final String username,
            final String password,
            final String author,
            final Environment source,
            final String srcRevision,
            final String comment,
            final Map<String, String[]> requestParameterMap) {
        LOGGER.info(
                String.format("Deleting test %s branch: %s user: %s ", testName, source, username));
        final BackgroundJob<Void> backgroundJob =
                jobFactory.createBackgroundJob(
                        String.format(
                                "(username:%s author:%s) deleting %s branch: %s ",
                                username, author, testName, source),
                        author,
                        BackgroundJob.JobType.TEST_DELETION,
                        job -> {
                            try {
                                doDeleteInternal(
                                        testName,
                                        username,
                                        password,
                                        author,
                                        source,
                                        srcRevision,
                                        comment,
                                        requestParameterMap,
                                        job);
                            } catch (final GitNoMasterAccessLevelException
                                    | GitNoAuthorizationException
                                    | GitNoDevelperAccessLevelException
                                    | IllegalArgumentException exp) {
                                job.logFailedJob(exp);
                                LOGGER.info("Deletion Failed: " + job.getTitle(), exp);
                            } catch (final Exception e) {
                                job.logFailedJob(e);
                                LOGGER.error("Deletion Failed: " + job.getTitle(), e);
                            }
                            return null;
                        });
        jobManager.submit(backgroundJob);
        return backgroundJob;
    }

    private Boolean doDeleteInternal(
            final String testName,
            final String username,
            final String password,
            final String author,
            final Environment source,
            final String srcRevision,
            final String comment,
            final Map<String, String[]> requestParameterMap,
            final BackgroundJob<?> job)
            throws Exception {
        final ProctorStore store = determineStoreFromEnvironment(source);
        final TestDefinition definition = TestDefinitionUtil.getTestDefinition(store, testName);
        if (definition == null) {
            job.log("Unknown test definition : " + testName);
            return false;
        }
        validateUsernamePassword(username, password);

        final Revision prevVersion;
        job.logWithTiming("(scm) getting history for '" + testName + "'", "getHistory");
        final List<Revision> history = TestDefinitionUtil.getTestHistory(store, testName, 1);
        if (!history.isEmpty()) {
            prevVersion = history.get(0);
            if (!prevVersion.getRevision().equals(srcRevision)) {
                throw new IllegalArgumentException(
                        "Test has been updated since "
                                + srcRevision
                                + " currently at "
                                + prevVersion.getRevision());
            }
        } else {
            throw new IllegalArgumentException("Could not get any history for " + testName);
        }
        job.log("(scm) Success: getting history for '" + testName + "'");

        job.logWithTiming("checking clients usage", "checkMatrix");

        if (source.equals(Environment.WORKING) || source.equals(Environment.QA)) {
            final MatrixChecker.CheckMatrixResult checkMatrixResultInQa =
                    matrixChecker.checkMatrix(Environment.QA, testName, null);
            if (!checkMatrixResultInQa.isValid) {
                throw new IllegalArgumentException(
                        "There are still clients in QA using "
                                + testName
                                + " "
                                + checkMatrixResultInQa.getErrors().get(0));
            }
        }
        final MatrixChecker.CheckMatrixResult checkMatrixResult =
                matrixChecker.checkMatrix(Environment.PRODUCTION, testName, null);
        if (!checkMatrixResult.isValid()) {
            throw new IllegalArgumentException(
                    "There are still clients in prod using "
                            + testName
                            + " "
                            + checkMatrixResult.getErrors().get(0));
        }

        job.log("Success: checking clients usage");

        final DefinitionChangeLogger logger = new BackgroundJobLogger(job);
        // PreDefinitionDeleteChanges
        if (!preDefinitionDeleteChanges.isEmpty()) {
            job.logWithTiming("Executing pre delete extension tasks.", "preDeleteExtension");
            for (final PreDefinitionDeleteChange preDefinitionDeleteChange :
                    preDefinitionDeleteChanges) {
                preDefinitionDeleteChange.preDelete(definition, requestParameterMap, logger);
            }
            job.log("Finished pre delete extension tasks.");
        }

        job.logWithTiming("Deleting", "Delete");
        job.log("(scm) delete " + testName);
        store.deleteTestDefinition(
                ChangeMetadata.builder()
                        .setUsername(username)
                        .setPassword(password)
                        .setAuthor(author)
                        .setComment(
                                commentFormatter.formatFullComment(
                                        formatDefaultDeleteComment(testName, comment),
                                        requestParameterMap))
                        .build(),
                srcRevision,
                testName,
                definition);
        job.log("(scm) Success: delete " + testName);

        // add urls to same test in other environments to job information for user
        boolean testExistsInOtherEnvironments = false;
        for (final Environment otherEnvironment : Environment.values()) {
            if (otherEnvironment != source) {
                final TestDefinition otherDefinition =
                        TestDefinitionUtil.getTestDefinition(
                                determineStoreFromEnvironment(otherEnvironment), testName);
                if (otherDefinition != null) {
                    testExistsInOtherEnvironments = true;
                    job.addUrl(
                            "/proctor/definition/"
                                    + EncodingUtil.urlEncodeUtf8(testName)
                                    + "?branch="
                                    + otherEnvironment.getName(),
                            "view " + testName + " on " + otherEnvironment.getName());
                }
            }
        }
        if (!testExistsInOtherEnvironments) {
            job.setEndMessage("This test no longer exists in any environment.");
        }

        // PostDefinitionDeleteChanges
        if (!postDefinitionDeleteChanges.isEmpty()) {
            job.logWithTiming("Executing post delete extension tasks.", "PostDeleteExtension");
            for (final PostDefinitionDeleteChange postDefinitionDeleteChange :
                    postDefinitionDeleteChanges) {
                postDefinitionDeleteChange.postDelete(requestParameterMap, logger);
            }
            job.log("Finished post delete extension tasks.");
        }
        job.logComplete();
        return true;
    }

    private String formatDefaultDeleteComment(final String testName, final String comment) {
        if (Strings.isNullOrEmpty(comment)) {
            return String.format("Deleting A/B test %s", testName);
        }
        return comment;
    }
}
