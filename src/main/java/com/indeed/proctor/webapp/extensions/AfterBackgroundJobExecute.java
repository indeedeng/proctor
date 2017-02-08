package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.webapp.controllers.BackgroundJob;

public interface AfterBackgroundJobExecute {
    void afterExecute(final BackgroundJob job, final Throwable t);
}
