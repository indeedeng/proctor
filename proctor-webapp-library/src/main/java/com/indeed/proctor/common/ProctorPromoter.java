package com.indeed.proctor.common;

import com.google.common.annotations.VisibleForTesting;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.store.GitProctorUtils;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.util.TestDefinitionUtil;
import com.indeed.proctor.webapp.util.ThreadPoolExecutorVarExports;
import com.indeed.util.varexport.VarExporter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.annotation.CheckForNull;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.indeed.proctor.common.EnvironmentVersion.UNKNOWN_REVISION;

/**
 * Handle possible promote in all environment.
 *
 * @author parker
 */
public class ProctorPromoter {
    private static final Logger LOGGER = Logger.getLogger(ProctorPromoter.class);
    private static final String UNKNOWN_VERSION = EnvironmentVersion.UNKNOWN_VERSION;

    private final ProctorStore trunk;
    private final ProctorStore qa;
    private final ProctorStore production;
    private final ExecutorService executor;

    public ProctorPromoter(final ProctorStore trunk,
                           final ProctorStore qa,
                           final ProctorStore production,
                           final ExecutorService executor) {
        this.trunk = trunk;
        this.qa = qa;
        this.production = production;
        if (executor instanceof ThreadPoolExecutor) {
            final VarExporter exporter = VarExporter.forNamespace(getClass().getSimpleName());
            exporter.export(new ThreadPoolExecutorVarExports((ThreadPoolExecutor) executor), "ProctorPromoter-pool-");
        }
        this.executor = executor;
    }

    /**
     * @deprecated Just use promote
     */
    @Deprecated
    public void promoteTrunkToQa(final String testName, String trunkRevision, String qaRevision,
                                 String username, String password, String author, Map<String, String> metadata) throws StoreException, TestPromotionException {
        promote(testName, Environment.WORKING, trunkRevision, Environment.QA, qaRevision, username, password, author, metadata);
    }

    /**
     * @deprecated Just use promote
     */
    @Deprecated
    public void promoteQaToProduction(final String testName, String qaRevision, String prodRevision,
                                      String username, String password, String author, Map<String, String> metadata) throws StoreException, TestPromotionException {
        promote(testName, Environment.QA, qaRevision, Environment.PRODUCTION, prodRevision, username, password, author, metadata);
    }

    /**
     * @deprecated Just use promote
     */
    @Deprecated
    public void promoteTrunkToProduction(final String testName, String trunkRevision, String prodRevision,
                                         String username, String password, String author, Map<String, String> metadata) throws StoreException, TestPromotionException {
        promote(testName, Environment.WORKING, trunkRevision, Environment.PRODUCTION, prodRevision, username, password, author, metadata);
    }

    @SuppressWarnings({"MethodWithTooManyParameters"})
    @VisibleForTesting
    public void promote(final String testName, final Environment srcBranch, final String srcRevision, final Environment destBranch, final String destRevision,
                 final String username, final String password, final String author, final Map<String, String> metadata) throws TestPromotionException, StoreException {
        LOGGER.info(String.format("%s : Promoting %s from %s r%s to %s r%s", username, testName, srcBranch,
                srcRevision, destBranch, destRevision));
        final ProctorStore src = getStoreFromBranch(srcBranch);
        final ProctorStore dest = getStoreFromBranch(destBranch);
        final boolean isSrcTrunk = (Environment.WORKING == srcBranch);

        final TestDefinition d = new TestDefinition(getTestDefinition(src, testName, srcRevision));

        final String version = TestDefinitionUtil.getResolvedLastVersion(dest, testName, destBranch).getVersion();
        final String knownDestRevision = version != null ? version : UNKNOWN_VERSION;

        // destRevision > 0 indicates destination revision expected
        // TODO (parker) 7/1/14 - allow ProctorStore to implement valid / unknown revision parsing
        if (knownDestRevision.length() == 0 && destRevision.length() > 0) {
            throw new TestPromotionException("Positive revision r" + destRevision + " given for destination ( " + destBranch + " ) but '" + testName + "' does not exist.");
        }
        if (!knownDestRevision.equals(UNKNOWN_REVISION) && knownDestRevision.length() > 0 && destRevision.length() == 0) {
            throw new TestPromotionException("Non-Positive revision r" + destRevision + " given for destination ( " + destBranch + " ) but '" + testName + "' exists.");
        }

        final List<Revision> srcHistory = getHistoryFromRevision(src, testName, srcRevision);
        if (srcHistory.isEmpty()) {
            throw new TestPromotionException("Could not find history for " + testName + " at revision " + srcRevision);
        }
        final Revision srcVersion = srcHistory.get(0);

        // Update the Test Definition Version to the svn-revision of the source (if it is a migrated commit)
        final String effectiveRevision = GitProctorUtils.resolveSvnMigratedRevision(srcVersion, Environment.WORKING.getName());

        if (isSrcTrunk) {
            // If source is trunk, we want to set the version of the test-matrix to be the revision on trunk
            d.setVersion(effectiveRevision);
        }

        if (!knownDestRevision.equals(UNKNOWN_REVISION) && knownDestRevision.length() > 0) {
            // This test exists in the destination branch history (but might have been deleted).
            // Get its most recent test-history in the event that EnvironmentVersion is stale.
            final List<Revision> history = getMostRecentHistory(dest, testName);
            if (history.isEmpty()) {
                throw new TestPromotionException("No history found for '" + testName + "' in destination ( " + destBranch + " ).");
            }
            final Revision destVersion = history.get(0);
            if (!destVersion.getRevision().equals(destRevision)) {
                throw new IllegalArgumentException("Test '" + testName + "' updated since " + destRevision + ". Currently at " + history.get(0).getRevision());
            }
            if (dest.getCurrentTestDefinition(testName) == null) {
                // test exist in history but no current definition means it was deleted
                throw new IllegalArgumentException("Test '" + testName + "' has been deleted in destination, not allowed to promote again.");
            }

            final String commitMessage = formatCommitMessage(testName, srcBranch, effectiveRevision, destBranch, srcVersion.getMessage());
            LOGGER.info(String.format("%s : Committing %s from %s r%s to %s r%s", username, testName, srcBranch,
                    srcRevision, destBranch, destRevision));
            dest.updateTestDefinition(username, password, author, destRevision, testName, d, metadata, commitMessage);
        } else {
            final String commitMessage = formatCommitMessage(testName, srcBranch, effectiveRevision, destBranch, srcVersion.getMessage());
            dest.addTestDefinition(username, password, author, testName, d, metadata, commitMessage);
        }
    }

