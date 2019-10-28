package com.indeed.proctor.webapp.model.api;

import com.indeed.proctor.webapp.jobs.BackgroundJob;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.annotation.CheckForNull;
import java.util.List;
import java.util.UUID;

@ApiModel(value = "Background Job status", description = "For asynchronous requests starting background jobs, returns data to poll status.")
public class BackgroundJobResponseModel {

    private final BackgroundJob.JobInfo jobInfo;

    public BackgroundJobResponseModel(final BackgroundJob<?> job) {
        this(job.getJobInfo());
    }

    public BackgroundJobResponseModel(final BackgroundJob.JobInfo jobInfo) {
        this.jobInfo = jobInfo;
    }

    @ApiModelProperty("an id for background jobInfo.")
    public UUID getJobId() {
        return jobInfo.getJobId();
    }

    @ApiModelProperty("job status")
    public BackgroundJob.JobStatus getStatus() {
        return jobInfo.getStatus();
    }

    @ApiModelProperty("job status logs")
    public String getLog() {
        return jobInfo.getLog();
    }

    @ApiModelProperty("a title of the background job")
    public String getTitle() {
        return jobInfo.getTitle();
    }

    @ApiModelProperty("boolean flag to check if it's running or not.")
    public boolean isRunning() {
        return jobInfo.isRunning();
    }

    @ApiModelProperty("a list of urls that is gerated by the job and shown on result page in web interface.")
    public List<BackgroundJob.ResultUrl> getUrls() {
        return jobInfo.getUrls();
    }

    @ApiModelProperty("a message shown at the end of the job")
    public String getEndMessage() {
        return jobInfo.getEndMessage();
    }
}
