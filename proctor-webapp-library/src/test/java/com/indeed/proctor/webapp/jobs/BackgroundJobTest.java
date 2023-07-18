package com.indeed.proctor.webapp.jobs;

import com.indeed.proctor.webapp.jobs.AutoPromoter.AutoPromoteFailedException;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BackgroundJobTest {

    private BackgroundJob<Object> job;
    private static final BackgroundJobFactory BACKGROUND_JOB_FACTORY = new BackgroundJobFactory();

    @Before
    public void setUp() {
        final BackgroundJobFactory.Executor<Object> function = j -> "foo";
        job = createBackgroundJob(function);
    }

    private BackgroundJob<Object> createBackgroundJob(
            final BackgroundJobFactory.Executor<Object> function) {
        return BACKGROUND_JOB_FACTORY.createBackgroundJob(
                "fooTitle", "fooUser", BackgroundJob.JobType.TEST_EDIT, function);
    }

    @Test
    public void log() {
        job.log("hello");
        assertThat(job.getLog()).isEqualTo("hello\n");
    }

    @Test
    public void logWithTiming() {
        assertThat(job.getTimings()).isEmpty();
        job.logWithTiming("hello", "greet");
        assertThat(job.getLog()).isEqualTo("hello\n");

        assertThat(job.getTimings()).containsKeys("init");
        job.logComplete();
        assertThat(job.getTimings()).containsKeys("init", "greet");
    }

    @Test
    public void logPartialSuccess() {
        final String message = "auto-promote failed";
        final Future future = mock(Future.class);
        when(future.isCancelled()).thenReturn(false);
        job.setFuture(future);

        job.logPartialSuccess(new AutoPromoteFailedException(message));

        assertThat(job.getLog()).isEqualTo("Partial Success:\n" + message + "\n");
        assertThat(job.getError()).isInstanceOf(AutoPromoteFailedException.class);
        assertThat(job.getStatus()).isEqualTo(BackgroundJob.JobStatus.PARTIAL_SUCCESS);
    }
}
