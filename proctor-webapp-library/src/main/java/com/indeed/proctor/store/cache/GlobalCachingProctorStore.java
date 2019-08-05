package com.indeed.proctor.store.cache;

import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.RevisionDetails;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.store.utils.HistoryUtil;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.extensions.GlobalCacheStore;
import org.apache.log4j.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A decorator class for ProctorStore
 * This class handle latest test definition and history shared across multiple instance.
 */
public class GlobalCachingProctorStore implements ProctorStore {
    private static final Logger LOGGER = Logger.getLogger(GlobalCachingProctorStore.class);

    private final ProctorStore delegate;
    private final GlobalCacheStore globalCacheStore;
    private final Environment environment;

    public GlobalCachingProctorStore(final ProctorStore delegate,
                                     final GlobalCacheStore globalCacheStore,
                                     final Environment environment
    ) {
        this.delegate = delegate;
        this.globalCacheStore = globalCacheStore;
        this.environment = environment;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public TestMatrixVersion getCurrentTestMatrix() throws StoreException {
        return delegate.getCurrentTestMatrix();
    }

    @Override
    public TestDefinition getCurrentTestDefinition(final String test) throws StoreException {
        return globalCacheStore.getCachedTestDefinition(environment, test)
                .orElse(delegate.getCurrentTestDefinition(test));
    }

    @Override
    public void verifySetup() throws StoreException {
        delegate.verifySetup();
    }

    @Override
    public boolean cleanUserWorkspace(final String username) {
        return delegate.cleanUserWorkspace(username);
    }

    @Override
    public void updateTestDefinition(final String username, final String password, final String author, final String previousVersion, final String testName, final TestDefinition testDefinition, final Map<String, String> metadata, final String comment) throws StoreException.TestUpdateException {
        delegate.updateTestDefinition(username, password, author, previousVersion, testName, testDefinition, metadata, comment);
        updateGlobalCache(testName, testDefinition);
    }

    @Override
    public void deleteTestDefinition(final String username, final String password, final String author, final String previousVersion, final String testName, final TestDefinition testDefinition, final String comment) throws StoreException.TestUpdateException {
        delegate.deleteTestDefinition(username, password, author, previousVersion, testName, testDefinition, comment);
        updateGlobalCache(testName, null);
    }

    @Override
    public void addTestDefinition(final String username, final String password, final String author, final String testName, final TestDefinition testDefinition, final Map<String, String> metadata, final String comment) throws StoreException.TestUpdateException {
        delegate.addTestDefinition(username, password, author, testName, testDefinition, metadata, comment);
        updateGlobalCache(testName, testDefinition);
    }

    @Nonnull
    @Override
    public String getLatestVersion() throws StoreException {
        return delegate.getLatestVersion();
    }

    @Override
    public TestMatrixVersion getTestMatrix(final String fetchRevision) throws StoreException {
        return delegate.getTestMatrix(fetchRevision);
    }

    @Override
    public TestDefinition getTestDefinition(final String test, final String fetchRevision) throws StoreException {
        return globalCacheStore.getCachedTestDefinition(environment, test, fetchRevision)
                .orElse(delegate.getTestDefinition(test, fetchRevision));
    }

    @Nonnull
    @Override
    public List<Revision> getMatrixHistory(final int start, final int limit) throws StoreException {
        return delegate.getMatrixHistory(start, limit);
    }

    @Nonnull
    @Override
    public List<Revision> getHistory(final String test, final int start, final int limit) throws StoreException {
        return globalCacheStore.getCachedHistory(environment, test).map(
                history -> HistoryUtil.selectHistorySet(history, start, limit)
        ).orElse(delegate.getHistory(test, start, limit));
    }

    @Nonnull
    @Override
    public List<Revision> getHistory(final String test, final String revision, final int start, final int limit) throws StoreException {
        return globalCacheStore.getCachedHistory(environment, test).map(
                history -> HistoryUtil.selectRevisionHistorySetFrom(history, revision, start, limit)
        ).orElse(delegate.getHistory(test, revision, start, limit));
    }

    @CheckForNull
    @Override
    public RevisionDetails getRevisionDetails(final String revisionId) throws StoreException {
        return delegate.getRevisionDetails(revisionId);
    }

    @Nonnull
    @Override
    public Map<String, List<Revision>> getAllHistories() throws StoreException {
        return delegate.getAllHistories();
    }

    @Override
    public void refresh() throws StoreException {
        delegate.refresh();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    private void updateGlobalCache(final String testName,
                                   @Nullable final TestDefinition testDefinition
    ) {
        LOGGER.info("Start updating the global cache for " + testName);
        try {
            final List<Revision> revisions = delegate.getHistory(testName, 0, Integer.MAX_VALUE);
            globalCacheStore.updateCache(environment, testName, testDefinition, revisions);
        } catch (final StoreException e) {
            final String errorMessage = "Failed to update the global cache for " + testName
                    + " since history can't be read from proctor store";
            LOGGER.error(errorMessage, e);
            throw new GlobalCacheUpdateException.HistoryReadException(errorMessage, e);
        }
        LOGGER.info("Finish updating the global cache for " + testName);
    }
}

