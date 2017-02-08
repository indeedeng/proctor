package com.indeed.proctor.webapp.controllers;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.webapp.extensions.AfterBackgroundJobExecute;
import com.indeed.proctor.webapp.extensions.BeforeBackgroundJobExecute;
import com.indeed.proctor.webapp.model.BackgroundJobType;
import com.indeed.proctor.webapp.model.SessionViewModel;
import com.indeed.proctor.webapp.model.WebappConfiguration;
import com.indeed.proctor.webapp.views.JsonView;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.View;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author parker
 */
@Controller
@RequestMapping({ "/rpc/jobs", "/proctor/rpc/jobs" })
public class BackgroundJobRpcController {
    private static final Logger LOGGER = Logger.getLogger(BackgroundJobRpcController.class);

    private final WebappConfiguration configuration;
    private final BackgroundJobManager manager;

    @Autowired(required=false)
    private List<BeforeBackgroundJobExecute> beforeBackgroundJobExecutes = Collections.emptyList();
    @Autowired(required=false)
    private List<AfterBackgroundJobExecute> afterBackgroundJobExecutes = Collections.emptyList();

    @Autowired
    public BackgroundJobRpcController(final BackgroundJobManager manager,
                                      final WebappConfiguration configuration) {
        this.manager = manager;
        this.configuration = configuration;
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET)
    public View doGetJobStatus(@RequestParam("id") final long jobId) {
        final BackgroundJob job = manager.getJobForId(jobId);
        if(job == null) {
            final String msg = "Failed to identify job for " + jobId;
            final JsonResponse<String> err = new JsonResponse<String>(msg, false, msg);
            return new JsonView(err);
        } else {
            final Future future = job.getFuture();
            Object outcome = null;
            final long timeout = 100;
            final TimeUnit unit = TimeUnit.MILLISECONDS;
            try {
                if(future != null) {
                    outcome = future.get(timeout, unit);
                } else {
                    outcome = null;
                }
            } catch (InterruptedException exp) {
                LOGGER.warn("Interrupted during BackgroundJob.future.get(" + timeout + ", " + unit + ")");
            } catch (TimeoutException exp) {
                // Expected if Future is not complete, no need to log anything
            } catch (ExecutionException exp) {
                // bummer...
                outcome = null;
                LOGGER.error("Exception during BackgroundJob.future.get(" + timeout + ", " + unit + ")", exp);
            }

            final Map<String, Object> result = buildJobJson(job, outcome);
            final JsonResponse<Map> response = new JsonResponse<Map>(result, true, null);
            return new JsonView(response);
        }
    }

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public String doGetJobList(final Model model) {
        final List<BackgroundJob> jobs = manager.getRecentJobs();
        model.addAttribute("session",
                   SessionViewModel.builder()
                       .setUseCompiledCSS(configuration.isUseCompiledCSS())
                       .setUseCompiledJavaScript(configuration.isUseCompiledJavaScript())
                       .build());
        model.addAttribute("jobs", jobs);
        return "jobs";
    }


    @RequestMapping(value = "/cancel", method = RequestMethod.GET)
    public View doCancelJob(@RequestParam("id") final long jobId) {
        final BackgroundJob job = manager.getJobForId(jobId);
        if(job == null) {
            final String msg = "Failed to identify job for " + jobId;
            final JsonResponse<String> err = new JsonResponse<String>(msg, false, msg);
            return new JsonView(err);
        } else {
            if(job.getFuture() != null) {
                job.getFuture().cancel(true);
            }
            final Map<String, Object> result = buildJobJson(job);
            final JsonResponse<Map> response = new JsonResponse<Map>(result, true, null);
            return new JsonView(response);
        }
    }


    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public View submitTestJob(@RequestParam(value = "ms", defaultValue = "1000") final long ms) {
        final long start = System.currentTimeMillis();
        final long interval = 100; // log every 100 milliseconds
        final BackgroundJob<Boolean> job = new BackgroundJob<Boolean>() {
            @Override
            public String getTitle() {
                return "Sleeping for a total of " + ms + " ms";
            }

            @Override
            public BackgroundJobType getJobType() {
                return BackgroundJobType.JOB_TEST;
            }

            @Override
            protected List<BeforeBackgroundJobExecute> getBeforeBackgroundJobExecutes() {
                return beforeBackgroundJobExecutes;
            }

            @Override
            protected List<AfterBackgroundJobExecute> getAfterBackgroundJobExecutes() {
                return afterBackgroundJobExecutes;
            }

            @Override
            public Boolean execute() throws Exception {
                final long endms = start + ms;
                while (true) {
                    long now = System.currentTimeMillis();
                    long sleepms = Math.min(interval, endms - now);
                    if( sleepms > 0) {
                        final double elapsed_sec = (now - start) / 1000.0;
                        log(String.format("Elapsed = %.3f seconds, sleeping for %s ms", elapsed_sec, sleepms));
                        Thread.sleep(sleepms);
                    } else {
                        break;
                    }
                }
                addUrl("http://www.indeed.com", "Indeed.com");
                addUrl("http://www.google.com", "Google", "_blank");
                return Boolean.TRUE;
            }
        };
        manager.submit(job);

        final JsonResponse<Map> response = new JsonResponse<Map>(buildJobJson(job), true, null);
        return new JsonView(response);
    }

    public static Map<String,Object> buildJobJson(final BackgroundJob job) {
        return buildJobJson(job, null);
    }
    public static Map<String,Object> buildJobJson(final BackgroundJob job, final Object outcome) {
        final ImmutableMap.Builder<String,Object> builder = ImmutableMap.builder();

        builder.put("jobId", job.getId())
            .put("status", job.getStatus())
            .put("log", job.getLog())
            .put("title", job.getTitle())
            .put("running", job.isRunning());
        if(outcome != null) {
            builder.put("outcome", outcome);
        }
        builder.put("urls", job.getUrls());
        builder.put("endMessage", job.getEndMessage());
        return builder.build();
    }
}
