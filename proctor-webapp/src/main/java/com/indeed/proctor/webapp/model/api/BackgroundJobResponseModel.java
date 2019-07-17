package com.indeed.proctor.webapp.model.api;

import com.indeed.proctor.webapp.jobs.BackgroundJob;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.UUID;

@ApiModel(value = "Background Job status", description = "For asynchronous requests starting background jobs, returns data to poll status.")
public class BackgroundJobResponseModel {

    private final BackgroundJob<?> job;

    public BackgroundJobResponseModel(final BackgroundJob<?> job) {
        this.job = job;
    }

    @ApiModelProperty("an id for background job.")
    public UUID getJobId() {
        return job.getUUID();
    }

    @ApiModelProperty("job status")
    public BackgroundJob.JobStatus getStatus() {
        return job.getStatus();
    }

    @ApiModelProperty("job status logs")
    public String getLog() {
        return job.getLog();
    }

    @ApiModelProperty("a title of the background job")
    public String getTitle() {
        return job.getTitle();
    }

    @ApiModelProperty("boolean flag to check if it's running or not.")
    public boolean isRunning() {
        return job.isRunning();
    }

    @ApiModelProperty("a list of urls that is gerated by the job and shown on result page in web interface.")
    public List<BackgroundJob.ResultUrl> getUrls() {
        return job.getUrls();
    }

    @ApiModelProperty("a message shown at the end of the job")
    public String getEndMessage() {
        return job.getEndMessage();
    }


}
