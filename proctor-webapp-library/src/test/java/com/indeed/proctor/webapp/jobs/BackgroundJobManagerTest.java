package com.indeed.proctor.webapp.jobs;

import com.indeed.proctor.webapp.extensions.JobInfoStore;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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


    private BackgroundJobManager manager = new BackgroundJobManager(new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>()));
    private JobInfoStore infoStoreMock = mock(JobInfoStore.class);

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
        assertThat(manager.getRecentJobs()).isEmpty();

        when(infoStoreMock.shouldUpdateJobInfo(isA(BackgroundJob.class))).thenReturn(true);
        manager.scheduledCacheUpdate();
        verify(infoStoreMock, times(1)).shouldUpdateJobInfo(job);
        verify(infoStoreMock, times(2)).updateJobInfo(any(UUID.class), any(BackgroundJob.JobInfo.class));
    }

    private BackgroundJob mockBackgroundJob() {
        final BackgroundJob mock = mock(BackgroundJob.class);
        when(mock.getFuture()).thenReturn(CompletableFuture.completedFuture("Done"));
        final BackgroundJob.JobInfo jobInfoMock = mock(BackgroundJob.JobInfo.class);
        when(mock.getJobInfo()).thenReturn(jobInfoMock);
        return mock;
    }
}
