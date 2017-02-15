package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.webapp.controllers.BackgroundJob;

public interface AfterBackgroundJobExecute {
    <T> void afterExecute(final BackgroundJob<T> job, T result);
}
