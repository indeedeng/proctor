package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.webapp.jobs.BackgroundJob;

import java.util.UUID;

public interface JobInfoStore {
    boolean shouldUpdateJobInfo(final BackgroundJob<?> job);

    void updateJobInfo(final UUID uuid, final BackgroundJob.JobInfo jobInfo);

    BackgroundJob.JobInfo getJobInfo(final UUID uuid);
}
