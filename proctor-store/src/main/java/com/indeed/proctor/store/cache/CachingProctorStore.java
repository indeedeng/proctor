package com.indeed.proctor.store.cache;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.store.utils.HistoryUtil;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A decorator class for ProctorStore.
 * This class caches result of read methods and invalidates cache on add/update/delete methods
 *
 * @author yiqing
 */
public class CachingProctorStore implements ProctorStore {
    private static final Logger LOGGER = Logger.getLogger(CachingProctorStore.class);
    private static final long REFRESH_RATE_IN_SECOND = 15;
    private static final long READ_TIMEOUT_IN_SECOND = 30;
    private static final long WRITE_TIMEOUT_IN_SECOND = 180;

    /**
     * ProctorStore delegate.
     * We assume all the methods of delegate is thread-safe.
     */
    private final ProctorStore delegate;

    private final CacheHolder cacheHolder;

    public CachingProctorStore(final ProctorStore delegate) {
        this.delegate = delegate;
        cacheHolder = new CacheHolder();
        try {
            /**
             * We assume local repository is initialized, so we can call cacheHolder#start in constructor
             */
            cacheHolder.start();
        } catch (final StoreException e) {
            LOGGER.error("Failed to initialize CachingProctorStore", e);
            throw Throwables.propagate(e);
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
        final TestMatrixDefinition testMatrixDefinition = cacheHolder.getCachedCurrentTestMatrix().getTestMatrixDefinition();
        Preconditions.checkNotNull(testMatrixDefinition, "TestMatrix should contain non null TestMatrixDefinition");
        return testMatrixDefinition.getTests().get(test);
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

        if (isChangedRevision(test, fetchRevision)) {
            return cacheHolder.getCachedTestDefinition(test, fetchRevision);
        } else {
            return delegate.getTestDefinition(test, fetchRevision);
        }
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
        return HistoryUtil.selectHistorySet(cacheHolder.getCachedHistory().get(test), start, limit);
    }

    @Override
    public List<Revision> getHistory(final String test, final String revision, final int start, final int limit) throws StoreException {
        return HistoryUtil.selectRevisionHistorySetFrom(cacheHolder.getCachedHistory().get(test), revision, start, limit);
    }

    @Override
    public Map<String, List<Revision>> getAllHistories() throws StoreException {
        return cacheHolder.getCachedHistory();
    }

    @Override
    public void refresh() throws StoreException {
        cacheHolder.refreshAll();
    }

    /**
     * Please use {@link HistoryUtil#selectHistorySet(List, int, int)}
     */
    @Deprecated
    public static <T> List<T> selectHistorySet(final List<T> histories, final int start, final int limit) {
        return HistoryUtil.selectHistorySet(histories, start, limit);
    }

    /**
     * Please use {@link HistoryUtil#selectRevisionHistorySetFrom(List, String, int, int)}
     */
    @Deprecated
    public static List<Revision> selectRevisionHistorySetFrom(final List<Revision> history, final String from, final int start, final int limit) {
        return HistoryUtil.selectRevisionHistorySetFrom(history, from, start, limit);
    }

    @Override
    public boolean cleanUserWorkspace(final String username) {
        return delegate.cleanUserWorkspace(username);
    }

    /**
     * Following three methods make side-effect and it would trigger cache refreshing at once
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
        cacheHolder.startRefreshCacheTask();
    }

    @Override
    public void updateTestDefinition(final String username,
                                     final String password,
                                     final String author,
                                     final String previousVersion,
                                     final String testName,
                                     final TestDefinition testDefinition,
                                     final Map<String, String> metadata,
                                     final String comment) throws StoreException.TestUpdateException {
        delegate.updateTestDefinition(username, password, author, previousVersion, testName, testDefinition, metadata, comment);
        cacheHolder.startRefreshCacheTask();
    }

    @Override
    public void deleteTestDefinition(final String username,
                                     final String password,
                                     final String previousVersion,
                                     final String testName,
                                     final TestDefinition testDefinition,
                                     final String comment) throws StoreException.TestUpdateException {
        delegate.deleteTestDefinition(username, password, previousVersion, testName, testDefinition, comment);
        cacheHolder.startRefreshCacheTask();
    }

    @Override
    public void deleteTestDefinition(final String username,
                                     final String password,
                                     final String author,
                                     final String previousVersion,
                                     final String testName,
                                     final TestDefinition testDefinition,
                                     final String comment) throws StoreException.TestUpdateException {
        delegate.deleteTestDefinition(username, password, author, previousVersion, testName, testDefinition, comment);
        cacheHolder.startRefreshCacheTask();
    }

    @Override
    public void addTestDefinition(final String username,
                                  final String password,
                                  final String testName,
                                  final TestDefinition testDefinition,
                                  final Map<String, String> metadata,
                                  final String comment) throws StoreException.TestUpdateException {
        delegate.addTestDefinition(username, password, testName, testDefinition, metadata, comment);
        cacheHolder.startRefreshCacheTask();
    }

    @Override
    public void addTestDefinition(final String username,
                                  final String password,
                                  final String author,
                                  final String testName,
                                  final TestDefinition testDefinition,
                                  final Map<String, String> metadata,
                                  final String comment) throws StoreException.TestUpdateException {
        delegate.addTestDefinition(username, password, author, testName, testDefinition, metadata, comment);
        cacheHolder.startRefreshCacheTask();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @VisibleForTesting
    ScheduledFuture<?> getRefreshTaskFuture() {
        return cacheHolder.scheduledFuture;
    }

    private boolean isChangedRevision(final String test, final String revision) throws StoreException {
        final List<Revision> revisions = cacheHolder.getCachedHistory().get(test);
        if (revisions != null) {
            for (final Revision r : revisions) {
                if (r.getRevision().equals(revision)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This class provides thread-safe read/write operations to the cached data including
     * the latest version, revision histories of all ProTest, maximum 3 versions of test Matrix
     * and maximum 5000 versions of test definitions.
     */
    class CacheHolder {
        private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        private final Lock readLock = readWriteLock.readLock();
        private final Lock writeLock = readWriteLock.writeLock();

        private TestMatrixVersion cachedLatestTestMatrixVersion;
        private Map<String, List<Revision>> historyCache;

        /* version information won't change so we don't expire */
        private final Cache<String, TestMatrixVersion> revisionTestMatrixCache = CacheBuilder.newBuilder()
                .maximumSize(3)
                .build();
        private final Cache<TDKey, TestDefinition> revisionTestDefinitionCache = CacheBuilder.newBuilder()
                .maximumSize(5000) // 5000 * (the size of test definition) ~ 50 MB
                .build();

        private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        private ScheduledFuture<?> scheduledFuture;

        /**
         * background task to refresh cache
         */
        final Runnable refreshCacheTask = new Runnable() {
            @Override
            public void run() {
                try {
                    refreshAll();
                } catch (final Exception e) {
                    LOGGER.error("Failed to update cache", e);
                }
            }
        };

        @Nonnull
        public Map<String, List<Revision>> getCachedHistory() throws StoreException {
            return synchronizedCacheRead(new Callable<Map<String, List<Revision>>>() {
                @Override
                public Map<String, List<Revision>> call() {
                    return historyCache;
                }
            });
        }

        @Nonnull
        public String getCachedLatestVersion() throws StoreException {
            return synchronizedCacheRead(new Callable<String>() {
                @Override
                public String call() {
                    return cachedLatestTestMatrixVersion.getVersion();
                }
            });
        }

        public TestMatrixVersion getCachedTestMatrix(final String fetchRevision) throws StoreException {
            return synchronizedCacheRead(new Callable<TestMatrixVersion>() {
                @Override
                public TestMatrixVersion call() throws StoreException {
                    TestMatrixVersion testMatrix = revisionTestMatrixCache.getIfPresent(fetchRevision);
                    if (testMatrix == null) {
                        LOGGER.debug("Cache miss for fetch revision: " + fetchRevision);
                        testMatrix = delegate.getTestMatrix(fetchRevision);
                        revisionTestMatrixCache.put(fetchRevision, testMatrix);
                    }
                    return testMatrix;
                }
            });
        }

        public TestMatrixVersion getCachedCurrentTestMatrix() throws StoreException {
            return synchronizedCacheRead(new Callable<TestMatrixVersion>() {
                @Override
                public TestMatrixVersion call() throws StoreException {
                    return cachedLatestTestMatrixVersion;
                }
            });
        }

        public TestDefinition getCachedTestDefinition(final String testName, final String fetchRevision) throws StoreException {
            return synchronizedCacheRead(new Callable<TestDefinition>() {
                @Override
                public TestDefinition call() throws Exception {
                    final TDKey key = new TDKey(testName, fetchRevision);
                    TestDefinition testDefinition = revisionTestDefinitionCache.getIfPresent(key);
                    if (testDefinition == null) {
                        LOGGER.debug("Cache miss for test definition : name=" + testName + " revision=" + fetchRevision);
                        testDefinition = delegate.getTestDefinition(testName, fetchRevision);
                        revisionTestDefinitionCache.put(key, testDefinition);
                    }
                    return testDefinition;
                }
            });
        }

        private boolean hasNewVersion() throws StoreException {
            final String newVersion = delegate.getLatestVersion();
            return !newVersion.equals(getCachedLatestVersion());
        }

        /**
         * This method refreshes ProctorStore delegate and determines whether to refresh cache data.
         *
         * @throws StoreException
         */
        public void refreshAll() throws StoreException {
            delegate.refresh();
            if (hasNewVersion()) {
                lockAndRefreshCache();
            } else {
                LOGGER.debug(String.format("[%s] Latest version is not changed. Do not refresh cache", delegate.getName()));
            }
        }

        /**
         * This method refreshes cache data.
         * Other read/write operations are blocked
         *
         * @throws StoreException
         */
        private void lockAndRefreshCache() throws StoreException {
            LOGGER.debug(String.format("[%s] Refreshing cache data started", delegate.getName()));
            synchronizedCacheWrite(new Callable<Void>() {
                @Override
                public Void call() throws StoreException {
                    final TestMatrixVersion currentTestMatrix = delegate.getCurrentTestMatrix();
                    final Revision revision = delegate.getMatrixHistory(0, 1).get(0);
                    final Map<String, List<Revision>> allHistories = delegate.getAllHistories();
                    revisionTestMatrixCache.put(revision.getRevision(), currentTestMatrix);
                    cachedLatestTestMatrixVersion = currentTestMatrix;
                    historyCache = allHistories;
                    return null;
                }
            });
            LOGGER.debug(String.format("[%s] Refreshing cache data finished", delegate.getName()));
        }

        /**
         * This method initialize CacheHolder.
         * It loads data to cache and starts background task to refresh cache periodically
         *
         * @throws StoreException
         */
        public void start() throws StoreException {
            LOGGER.info(String.format("[%s] Starting Caching for ProctorStore ", delegate.getName()));
            lockAndRefreshCache();
            scheduledFuture = scheduledExecutorService
                    .scheduleWithFixedDelay(refreshCacheTask, REFRESH_RATE_IN_SECOND, REFRESH_RATE_IN_SECOND, TimeUnit.SECONDS);
        }

        /**
         * This method refreshes cache at once.
         * Cache is invalidated until the method completes. Read operations are blocked in this method.
         */
        public void startRefreshCacheTask() {
            LOGGER.info(String.format("[%s] Rescheduling UpdateCacheTask due to new updates.", delegate.getName()));
            /**
             * cancel scheduled task, executing task is allowed to finish;
             */
            scheduledFuture.cancel(false);
            try {
                lockAndRefreshCache();
            } catch (final StoreException e) {
                LOGGER.error("failed to update the cache");
            }

            scheduledFuture = scheduledExecutorService
                    .scheduleWithFixedDelay(refreshCacheTask, REFRESH_RATE_IN_SECOND, REFRESH_RATE_IN_SECOND, TimeUnit.SECONDS);
        }

        /**
         * This method provides an interface to perform thread-safe <b>read</b> operation.
         * It only blocks other write operations.
         *
         * @param callable
         * @param <T>
         * @return
         * @throws StoreException
         */
        private <T> T synchronizedCacheRead(final Callable<T> callable) throws StoreException {
            try {
                if (readLock.tryLock(READ_TIMEOUT_IN_SECOND, TimeUnit.SECONDS)) {
                    try {
                        return callable.call();
                    } catch (final Exception e) {
                        throw new StoreException("Failed to perform read operation to cache. ", e);
                    } finally {
                        readLock.unlock();
                    }
                } else {
                    throw new StoreException("Failed to acquire the lock. Timeout after " + READ_TIMEOUT_IN_SECOND);
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new StoreException("Read operation to cache was interrupted", e);
            }
        }

        /**
         * This method provides an interface to perform thread-safe <b>write</b> operation.
         * It blocks other read/write operations.
         *
         * @param callable
         * @param <T>
         * @return
         * @throws StoreException
         */
        private <T> T synchronizedCacheWrite(final Callable<T> callable) throws StoreException {
            try {
                if (writeLock.tryLock(WRITE_TIMEOUT_IN_SECOND, TimeUnit.SECONDS)) {
                    try {
                        return callable.call();
                    } catch (final Exception e) {
                        throw new StoreException.TestUpdateException("Failed to perform write operation to cache. ", e);
                    } finally {
                        writeLock.unlock();
                    }
                } else {
                    throw new StoreException.TestUpdateException("Failed to acquire the lock. Timeout after " + WRITE_TIMEOUT_IN_SECOND);
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new StoreException.TestUpdateException("Write operation to cache was interrupted", e);
            }
        }

        private class TDKey {
            private final String test;
            private final String fetchRevision;

            private TDKey(final String test, final String fetchRevision) {
                this.test = test;
                this.fetchRevision = fetchRevision;
            }

            @Override
            public boolean equals(final Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                final TDKey tdKey = (TDKey) o;
                return Objects.equals(test, tdKey.test) &&
                        Objects.equals(fetchRevision, tdKey.fetchRevision);
            }

            @Override
            public int hashCode() {
                return Objects.hash(test, fetchRevision);
            }
        }
    }
}
