package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.webapp.jobs.BackgroundJob;

public class BackgroundJobChangeLog implements DefinitionChangeLog {
    private final BackgroundJob backgroundJob;

    public BackgroundJobChangeLog(final BackgroundJob backgroundJob) {
        this.backgroundJob = backgroundJob;
    }

    @Override
    public void logMessage(final String message) {
        backgroundJob.log(message);
    }

    @Override
    public void addUrl(final String url, final String text, final String target) {
        backgroundJob.addUrl(url, text, target);
    }
}
