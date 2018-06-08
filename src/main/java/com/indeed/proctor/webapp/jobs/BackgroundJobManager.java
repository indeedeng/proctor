package com.indeed.proctor.webapp.jobs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.indeed.proctor.webapp.extensions.JobStatusStore;
import com.indeed.proctor.webapp.util.ThreadPoolExecutorVarExports;
import com.indeed.proctor.webapp.util.threads.LogOnUncaughtExceptionHandler;
import com.indeed.util.varexport.VarExporter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

@EnableScheduling
public class BackgroundJobManager {
    private static final Logger LOGGER = Logger.getLogger(BackgroundJobManager.class);

    private final List<BackgroundJob> backgroundJobs = Lists.newLinkedList();
    private final ExecutorService service;
    private final Map<UUID, BackgroundJob> history = new MapMaker()
            .softValues()
            .makeMap();
    private final AtomicLong lastId = new AtomicLong(0);

    private JobStatusStore jobStatusStore;

    public BackgroundJobManager() {
        this(initThreadPool());
    }

    public BackgroundJobManager(final ThreadPoolExecutor executor) {
        final VarExporter exporter = VarExporter.forNamespace(getClass().getSimpleName());

        exporter.export(new ThreadPoolExecutorVarExports(executor), "pool-");
        this.service = executor;
    }

    private static ThreadPoolExecutor initThreadPool() {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(BackgroundJobManager.class.getSimpleName() + "-Thread-%d")
                .setUncaughtExceptionHandler(new LogOnUncaughtExceptionHandler())
                .build();
        return new ThreadPoolExecutor(3, 3, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), threadFactory);
    }

    @Autowired(required = false)
    public void setJobStatusStore(final JobStatusStore jobStatusStore) {
        this.jobStatusStore = jobStatusStore;
    }

    public <T> void submit(BackgroundJob<T> job) {
        final long id = lastId.incrementAndGet();
        final UUID uuid = UUID.randomUUID();
        job.setId(id);
        job.setUUID(uuid);
        Future<T> future = service.submit(job);
        job.setFuture(future);
        backgroundJobs.add(job);
        history.put(uuid, job);
        if (jobStatusStore != null) {
            jobStatusStore.updateJobStatus(uuid, getJobStatus(job));
        }
        LOGGER.info("a background job was submitted : id=" + id + " uuid=" + uuid + " title=" + job.getTitle());
    }

    public List<BackgroundJob> getRecentJobs() {
        List<BackgroundJob> recent = Lists.newArrayListWithCapacity(backgroundJobs.size());
        ListIterator<BackgroundJob> jobs = backgroundJobs.listIterator();
        while (jobs.hasNext()) {
            BackgroundJob job = jobs.next();
            recent.add(job); // inactive jobs get to be returned once...
            if (job.getFuture().isDone() || job.getFuture().isCancelled()) {
                jobs.remove();
            }
        }
        return recent;
    }

    @SuppressWarnings("unchecked")
    public <T> BackgroundJob<T> getJobForId(final UUID id) {
        return history.get(id);
    }

    @Nonnull
    public Map<String, Object> getJobStatus(final UUID jobId) {
        final BackgroundJob job = getJobForId(jobId);
        if (job != null) {
            return getJobStatus(job);
        } else if (jobStatusStore != null) {
            return jobStatusStore.getJobStatus(jobId);
        } else {
            return ImmutableMap.of();
        }
    }

    @Nonnull
    private Map<String, Object> getJobStatus(final BackgroundJob job) {
        final Future future = job.getFuture();
        Object outcome = null;
        final long timeout = 100;
        final TimeUnit unit = TimeUnit.MILLISECONDS;
        try {
            if (future != null) {
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

        final ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

        builder.put("jobId", job.getUUID())
                .put("status", job.getStatus())
                .put("log", job.getLog())
                .put("title", job.getTitle())
                .put("running", job.isRunning());
        if (outcome != null) {
            builder.put("outcome", outcome);
        }
        builder.put("urls", job.getUrls());
        builder.put("endMessage", job.getEndMessage());
        return builder.build();
    }

    // Update the job status to jobStatusStore
    @Scheduled(fixedDelay = 1000, initialDelay = 1000)
    private void scheduledCacheUpdate() {
        if (jobStatusStore == null) {
            return;
        }

        history.entrySet().stream().filter(entry -> jobStatusStore.shouldUpdateJobStatus(entry.getValue())).forEach(entry -> jobStatusStore.updateJobStatus(entry.getKey(), getJobStatus(entry.getValue())));
    }
}
