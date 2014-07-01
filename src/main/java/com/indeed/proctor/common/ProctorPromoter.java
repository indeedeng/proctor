package com.indeed.proctor.common;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.util.core.DataLoadingTimerTask;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author parker
 */
public class ProctorPromoter extends DataLoadingTimerTask {
    private static final Logger LOGGER = Logger.getLogger(ProctorPromoter.class);
    private static final String UNKNOWN_VERSION = EnvironmentVersion.UNKNOWN_VERSION;

    final ProctorStore trunk;
    final ProctorStore qa;
    final ProctorStore production;
    private volatile ConcurrentMap<String, EnvironmentVersion>  environmentVersions;

    public ProctorPromoter(final ProctorStore trunk,
                           final ProctorStore qa,
                           final ProctorStore production) {
        super(ProctorPromoter.class.getSimpleName());
        this.trunk = trunk;
        this.qa = qa;
        this.production = production;
    }

    public void promoteTrunkToQa(final String testName, String trunkRevision, String qaRevision,
                                 String username, String password, Map<String, String> metadata) throws StoreException, TestPromotionException {
        promote(testName, Environment.WORKING, trunkRevision, Environment.QA, qaRevision, username, password, metadata);
    }

    public void promoteQaToProduction(final String testName, String qaRevision, String prodRevision,
                                 String username, String password, Map<String, String> metadata) throws StoreException, TestPromotionException {
        promote(testName, Environment.QA, qaRevision, Environment.PRODUCTION, prodRevision, username, password, metadata);
    }

    public void promoteTrunkToProduction(final String testName, String trunkRevision, String prodRevision,
                                 String username, String password, Map<String, String> metadata) throws StoreException, TestPromotionException {
        promote(testName, Environment.WORKING, trunkRevision, Environment.PRODUCTION, prodRevision, username, password, metadata);
    }

    public void refreshWorkingVersion(final String testName) throws StoreException {
        final ConcurrentMap<String, EnvironmentVersion> versions = this.environmentVersions;
        if(versions != null) {
            final Environment branch = Environment.WORKING;
            final ProctorStore store = getStoreFromBranch(branch);
            // After a promotion update the current version
            final EnvironmentVersion current = getEnvironmentVersion(testName);
            final List<Revision> history = getMostRecentHistory(store, testName);
            final Revision trunkVersion = history.get(0);
            final EnvironmentVersion updated;
            if(current == null) {
                updated = new EnvironmentVersion(testName, trunkVersion, null, UNKNOWN_VERSION, null, UNKNOWN_VERSION);
            } else {
                updated = current.update(branch, trunkVersion, trunkVersion.getRevision().toString());
            }
            versions.replace(testName, updated);
        }
    }

    private void updateTestVersion(final String testName,
                                   final Environment branch,
                                   final String effectiveVersion) throws StoreException {
        final ConcurrentMap<String, EnvironmentVersion> versions = this.environmentVersions;
        if(versions != null) {
            final ProctorStore store = getStoreFromBranch(branch);
            // After a promotion update the current version
            final EnvironmentVersion current = getEnvironmentVersion(testName);
            final List<Revision> history = getMostRecentHistory(store, testName);
            final Revision destVersion = history.get(0);
            final EnvironmentVersion updated = current.update(branch, destVersion, effectiveVersion);
            versions.replace(testName, updated);
        }
    }


