package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.webapp.jobs.BackgroundJob;

import java.util.Map;
import java.util.UUID;

public interface JobStatusStore {
    boolean shouldUpdateJobStatus(final BackgroundJob job);

    void updateJobStatus(final UUID uuid, final Map<String, Object> status);

    Map<String, Object> getJobStatus(final UUID uuid);
}
