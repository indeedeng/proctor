package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.webapp.controllers.BackgroundJob;

public interface BeforeBackgroundJobExecute {
    <T> void beforeExecute(final BackgroundJob<T> job);
}