    @SuppressWarnings({"MethodWithTooManyParameters"})
    private void promote(final String testName, final Environment srcBranch, final String srcRevision, final Environment destBranch, String destRevision,
                         String username, String password, Map<String, String> metadata) throws TestPromotionException, StoreException {
        LOGGER.info(String.format("%s : Promoting %s from %s r%s to %s r%s", username, testName, srcBranch,
                srcRevision, destBranch, destRevision));
        final ProctorStore src = getStoreFromBranch(srcBranch);
        final ProctorStore dest = getStoreFromBranch(destBranch);
        final boolean isSrcTrunk = Environment.WORKING == srcBranch;

        final TestDefinition d = getTestDefinition(src, testName, srcRevision);

        final EnvironmentVersion version = getEnvironmentVersion(testName);
        final String knownDestRevision = version != null ? version.getRevision(destBranch) : UNKNOWN_VERSION;

        // destRevision > 0 indicates destination revision expected
        // TODO (parker) 7/1/14 - alloq ProctorStore to implement valid / unknown revision parsing
        if(knownDestRevision.length() == 0 && destRevision.length() > 0) {
            throw new TestPromotionException("Positive revision r" + destRevision + " given for destination ( " + destBranch + " ) but '" + testName + "' does not exist.");
        } else if (!knownDestRevision.equals("-1") && knownDestRevision.length() > 0 && destRevision.length() == 0) {
            throw new TestPromotionException("Non-Positive revision r" + destRevision + " given for destination ( " + destBranch + " ) but '" + testName + "' exists.");
        }

        // Update the Test Definition Version to the svn-revision of the source
        if(isSrcTrunk) {
            // If source is trunk, we want to set the version of the test-matrix to be the revision on trunk
            d.setVersion(srcRevision);
        }

        final List<Revision> srcHistory = getHistoryFromRevision(src, testName, srcRevision);
        if(srcHistory.isEmpty()) {
            throw new TestPromotionException("Could not find history for " + testName + " at revision " + srcRevision);
        }
        final Revision srcVersion = srcHistory.get(0);

        if(!knownDestRevision.equals("-1") && knownDestRevision.length() > 0) {
            // This test exists in the destination branch. Get its most recent test-history in the event that EnvironmentVersion is stale.
            List<Revision> history = getMostRecentHistory(dest, testName);
            if(history.isEmpty()) {
                throw new TestPromotionException("No history found for '" + testName + "' in destination ( " + destBranch + ").");
            }
            final Revision destVersion = history.get(0);
            if(!destVersion.getRevision().equals(destRevision)) {
                throw new TestPromotionException("Test '" + testName + "' updated since " + destRevision + ". Currently at " + history.get(0).getRevision());
            }
            final String commitMessage = formatCommitMessage(testName , srcBranch, srcRevision, destBranch, srcVersion.getMessage());
            LOGGER.info(String.format("%s : Committing %s from %s r%s to %s r%s", username, testName, srcBranch,
                    srcRevision, destBranch, destRevision));
            dest.updateTestDefinition(username, password, destRevision, testName, d, metadata, commitMessage);
        } else {
            final String commitMessage = formatCommitMessage(testName , srcBranch, srcRevision, destBranch, srcVersion.getMessage());
            dest.addTestDefinition(username, password, testName, d, metadata, commitMessage);
        }

        updateTestVersion(testName, destBranch, d.getVersion());
    }

    private ProctorStore getStoreFromBranch(Environment srcBranch) {
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
        if(!CharMatcher.WHITESPACE.matchesAllOf(Strings.nullToEmpty(comment))) {
            // PROW-59: Replace smart-commit commit messages when promoting tests
            final String cleanedComment = comment.replace("+review", "_review");
            sb.append("\n\n").append(cleanedComment);
        }
        return sb.toString();
    }

    // cached
    public EnvironmentVersion getEnvironmentVersion(final String testName) {
        final ConcurrentMap<String, EnvironmentVersion> versions = environmentVersions;
        if(versions != null) {
            return versions.get(testName);
        } else {
            return null;
        }
    }

    @Override
    public boolean load() {
        final String trunkMatrixVersion;
        final String qaMatrixVersion;
        final String prodMatrixVersion;
        try {
            trunkMatrixVersion = trunk.getLatestVersion();
            qaMatrixVersion = qa.getLatestVersion();
            prodMatrixVersion = production.getLatestVersion();
        } catch (StoreException e) {
            LOGGER.error("Unable to retrieve latest version for trunk or qa or production", e);
            return false;
        }


        // Compute version as "trunk=@Version,qa=@Version,production=@Version"
        final String version = String.format("trunk=%s,qa=%s,production=%s",
                                             trunkMatrixVersion,
                                             qaMatrixVersion,
                                             prodMatrixVersion);

        if(version.equals(this.getDataVersion())) {
            LOGGER.info("Skipping branch definition reload, versions are equal: " + version);
            return false;
        }

        final TestMatrixVersion trunkMatrix;
        final TestMatrixVersion qaMatrix;
        final TestMatrixVersion prodMatrix;
        try {
            trunkMatrix = trunk.getTestMatrix(trunkMatrixVersion);
            qaMatrix = qa.getTestMatrix(qaMatrixVersion);
            prodMatrix = production.getTestMatrix(prodMatrixVersion);
        } catch (StoreException e) {
            LOGGER.error("Unable to retrieve test matrix for trunk or qa or production", e);
            return false;
        }
        if(trunkMatrix == null || qaMatrix == null || prodMatrix == null) {
            LOGGER.error("NULL Test Matrix returned for trunk or qa or production");
            return false;
        }

        final ImmutableMap.Builder<String, Revision> trunkVersionBuilder = ImmutableMap.builder();
        final ImmutableMap.Builder<String, Revision> qaVersionBuilder = ImmutableMap.builder();
        final ImmutableMap.Builder<String, Revision> productionVersionBuilder = ImmutableMap.builder();

        populateVersionMap(trunk, trunkVersionBuilder, trunkMatrix);
        populateVersionMap(qa, qaVersionBuilder, qaMatrix);
        populateVersionMap(production, productionVersionBuilder, prodMatrix);

        final Map<String, Revision> trunkVersions = trunkVersionBuilder.build();
        final Map<String, Revision> qaVersions = qaVersionBuilder.build();
        final Map<String, Revision> productionVersions= productionVersionBuilder.build();

        final ImmutableSet<String> tests = ImmutableSet.<String>builder().addAll(trunkVersions.keySet()).addAll(qaVersions.keySet()).addAll(productionVersions.keySet()).build();

        final ConcurrentMap<String, EnvironmentVersion> versions = Maps.newConcurrentMap();
        // Join all of the Maps together
        for(String testName : tests) {
            final Revision trunkVersion = trunkVersions.get(testName);

            final Revision qaRevision = qaVersions.get(testName);
            final TestDefinition qaDefinition = getTestDefinition(qaMatrix, testName);
            final Revision productionRevision = productionVersions.get(testName);
            final TestDefinition productionDefinition = getTestDefinition(prodMatrix, testName);

            versions.put(testName, new EnvironmentVersion(testName, trunkVersion,
                                                     qaRevision, identifyEffectiveRevision(qaDefinition, qaRevision),
                                                     productionRevision, identifyEffectiveRevision(productionDefinition, productionRevision)));
        }

        this.environmentVersions = versions;
        this.setDataVersion(version);
        return true;
    }

