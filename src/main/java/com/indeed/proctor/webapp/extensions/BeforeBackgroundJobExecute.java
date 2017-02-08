package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.webapp.controllers.BackgroundJob;

public interface BeforeBackgroundJobExecute {
    void beforeExecute(final BackgroundJob job);
}
