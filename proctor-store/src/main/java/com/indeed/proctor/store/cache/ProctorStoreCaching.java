package com.indeed.proctor.store.cache;

import com.google.common.base.Throwables;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Caching test histories
 */
public class ProctorStoreCaching implements ProctorStore {

    private static final Logger LOGGER = Logger.getLogger(ProctorStoreCaching.class);

    private final ProctorStore delegate;
    private final CacheHolder cacheHolder;

    public ProctorStoreCaching(final ProctorStore delegate) {
        this.delegate = delegate;
        cacheHolder = new CacheHolder(delegate);
        try {
            /**
             * We assume local repository is initialized, so we can call cacheHolder#start in constructor
             */
            cacheHolder.start();
        } catch (final StoreException e) {
            LOGGER.error("Failed to initialize ProctorStoreCache", e);
            Throwables.propagate(e);
        }
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public TestMatrixVersion getCurrentTestMatrix() throws StoreException {
        return cacheHolder.getCachedCurrentTestMatrix();
    }

    @Override
    public TestDefinition getCurrentTestDefinition(final String test) throws StoreException {
        return delegate.getCurrentTestDefinition(test);
    }

    @Override
    public void verifySetup() throws StoreException {
        delegate.verifySetup();
    }

    @Override
    public String getLatestVersion() throws StoreException {
        return cacheHolder.getCachedLatestVersion();
    }

    @Override
    public TestMatrixVersion getTestMatrix(final String fetchRevision) throws StoreException {
        return cacheHolder.getCachedTestMatrix(fetchRevision);
    }

    @Override
    public TestDefinition getTestDefinition(final String test, final String fetchRevision) throws StoreException {

        if (!cacheHolder.getCachedHistory().containsKey(test)) {
            LOGGER.info(String.format("Test {%s} doesn't exist", test));
            return null;
        }

        final TestMatrixVersion version = cacheHolder.getCachedTestMatrix(fetchRevision);
        if (version == null) {
            LOGGER.info(String.format("Fetch version {%s} doesn't exists", fetchRevision));
            return null;
        }

        final TestMatrixDefinition testMatrixDefinition = version.getTestMatrixDefinition();
        if (testMatrixDefinition == null) {
            LOGGER.info(String.format("Fetch version {%s} doesn't contain any test", fetchRevision));
            return null;
        }

        final TestDefinition testDefinition = testMatrixDefinition.getTests().get(test);
        if (testDefinition == null) {
            LOGGER.info(String.format("Fetch version {%s} doesn't contain test {%s}", fetchRevision, test));
        }

        return testDefinition;
    }

    /**
     * caching is not supported for this method
     **/
    @Override
    public List<Revision> getMatrixHistory(final int start, final int limit) throws StoreException {
        return delegate.getMatrixHistory(start, limit);
    }

    @Override
    public List<Revision> getHistory(final String test, final int start, final int limit) throws StoreException {
        return selectHistorySet(cacheHolder.getCachedHistory().get(test), start, limit);
    }

    @Override
    public List<Revision> getHistory(final String test, final String revision, final int start, final int limit) throws StoreException {
        final List<Revision> revisions = cacheHolder.getCachedHistory().get(test);
        int i = 0;
        for (final Revision rev : revisions) {
            if (rev.getRevision().equals(revision)) {
                break;
            }
            i++;
        }
        return selectHistorySet(revisions, start + i, limit);
    }

    @Override
    public Map<String, List<Revision>> getAllHistories() throws StoreException {
        return cacheHolder.getCachedHistory();
    }

    @Override
    public void refresh() throws StoreException {
        delegate.refresh();
    }

    public static <T> List<T> selectHistorySet(final List<T> histories, final int start, final int limit) {
        if ((histories == null) || (start >= histories.size()) || (limit < 1)) {
            return Collections.emptyList();
        }
        final int s = Math.max(start, 0);
        final int l = Math.min(limit, histories.size() - s); /** to avoid overflow**/
        return histories.subList(s, s + l);
    }

    @Override
    public boolean cleanUserWorkspace(final String username) {
        return delegate.cleanUserWorkspace(username);
    }

    /**
     * Following methods will make side-effect and it would trigger cache updating at once
     */

    @Override
    public void updateTestDefinition(final String username,
                                     final String password,
                                     final String previousVersion,
                                     final String testName,
                                     final TestDefinition testDefinition,
                                     final Map<String, String> metadata,
                                     final String comment) throws StoreException.TestUpdateException {
        delegate.updateTestDefinition(username, password, previousVersion, testName, testDefinition, metadata, comment);
        cacheHolder.reschedule();
    }

    @Override
    public void deleteTestDefinition(final String username,
                                     final String password,
                                     final String previousVersion,
                                     final String testName,
                                     final TestDefinition testDefinition,
                                     final String comment) throws StoreException.TestUpdateException {
        delegate.deleteTestDefinition(username, password, previousVersion, testName, testDefinition, comment);
        cacheHolder.reschedule();
    }

    @Override
    public void addTestDefinition(final String username,
                                  final String password,
                                  final String testName,
                                  final TestDefinition testDefinition,
                                  final Map<String, String> metadata,
                                  final String comment) throws StoreException.TestUpdateException {
        delegate.addTestDefinition(username, password, testName, testDefinition, metadata, comment);
        cacheHolder.reschedule();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }
}
