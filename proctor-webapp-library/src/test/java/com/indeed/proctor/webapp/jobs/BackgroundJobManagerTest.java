package com.indeed.proctor.webapp.jobs;

import com.indeed.proctor.webapp.extensions.JobInfoStore;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BackgroundJobManagerTest {

    private final BackgroundJobManager manager =
            new BackgroundJobManager(
                    new ThreadPoolExecutor(
                            1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()));
    private final JobInfoStore infoStoreMock = mock(JobInfoStore.class);

    @Before
    public void setUp() {
        manager.setJobInfoStore(infoStoreMock);
    }

    @Test
    public void submit() {
        assertThat(manager.getRecentJobs()).isEmpty();
        manager.scheduledCacheUpdate();
        verify(infoStoreMock, never()).shouldUpdateJobInfo(any(BackgroundJob.class));

        final BackgroundJob<?> job = mockBackgroundJob();
        manager.submit(job);
        assertThat(manager.getRecentJobs()).containsExactly(job);

        when(infoStoreMock.shouldUpdateJobInfo(isA(BackgroundJob.class))).thenReturn(true);
        manager.scheduledCacheUpdate();
        verify(infoStoreMock, times(1)).shouldUpdateJobInfo(job);
        verify(infoStoreMock, times(2))
                .updateJobInfo(any(UUID.class), any(BackgroundJob.JobInfo.class));
    }

    @Test
    public void testEvictLeastRecentlyInsertedJobFromHistory() {
        final BackgroundJob<?> firstJob = mockBackgroundJob();
        manager.submit(firstJob);

        // Get the first job id with argument capture
        final ArgumentCaptor<UUID> argument = ArgumentCaptor.forClass(UUID.class);
        verify(firstJob).setUUID(argument.capture());
        final UUID firstJobId = argument.getValue();

        final int firstHalfSize = BackgroundJobManager.JOB_HISTORY_MAX_SIZE / 2;
        for (int i = 0; i < firstHalfSize; i++) {
            manager.submit(mockBackgroundJob());
        }
        // expected to have job before reaching to the max size
        // also accessing to the job will help to make sure it's not LRU.
        assertThat(manager.getJobForId(firstJobId)).isNotNull();

        final int secondHalfSize = BackgroundJobManager.JOB_HISTORY_MAX_SIZE - firstHalfSize;
        for (int i = 0; i < secondHalfSize; i++) {
            manager.submit(mockBackgroundJob());
        }
        // expected to have no history of the first job just after reaching to the max size
        assertThat(manager.getJobForId(firstJobId)).isNull();
    }

    /** Checks scheduledCacheUpdate is thread-safe for insertion during iteration */
    @Test(timeout = 10000) // timeout just in case it's in dead lock due to bug
    public void testScheduledCacheUpdateWithSubmit() throws InterruptedException {
        manager.submit(mockBackgroundJob());
        manager.submit(mockBackgroundJob());
        manager.submit(mockBackgroundJob());

        // for the update method to wait until submit is done
        final CountDownLatch submitIsCalled = new CountDownLatch(1);
        // for the main thread to wait until cacheUpdate is in loop
        final CountDownLatch isInUpdateLoop = new CountDownLatch(1);

        manager.setJobInfoStore(
                new JobInfoStore() {
                    @Override
                    public boolean shouldUpdateJobInfo(final BackgroundJob<?> job) {
                        try {
                            // release wait of the main thread to start submit()
                            if (isInUpdateLoop.getCount() > 0) {
                                isInUpdateLoop.countDown();
                            }
                            // wait until submit is called.
                            submitIsCalled.await(10, TimeUnit.SECONDS); // timeout for safety
                        } catch (final InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        return true;
                    }

                    @Override
                    public void updateJobInfo(
                            final UUID uuid, final BackgroundJob.JobInfo jobInfo) {
                        // Do nothing here.
                        // synchronization is done in shouldUpdateJobInfo that is not called by
                        // submit()
                    }

                    @Override
                    public BackgroundJob.JobInfo getJobInfo(final UUID uuid) {
                        return null;
                    }
                });

        // 1: start scheduledCacheUpdate()
        final Thread cacheUpdateThread = new Thread(manager::scheduledCacheUpdate);
        cacheUpdateThread.start();

        // 2: wait until cache update is started.
        isInUpdateLoop.await(10, TimeUnit.SECONDS); // timeout for safety

        // 3: now, scheduledCacheUpdate() is blocked by submitIsCalled.await() in
        // shouldUpdateJobInfo
        // submit new background job while scheduledCacheUpdate is waiting
        manager.submit(mockBackgroundJob());

        // 4: release wait of submitIsCalled. scheduledCacheUpdate will be running
        submitIsCalled.countDown();

        // 5: wait until scheduledCacheUpdate() is done.
        // it's expected to have ConcurrentModificationException here if it's not threadsafe,
        // or to block until timeout on over synchronization
        cacheUpdateThread.join(5000);
    }

    private BackgroundJob<?> mockBackgroundJob() {
        final BackgroundJob mock = mock(BackgroundJob.class);
        when(mock.getFuture()).thenReturn(CompletableFuture.completedFuture("Done"));
        final BackgroundJob.JobInfo jobInfoMock = mock(BackgroundJob.JobInfo.class);
        when(mock.getJobInfo()).thenReturn(jobInfoMock);
        return mock;
    }
}
