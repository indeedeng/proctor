package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.webapp.jobs.BackgroundJob;

public interface AfterBackgroundJobExecute {
    <T> void afterExecute(final BackgroundJob<T> job, T result);
}
