package com.indeed.proctor.store.cache;

import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Caching test histories
 */
public class ProctorStoreCaching implements ProctorStore {

    private static final Logger LOGGER = Logger.getLogger(ProctorStoreCaching.class);
    private static final long REFRESH_RATE_IN_SECOND = 10;
    private static final long READ_TIMEOUT_IN_SECOND = 30;
    private static final long WRITE_TIMEOUT_IN_SECOND = 180;
    private final ProctorStore delegate;
    private final CacheHolder cacheHolder;

    public ProctorStoreCaching(final ProctorStore delegate) {
        this.delegate = delegate;
        cacheHolder = new CacheHolder();
        try {
            /**
             * We assume local repository is initialized, so we can call cacheHolder#start in constructor
             */
            cacheHolder.start();
        } catch (final StoreException e) {
            LOGGER.error("Failed to initialize ProctorStoreCaching", e);
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

    class CacheHolder {

        private final AtomicReference<Map<String, List<Revision>>> historyCache = new AtomicReference<Map<String, List<Revision>>>();
        private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        private final Lock readLock = readWriteLock.readLock();
        private final Lock writeLock = readWriteLock.writeLock();

        private final AtomicReference<String> cachedLatestVersion = new AtomicReference<String>();
        /* version information would changed so we don't expire */
        private final Cache<String, TestMatrixVersion> versionCache = CacheBuilder.newBuilder()
                .maximumSize(5)
                .build();

        private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        private ScheduledFuture<?> scheduledFuture;
        //private final ProctorStore proctorStore;

        /*public CacheHolder(final ProctorStore proctorStore) {
            delegate = proctorStore;
        }*/

        final Runnable updateCacheTask = new Runnable() {
            @Override
            public void run() {
                try {
                    tryUpdateCache();
                } catch (final StoreException e) {
                    LOGGER.error("Failed to update cache", e);
                }
            }
        };

        @Nonnull
        public Map<String, List<Revision>> getCachedHistory() throws StoreException {
            return synchronizedCacheRead(new Callable<Map<String, List<Revision>>>() {
                @Override
                public Map<String, List<Revision>> call() {
                    return historyCache.get();
                }
            });
        }

        @Nonnull
        public String getCachedLatestVersion() throws StoreException {
            return synchronizedCacheRead(new Callable<String>() {
                @Override
                public String call() {
                    return cachedLatestVersion.get();
                }
            });
        }

        public TestMatrixVersion getCachedTestMatrix(final String fetchVersion) throws StoreException {
            return synchronizedCacheRead(new Callable<TestMatrixVersion>() {
                @Override
                public TestMatrixVersion call() throws StoreException {
                    TestMatrixVersion testMatrix = versionCache.getIfPresent(fetchVersion);
                    if (testMatrix == null) {
                        testMatrix = delegate.getTestMatrix(fetchVersion);
                        versionCache.put(fetchVersion, testMatrix);
                    }
                    return testMatrix;
                }
            });
        }

        public TestMatrixVersion getCachedCurrentTestMatrix() throws StoreException {
            return synchronizedCacheRead(new Callable<TestMatrixVersion>() {
                @Override
                public TestMatrixVersion call() throws StoreException {
                    final String latestVersion = getCachedLatestVersion();
                    return getCachedTestMatrix(latestVersion);
                }
            });
        }

        private boolean hasNewVersion() throws StoreException {
            final String newVersion = delegate.getLatestVersion();
            return !newVersion.equals(getCachedLatestVersion());
        }

        protected void tryUpdateCache() throws StoreException {
            delegate.refresh();
            if (hasNewVersion()) {
                lockAndUpdateCache();
            } else {
                LOGGER.info(String.format("[%s] Latest version not changed, no need to update cache", delegate.getName()));
            }
        }

        private void lockAndUpdateCache() throws StoreException {
            LOGGER.debug(String.format("[%s] Cache reload started", delegate.getName()));
            synchronizedCacheWrite(new Callable<Void>() {
                @Override
                public Void call() throws StoreException {
                    final TestMatrixVersion currentTestMatrix = delegate.getCurrentTestMatrix();
                    final String newVersion = currentTestMatrix.getVersion();
                    final Map<String, List<Revision>> allHistories = delegate.getAllHistories();
                    versionCache.put(newVersion, currentTestMatrix);
                    cachedLatestVersion.set(newVersion);
                    historyCache.set(allHistories);
                    return null;
                }
            });
            LOGGER.debug(String.format("[%s] Cache reload finished", delegate.getName()));
        }

        public void start() throws StoreException {
            LOGGER.info(String.format("[%s] Starting CacheHolder for branch ", delegate.getName()));
            lockAndUpdateCache();
            scheduledFuture = scheduledExecutorService
                    .scheduleWithFixedDelay(updateCacheTask, REFRESH_RATE_IN_SECOND, REFRESH_RATE_IN_SECOND, TimeUnit.SECONDS);
        }

        public void reschedule() {
            /**
             * cancel scheduled task, executing task is allowed to finish;
             */
            LOGGER.info(String.format("[%s] Rescheduling UpdateCacheTask due to new updates.", delegate.getName()));
            scheduledFuture.cancel(false);
            try {
                lockAndUpdateCache();
            } catch (final StoreException e) {
                LOGGER.error("failed to update the cache");
            }

            scheduledFuture = scheduledExecutorService
                    .scheduleWithFixedDelay(updateCacheTask, REFRESH_RATE_IN_SECOND, REFRESH_RATE_IN_SECOND, TimeUnit.SECONDS);
        }

        private <T> T synchronizedCacheRead(final Callable<T> callable) throws StoreException {
            try {
                if (readLock.tryLock(READ_TIMEOUT_IN_SECOND, TimeUnit.SECONDS)) {
                    try {
                        return callable.call();
                    } catch (final Exception e) {
                        throw new StoreException("Failed perform read operation to read. ", e);
                    } finally {
                        readLock.unlock();
                    }
                } else {
                    throw new StoreException("Cached is locked. Failed to acquire the lock. timeout. " + READ_TIMEOUT_IN_SECOND);
                }
            } catch (final InterruptedException e) {
                throw new StoreException("Read operation to cache was interrupted", e);
            }
        }

        private <T> T synchronizedCacheWrite(final Callable<T> callable) throws StoreException {
            try {
                if (writeLock.tryLock(WRITE_TIMEOUT_IN_SECOND, TimeUnit.SECONDS)) {
                    try {
                        return callable.call();
                    } catch (final Exception e) {
                        throw new StoreException.TestUpdateException("Failed perform write operation to cache. ", e);
                    } finally {
                        writeLock.unlock();
                    }
                } else {
                    throw new StoreException.TestUpdateException("Failed to acquire the lock " + WRITE_TIMEOUT_IN_SECOND);
                }
            } catch (final InterruptedException e) {
                throw new StoreException.TestUpdateException("Write operation to cache was interrupted", e);
            }
        }
    }
}