    private final Pattern CHARM_MERGE_REVISION = Pattern.compile("^merged r([\\d]+):", Pattern.MULTILINE);
    private String identifyEffectiveRevision(final TestDefinition branchDefinition,
                                             final Revision branchRevision) {
        if(branchDefinition == null) {
            return UNKNOWN_VERSION;
        }
        if(branchRevision == null) {
            return branchDefinition.getVersion();
        }
        final Matcher m = CHARM_MERGE_REVISION.matcher(branchRevision.getMessage());
        if(m.find()) {
            final String trunkRevision = m.group(1);
            return trunkRevision;
        }
        return branchDefinition.getVersion();
    }

    /**
     *  Populates the collector with the most recent Revision for all the tests in the matrix.
     *  Additional calls to the ProctorStore are necessary because the TestDefinition does not have a
     *  SVN revision value associated with it.
     * @param store
     * @param collector
     * @param matrix
     */
    private static void populateVersionMap(final ProctorStore store,
                                           final ImmutableMap.Builder<String, Revision> collector,
                                           final TestMatrixVersion matrix) {
        Preconditions.checkNotNull(store, "Store cannot be null");
        Preconditions.checkNotNull(collector, "Collector cannot be null");
        if(matrix == null) {
            LOGGER.info("Matrix is null, cannot populate version map");
            return;
        }

        for(Map.Entry<String, TestDefinition> test : matrix.getTestMatrixDefinition().getTests().entrySet()) {
            final String testName = test.getKey();
            try {
                final List<Revision> history = getMostRecentHistory(store, testName);
                if(history.size() > 0) {
                    final Revision version = history.get(0);
                    collector.put(testName, version);
                }
            } catch (StoreException exp) {
                // store.getHistory throws RuntimeException if test does not exist
                LOGGER.info("Failed to read history for : " + testName, exp);
            }
        }
    }

    // @Nonnull
    private static List<Revision> getMostRecentHistory(final ProctorStore store, final String testName) throws StoreException {
        final List<Revision> history = store.getHistory(testName, 0, 1);
        if(history.size() == 0) {
            LOGGER.info("No version history for [" + testName + "]");
        }
        return history;
    }

    private static List<Revision> getHistoryFromRevision(final ProctorStore src,
                                                         final String testName,
                                                         final String srcRevision) throws StoreException {
        return src.getHistory(testName, srcRevision, 0, 1);
    }


    private static TestDefinition getCurrentTestDefinition(final ProctorStore store, final String testName) throws StoreException {
        return store.getCurrentTestDefinition(testName);
    }

    // @Nullable
    private static TestDefinition getTestDefinition(final ProctorStore store, final String testName, String version) throws StoreException {
        return store.getTestDefinition(testName, version);
    }

    // @Nullable
    private static TestDefinition getTestDefinition(/* @Nullable */ final TestMatrixVersion matrix, final String testName) {
        if(matrix == null) {
            return null;
        }
        final TestMatrixDefinition def = matrix.getTestMatrixDefinition();
        return def.getTests().get(testName);
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