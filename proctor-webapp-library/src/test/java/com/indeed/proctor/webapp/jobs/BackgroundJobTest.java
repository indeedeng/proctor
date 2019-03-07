package com.indeed.proctor.webapp.jobs;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BackgroundJobTest {

    private BackgroundJob<Object> job;
    private static final BackgroundJobFactory BACKGROUND_JOB_FACTORY = new BackgroundJobFactory();

    @Before
    public void setUp() {
        final BackgroundJobFactory.Executor<Object> function = j -> "foo";
        job = createBackgroundJob(function);
    }

    private BackgroundJob<Object> createBackgroundJob(final BackgroundJobFactory.Executor<Object> function) {
        return BACKGROUND_JOB_FACTORY.createBackgroundJob("fooTitle", "fooUser", BackgroundJob.JobType.TEST_EDIT, function);
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

}