package com.indeed.proctor.webapp.controllers;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.webapp.jobs.BackgroundJob;
import com.indeed.proctor.webapp.jobs.BackgroundJobFactory;
import com.indeed.proctor.webapp.jobs.BackgroundJobManager;
import com.indeed.proctor.webapp.model.SessionViewModel;
import com.indeed.proctor.webapp.model.WebappConfiguration;
import com.indeed.proctor.webapp.model.api.BackgroundJobResponseModel;
import com.indeed.proctor.webapp.views.JsonView;
import com.indeed.proctor.webapp.views.ProctorView;
import io.swagger.annotations.ApiOperation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.View;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Endpoints to query background job status/
 *
 * Endpoints:
 * - GET /rpc/jobs/list
 * - GET /rpc/jobs/status?id=
 * - GET /rpc/jobs/cancel?id=
 * - GET /rpc/jobs/test
 */
@Controller
@RequestMapping({"/rpc/jobs", "/proctor/rpc/jobs"})
public class BackgroundJobRpcController {
    private static final Logger LOGGER = LogManager.getLogger(BackgroundJobRpcController.class);

    private final WebappConfiguration configuration;
    private final BackgroundJobManager manager;
    private final BackgroundJobFactory factory;

    @Autowired
    public BackgroundJobRpcController(final BackgroundJobManager manager,
                                      final WebappConfiguration configuration,
                                      final BackgroundJobFactory factory) {
        this.manager = manager;
        this.configuration = configuration;
        this.factory = factory;
    }

    @ApiOperation(value = "Request background job status")
    @RequestMapping(value = "/status", method = RequestMethod.GET)
    @ResponseBody
    public JsonResponse<BackgroundJob.JobInfo> doGetJobStatus(@RequestParam("id") final UUID jobId) {
        final BackgroundJob.JobInfo jobInfo = manager.getJobInfo(jobId);

        if (jobInfo == null) {
            final String msg = "Failed to identify job for " + jobId;
            return new JsonResponse<>(null, false, msg);
        } else {
            return new JsonResponse<>(jobInfo, true, null);
        }
    }

    /**
     * sets spring model jobs attribute
     * @return new spring view name
     */
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public String doGetJobList(final Model model) {
        final List<BackgroundJob<?>> jobs = manager.getRecentJobs();
        model.addAttribute("session",
                SessionViewModel.builder()
                        .setUseCompiledCSS(configuration.isUseCompiledCSS())
                        .setUseCompiledJavaScript(configuration.isUseCompiledJavaScript())
                        .build());
        model.addAttribute("jobs", jobs);
        return ProctorView.JOBS.getName();
    }

    @ApiOperation(value = "Cancel a background job")
    @RequestMapping(value = "/cancel", method = RequestMethod.GET)
    public View doCancelJob(@RequestParam("id") final UUID jobId) {
        final BackgroundJob<?> job = manager.getJobForId(jobId);
        if (job == null) {
            final String msg = "Failed to identify job for " + jobId;
            final JsonResponse<String> err = new JsonResponse<>(msg, false, msg);
            return new JsonView(err);
        } else {
            if (job.getFuture() != null) {
                job.getFuture().cancel(true);
            }
            final BackgroundJobResponseModel result = new BackgroundJobResponseModel(job);
            final JsonResponse<BackgroundJobResponseModel> response = new JsonResponse<>(result, true, null);
            return new JsonView(response);
        }
    }

    @ApiOperation(value = "Test endpoint sleeps for ms milliseconds")
    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public View submitTestJob(@RequestParam(value = "ms", defaultValue = "1000") final long ms) {
        final long start = System.currentTimeMillis();
        final long interval = 100; // log every 100 milliseconds
        final BackgroundJob<Boolean> job = factory.createBackgroundJob(
                "Sleeping for a total of " + ms + " ms",
                "test-user",
                BackgroundJob.JobType.JOB_TEST,
                new BackgroundJobFactory.Executor<Boolean>() {
                    @Override
                    public Boolean execute(final BackgroundJob<Boolean> job) throws Exception {
                        final long endms = start + ms;
                        while (true) {
                            final long now = System.currentTimeMillis();
                            final long sleepms = Math.min(interval, endms - now);
                            if (sleepms > 0) {
                                final double elapsedSec = (now - start) / 1000.0;
                                job.log(String.format("Elapsed = %.3f seconds, sleeping for %s ms", elapsedSec, sleepms));
                                Thread.sleep(sleepms);
                            } else {
                                break;
                            }
                        }
                        job.addUrl("http://www.indeed.com", "Indeed.com");
                        job.addUrl("http://www.google.com", "Google", "_blank");
                        return Boolean.TRUE;
                    }
                }
        );
        manager.submit(job);

        final JsonResponse<BackgroundJobResponseModel> response =
                new JsonResponse<>(new BackgroundJobResponseModel(job), true, null);
        return new JsonView(response);
    }

    /**
     * @deprecated Use new BackgroundJobResponseModel(job)
     */
    @Deprecated
    public static Map<String, Object> buildJobJson(final BackgroundJob<?> job) {
        final ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

        builder.put("jobId", job.getUUID())
                .put("status", job.getStatus())
                .put("log", job.getLog())
                .put("title", job.getTitle())
                .put("running", job.isRunning());
        builder.put("urls", job.getUrls());
        builder.put("endMessage", job.getEndMessage());
        return builder.build();
    }
}
