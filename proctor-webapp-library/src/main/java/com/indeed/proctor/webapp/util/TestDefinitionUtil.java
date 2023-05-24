package com.indeed.proctor.webapp.util;

import com.indeed.proctor.common.ProctorPromoter;
import com.indeed.proctor.common.SingleEnvironmentVersion;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.store.GitProctorUtils;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.model.RevisionDefinition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.CheckForNull;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.indeed.proctor.common.EnvironmentVersion.UNKNOWN_REVISION;
import static com.indeed.proctor.common.EnvironmentVersion.UNKNOWN_VERSION;
import static com.indeed.proctor.webapp.db.Environment.WORKING;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

public class TestDefinitionUtil {
    private static final Logger LOGGER = LogManager.getLogger(TestDefinitionUtil.class);

    public static List<RevisionDefinition> makeRevisionDefinitionList(
            final ProctorStore store,
            final String testName,
            final String startRevision,
            final boolean useDefinitions) {
        final List<Revision> history = getTestHistory(store, testName, startRevision);
        final List<RevisionDefinition> revisionDefinitions = new ArrayList<>();
        if (useDefinitions) {
            for (final Revision revision : history) {
                final String revisionName = revision.getRevision();
                final TestDefinition definition = getTestDefinition(store, testName, revisionName);
                revisionDefinitions.add(new RevisionDefinition(revision, definition));
            }
        } else {
            for (final Revision revision : history) {
                revisionDefinitions.add(new RevisionDefinition(revision, null));
            }
        }
        return revisionDefinitions;
    }

    /** Fast retrieval of the current Definition from the testMatrix */
    // @Nullable
    @CheckForNull
    public static TestDefinition getTestDefinition(
            final ProctorStore store, final String testName) {
        try {
            return store.getCurrentTestDefinition(testName);
        } catch (final StoreException e) {
            // don't care about exception cause stacktrace for INFO level message
            LOGGER.info(
                    "Failed to get current test definition for: "
                            + testName
                            + ": "
                            + e.getMessage());
            return null;
        }
    }

    /** Slow? lookup of historic test definition */
    // @Nullable
    @CheckForNull
    public static TestDefinition getTestDefinition(
            final ProctorStore store, final String testName, final String revision) {
        try {
            if ("-1".equals(revision)) {
                LOGGER.info("Ignore revision id -1");
                return null;
            }
            return store.getTestDefinition(testName, revision);
        } catch (final StoreException e) {
            // don't care about exception cause stacktrace for INFO level message
            LOGGER.info(
                    "Failed to get current test definition for: "
                            + testName
                            + ": "
                            + e.getMessage());
            return null;
        }
    }

    /**
     * retrieves the TestDefinition from the ProctorStore using fast cache access for the latest
     * version if possible
     *
     * @deprecated use other getTestDefinitionTryCached
     */
    @Deprecated
    public static TestDefinition getTestDefinition(
            final ProctorStore store,
            final ProctorPromoter promoter, // obsolete after refactor
            final Environment environment,
            final String testName,
            final String revision) {
        return getTestDefinitionTryCached(store, environment, testName, revision);
    }

    /**
     * retrieves the TestDefinition from the ProctorStore using fast cache access for the latest
     * version if possible
     */
    @CheckForNull
    public static TestDefinition getTestDefinitionTryCached(
            final ProctorStore store,
            final Environment environment,
            final String testName,
            final String revision) {
        return getTestDefinitionTryCached(
                store,
                getResolvedLastVersion(store, testName, environment).getVersion(),
                testName,
                revision);
    }

    /**
     * retrieves the TestDefinition from the ProctorStore using fast cache access for the latest
     * version if possible
     */
    @CheckForNull
    private static TestDefinition getTestDefinitionTryCached(
            final ProctorStore store,
            final String latestRevision,
            final String testName,
            final String revision) {
        if (revision.isEmpty()
                || (!"-1".equals(latestRevision) && revision.equals(latestRevision))) {
            // if revision is environment latest version, fetching current environment version is
            // more cache-friendly
            return getTestDefinition(store, testName);
        } else {
            return getTestDefinition(store, testName, revision);
        }
    }

    /**
     * @param testName
     * @param environment
     * @return null on errors, "-1" when no history or resolution failed
     */
    @CheckForNull
    public static SingleEnvironmentVersion getResolvedLastVersion(
            final ProctorStore store, final String testName, final Environment environment) {
        final List<Revision> history;
        try {
            history = getMostRecentHistory(store, testName);
        } catch (final StoreException e) {
            LOGGER.error("Unable to retrieve latest version for trunk or qa or production", e);
            return null;
        }

        final Revision revision = history.isEmpty() ? null : history.get(0);

        final String version;
        if (WORKING.equals(environment)) {
            version = GitProctorUtils.resolveSvnMigratedRevision(revision, WORKING.getName());
        } else {
            version = getEffectiveRevisionFromStore(store, testName, revision);
        }

        return new SingleEnvironmentVersion(revision, defaultIfNull(version, UNKNOWN_REVISION));
    }

    // @Nonnull
    private static List<Revision> getMostRecentHistory(
            final ProctorStore store, final String testName) throws StoreException {
        final List<Revision> history = store.getHistory(testName, 0, 1);
        if (history.size() == 0) {
            LOGGER.info("No version history for [" + testName + "]");
        }
        return history;
    }

    private static String getEffectiveRevisionFromStore(
            final ProctorStore proctorStore, final String testName, final Revision revision) {
        try {
            return identifyEffectiveRevision(
                    proctorStore.getCurrentTestDefinition(testName), revision);
        } catch (final StoreException e) {
            LOGGER.error(
                    "Unable to retrieve test definition for "
                            + testName
                            + " at "
                            + revision
                            + " in "
                            + proctorStore.getName(),
                    e);
            return UNKNOWN_VERSION;
        }
    }

    private static final Pattern CHARM_MERGE_REVISION =
            Pattern.compile("^merged r([\\d]+):", Pattern.MULTILINE);

    private static String identifyEffectiveRevision(
            final TestDefinition branchDefinition, final Revision branchRevision) {
        if (branchDefinition == null) {
            return UNKNOWN_VERSION;
        }
        if (branchRevision == null) {
            return branchDefinition.getVersion();
        }
        final Matcher m = CHARM_MERGE_REVISION.matcher(branchRevision.getMessage());
        if (m.find()) {
            final String trunkRevision = m.group(1);
            return trunkRevision;
        }
        return branchDefinition.getVersion();
    }

    public static List<Revision> getTestHistory(
            final ProctorStore store, final String testName, final int limit) {
        return getTestHistory(store, testName, null, limit);
    }

    private static List<Revision> getTestHistory(
            final ProctorStore store, final String testName, final String startRevision) {
        return getTestHistory(store, testName, startRevision, Integer.MAX_VALUE);
    }

    // @Nonnull
    private static List<Revision> getTestHistory(
            final ProctorStore store,
            final String testName,
            final String startRevision,
            final int limit) {
        try {
            final List<Revision> history;
            if (startRevision == null) {
                history = store.getHistory(testName, 0, limit);
            } else {
                history = store.getHistory(testName, startRevision, 0, limit);
            }
            if (history.isEmpty()) {
                LOGGER.info("No version history for [" + testName + "]");
            }
            return history;
        } catch (final StoreException e) {
            // don't care about exception cause stacktrace for INFO level message
            LOGGER.info(
                    "Failed to get current test history for: " + testName + ": " + e.getMessage());
            return null;
        }
    }
}