    protected ProctorStore getStoreFromBranch(final Environment srcBranch) {
        switch (srcBranch) {
            case WORKING:
                return trunk;
            case QA:
                return qa;
            case PRODUCTION:
                return production;
        }
        throw new IllegalArgumentException("No store for branch " + srcBranch);
    }

    private static String formatCommitMessage(final String testName, final Environment src, final String srcRevision, final Environment dest, final String comment) {
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format("Promoting %s (%s r%s) to %s", testName, src.getName(), srcRevision, dest.getName()));
        if (StringUtils.isNotBlank((comment))) {
            // PROW-59: Replace smart-commit commit messages when promoting tests
            final String cleanedComment = comment.replace("+review", "_review");
            sb.append("\n\n").append(cleanedComment);
        }
        return sb.toString();
    }


    /**
     * Retrieve latest revision and version in each environment from ProctorStore and then return them as EnvironmentVersion.
     * Prefer TestDefinitionUtil.getResolvedLastVersion when possible
     */
    @CheckForNull
    public EnvironmentVersion getEnvironmentVersion(final String testName) {

        final SingleEnvironmentVersion trunkEnvironmentVersion;
        final SingleEnvironmentVersion qaEnvironmentVersion;
        final SingleEnvironmentVersion productionEnvironmentVersion;
        // Fetch versions in parallel
        final Future<SingleEnvironmentVersion> trunkFuture = executor.submit(() ->
                TestDefinitionUtil.getResolvedLastVersion(trunk, testName, Environment.WORKING));
        final Future<SingleEnvironmentVersion> qaFuture = executor.submit(() ->
                TestDefinitionUtil.getResolvedLastVersion(qa, testName, Environment.QA));
        final Future<SingleEnvironmentVersion> productionFuture = executor.submit(() ->
                TestDefinitionUtil.getResolvedLastVersion(production, testName, Environment.PRODUCTION));
        try {
            trunkEnvironmentVersion = trunkFuture.get(30, TimeUnit.SECONDS);
            qaEnvironmentVersion = qaFuture.get(30, TimeUnit.SECONDS);
            productionEnvironmentVersion = productionFuture.get(30, TimeUnit.SECONDS);
        } catch (final InterruptedException | ExecutionException e) {
            LOGGER.error("Unable to retrieve latest version for trunk or qa or production", e);
            return null;
        } catch (final TimeoutException e) {
            LOGGER.error("Timed out when retrieving latest version for trunk or qa or production", e);
            trunkFuture.cancel(true);
            qaFuture.cancel(true);
            productionFuture.cancel(true);
            return null;
        }

        return new EnvironmentVersion(
                testName,
                trunkEnvironmentVersion.getRevision(),
                trunkEnvironmentVersion.getVersion(),
                qaEnvironmentVersion.getRevision(),
                qaEnvironmentVersion.getVersion(),
                productionEnvironmentVersion.getRevision(),
                productionEnvironmentVersion.getVersion());
    }


    // @Nonnull
    private static List<Revision> getMostRecentHistory(final ProctorStore store, final String testName) throws StoreException {
        final List<Revision> history = store.getHistory(testName, 0, 1);
        if (history.size() == 0) {
            LOGGER.info("No version history for [" + testName + "]");
        }
        return history;
    }

    private static List<Revision> getHistoryFromRevision(final ProctorStore src,
                                                         final String testName,
                                                         final String srcRevision) throws StoreException {
        return src.getHistory(testName, srcRevision, 0, 1);
    }

    // @Nullable
    private static TestDefinition getTestDefinition(final ProctorStore store, final String testName, String version) throws StoreException {
        return store.getTestDefinition(testName, version);
    }

    public static class TestPromotionException extends Exception {

        public TestPromotionException(final String message) {
            super(message);
        }

        public TestPromotionException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

}
