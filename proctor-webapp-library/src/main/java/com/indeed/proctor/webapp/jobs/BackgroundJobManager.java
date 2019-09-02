package com.indeed.proctor.webapp.jobs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.indeed.proctor.webapp.extensions.JobInfoStore;
import com.indeed.proctor.webapp.util.ThreadPoolExecutorVarExports;
import com.indeed.proctor.webapp.util.threads.LogOnUncaughtExceptionHandler;
import com.indeed.util.varexport.VarExporter;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.Nullable;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.commons.collections.MapUtils.synchronizedMap;

@EnableScheduling
public class BackgroundJobManager {
    private static final Logger LOGGER = Logger.getLogger(BackgroundJobManager.class);

    private final List<BackgroundJob<?>> backgroundJobs = Lists.newLinkedList();
    private final ExecutorService service;

    static final int JOB_HISTORY_MAX_SIZE = 1000;
    // synchronizing Map because put() and entrySet() may be called in parallel by different threads
    private final Map<UUID, BackgroundJob<?>> jobHistoryMap = synchronizedMap(new LRUMap<>(JOB_HISTORY_MAX_SIZE));

    private final AtomicLong lastId = new AtomicLong(0);

    private JobInfoStore jobInfoStore;

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
        return new ThreadPoolExecutor(3, 3, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), threadFactory);
    }

    @Autowired(required = false)
    public void setJobInfoStore(final JobInfoStore jobInfoStore) {
        this.jobInfoStore = jobInfoStore;
    }

    public <T> void submit(final BackgroundJob<T> job) {
        final long id = lastId.incrementAndGet();
        final UUID uuid = UUID.randomUUID();
        job.setId(id);
        job.setUUID(uuid);
        final Future<T> future = service.submit(job);
        job.setFuture(future);
        backgroundJobs.add(job);
        jobHistoryMap.put(uuid, job);
        if (jobInfoStore != null) {
            jobInfoStore.updateJobInfo(uuid, job.getJobInfo());
        }
        LOGGER.info("a background job was submitted : id=" + id + " uuid=" + uuid + " title=" + job.getTitle());
    }

    public List<BackgroundJob<?>> getRecentJobs() {
        final List<BackgroundJob<?>> recent = Lists.newArrayListWithCapacity(backgroundJobs.size());
        final ListIterator<BackgroundJob<?>> jobs = backgroundJobs.listIterator();
        while (jobs.hasNext()) {
            final BackgroundJob<?> job = jobs.next();
            recent.add(job); // inactive jobs get to be returned once...
            if (job.getFuture().isDone() || job.getFuture().isCancelled()) {
                jobs.remove();
            }
        }
        return recent;
    }

    public BackgroundJob<?> getJobForId(final UUID id) {
        return jobHistoryMap.get(id);
    }

    @Nullable
    public BackgroundJob.JobInfo getJobInfo(final UUID jobId) {
        final BackgroundJob<?> job = getJobForId(jobId);
        if (job != null) {
            return job.getJobInfo();
        } else if (jobInfoStore != null) {
            return jobInfoStore.getJobInfo(jobId);
        } else {
            return null;
        }
    }

    // Update the job status to jobInfoStore
    @Scheduled(fixedDelay = 1000, initialDelay = 1000)
    @VisibleForTesting
    void scheduledCacheUpdate() {
        if (jobInfoStore == null) {
            return;
        }

        jobHistoryMap.entrySet().stream()
                .filter(entry -> jobInfoStore.shouldUpdateJobInfo(entry.getValue()))
                .forEach(entry -> jobInfoStore.updateJobInfo(entry.getKey(), entry.getValue().getJobInfo()));
    }
}
