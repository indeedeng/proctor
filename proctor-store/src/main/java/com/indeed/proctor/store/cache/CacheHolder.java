package com.indeed.proctor.store.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
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

public class CacheHolder {

    private static final Logger LOGGER = Logger.getLogger(CacheHolder.class);
    private static final long RATE_IN_SECOND = 10;
    private static final long READ_TIMEOUT_IN_SECOND = 30;
    private static final long WRITE_TIMEOUT_IN_SECOND = 180;

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
    private final ProctorStore proctorStore;

    public CacheHolder(final ProctorStore proctorStore) {
        this.proctorStore = proctorStore;
    }

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
            public Map<String, List<Revision>> call() throws Exception {
                return historyCache.get();
            }
        });
    }

    @Nonnull
    public String getCachedLatestVersion() throws StoreException {
        return synchronizedCacheRead(new Callable<String>() {
            @Override
            public String call() throws Exception {
                return cachedLatestVersion.get();
            }
        });
    }

    public TestMatrixVersion getCachedTestMatrix(final String fetchVersion) throws StoreException {
        return synchronizedCacheRead(new Callable<TestMatrixVersion>() {
            @Override
            public TestMatrixVersion call() throws Exception {
                TestMatrixVersion testMatrix = versionCache.getIfPresent(fetchVersion);
                if (testMatrix == null) {
                    testMatrix = proctorStore.getTestMatrix(fetchVersion);
                    versionCache.put(fetchVersion, testMatrix);
                }
                return testMatrix;
            }
        });
    }

    public TestMatrixVersion getCachedCurrentTestMatrix() throws StoreException {
        return synchronizedCacheRead(new Callable<TestMatrixVersion>() {
            @Override
            public TestMatrixVersion call() throws Exception {
                final String latestVersion = getCachedLatestVersion();
                return getCachedTestMatrix(latestVersion);
            }
        });
    }

    private boolean hasNewVersion() throws StoreException {
        final String newVersion = proctorStore.getLatestVersion();
        return !newVersion.equals(getCachedLatestVersion());
    }

    protected void tryUpdateCache() throws StoreException {
        proctorStore.refresh();
        if (hasNewVersion()) {
            lockAndUpdateCache();
        } else {
            LOGGER.info(String.format("[%s] Latest version not changed, no need to update cache", proctorStore.getName()));
        }
    }

    private void lockAndUpdateCache() throws StoreException {
        LOGGER.info(String.format("[%s] Cache reload started", proctorStore.getName()));
        synchronizedCacheWrite(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                final TestMatrixVersion currentTestMatrix = proctorStore.getCurrentTestMatrix();
                final String newVersion = currentTestMatrix.getVersion();
                final Map<String, List<Revision>> allHistories = proctorStore.getAllHistories();
                versionCache.put(newVersion, currentTestMatrix);
                cachedLatestVersion.set(newVersion);
                historyCache.set(allHistories);
                return null;
            }
        });
        LOGGER.info(String.format("[%s] Cache reload finished", proctorStore.getName()));
    }

    public void start() throws StoreException {
        LOGGER.info(String.format("[%s] Starting CacheHolder for branch ", proctorStore.getName()));
        lockAndUpdateCache();
        scheduledFuture = scheduledExecutorService
                .scheduleWithFixedDelay(updateCacheTask, RATE_IN_SECOND, RATE_IN_SECOND, TimeUnit.SECONDS);
    }

    public void reschedule() {
        /**
         * cancel scheduled task, executing task is allowed to finish;
         */
        LOGGER.info(String.format("[%s] Rescheduling UpdateCacheTask due to new updates.", proctorStore.getName()));
        scheduledFuture.cancel(false);
        try {
            lockAndUpdateCache();
        } catch (final StoreException e) {
            LOGGER.error("failed to update the cache");
        }

        scheduledFuture = scheduledExecutorService
                .scheduleWithFixedDelay(updateCacheTask, RATE_IN_SECOND, RATE_IN_SECOND, TimeUnit.SECONDS);
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
