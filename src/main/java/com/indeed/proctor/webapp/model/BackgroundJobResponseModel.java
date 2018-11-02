package com.indeed.proctor.webapp.model;

import com.indeed.proctor.webapp.jobs.BackgroundJob;
import io.swagger.annotations.ApiModel;

import java.util.List;
import java.util.UUID;

@ApiModel(value = "Background Job status", description = "For asynchronous requests starting background jobs, returns data to poll status.")
public class BackgroundJobResponseModel {

    private final BackgroundJob job;

    public BackgroundJobResponseModel(final BackgroundJob job) {
        this.job = job;
    }

    public UUID getJobId() {
        return job.getUUID();
    }

    public BackgroundJob.JobStatus getStatus() {
        return job.getStatus();
    }

    public String getLog() {
        return job.getLog();
    }

    public String getTitle() {
        return job.getTitle();
    }

    public boolean isRunning() {
        return job.isRunning();
    }

    public List<BackgroundJob.ResultUrl> getUrls() {
        return job.getUrls();
    }

    public String getEndMessage() {
        return job.getEndMessage();
    }


}
