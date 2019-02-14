package com.indeed.proctor.webapp.util;

import com.indeed.proctor.common.EnvironmentVersion;
import com.indeed.proctor.common.ProctorPromoter;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.model.RevisionDefinition;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class TestDefinitionUtil {
    private static final Logger LOGGER = Logger.getLogger(TestDefinitionUtil.class);

    public static List<RevisionDefinition> makeRevisionDefinitionList(final ProctorStore store,
                                                                      final String testName,
                                                                      final String startRevision,
                                                                      final boolean useDefinitions) {
        final List<Revision> history = getTestHistory(store, testName, startRevision);
        final List<RevisionDefinition> revisionDefinitions = new ArrayList<>();
        if (useDefinitions) {
            for (Revision revision : history) {
                final String revisionName = revision.getRevision();
                final TestDefinition definition = getTestDefinition(store, testName, revisionName);
                revisionDefinitions.add(new RevisionDefinition(revision, definition));
            }
        } else {
            for (Revision revision : history) {
                revisionDefinitions.add(new RevisionDefinition(revision, null));
            }
        }
        return revisionDefinitions;
    }

    // @Nullable
    public static TestDefinition getTestDefinition(final ProctorStore store, final String testName) {
        try {
            return store.getCurrentTestDefinition(testName);
        } catch (StoreException e) {
            LOGGER.info("Failed to get current test definition for: " + testName, e);
            return null;
        }
    }

    // @Nullable
    public static TestDefinition getTestDefinition(final ProctorStore store, final String testName, final String revision) {
        try {
            if ("-1".equals(revision)) {
                LOGGER.info("Ignore revision id -1");
                return null;
            }
            return store.getTestDefinition(testName, revision);
        } catch (StoreException e) {
            LOGGER.info("Failed to get current test definition for: " + testName, e);
            return null;
        }
    }

    public static TestDefinition getTestDefinition(final ProctorStore store, final ProctorPromoter promoter, final Environment environment, final String testName, final String revision) {
        final EnvironmentVersion version = promoter.getEnvironmentVersion(testName);
        final String environmentVersion = version.getRevision(environment);
        if (revision.isEmpty() ||
                (!"-1".equals(environmentVersion) && revision.equals(environmentVersion))) {
            // if revision is environment latest version, fetching current environment version is more cache-friendly
            return getTestDefinition(store, testName);
        } else {
            return getTestDefinition(store, testName, revision);
        }
    }

    public static List<Revision> getTestHistory(final ProctorStore store, final String testName, final int limit) {
        return getTestHistory(store, testName, null, limit);
    }

    private static List<Revision> getTestHistory(final ProctorStore store, final String testName, final String startRevision) {
        return getTestHistory(store, testName, startRevision, Integer.MAX_VALUE);
    }

    // @Nonnull
    private static List<Revision> getTestHistory(final ProctorStore store,
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
            if (history.size() == 0) {
                LOGGER.info("No version history for [" + testName + "]");
            }
            return history;
        } catch (StoreException e) {
            LOGGER.info("Failed to get current test history for: " + testName, e);
            return null;
        }
    }
}
