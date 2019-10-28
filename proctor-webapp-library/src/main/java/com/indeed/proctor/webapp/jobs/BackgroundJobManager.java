package com.indeed.proctor.webapp.jobs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.indeed.proctor.webapp.extensions.JobInfoStore;
import com.indeed.proctor.webapp.util.ThreadPoolExecutorVarExports;
import com.indeed.proctor.webapp.util.threads.LogOnUncaughtExceptionHandler;
import com.indeed.util.varexport.VarExporter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.commons.collections4.MapUtils.synchronizedMap;

@EnableScheduling
public class BackgroundJobManager {
    private static final Logger LOGGER = Logger.getLogger(BackgroundJobManager.class);

    private final ExecutorService service;

    static final int JOB_HISTORY_MAX_SIZE = 1000;
    // synchronizing Map because put() and iteration may be called in parallel by different threads
    private final Map<UUID, BackgroundJob<?>> jobHistoryMap = synchronizedMap(
            new LinkedHashMap<UUID, BackgroundJob<?>>(JOB_HISTORY_MAX_SIZE + 1) {
                @Override
                protected boolean removeEldestEntry(final Map.Entry<UUID, BackgroundJob<?>> eldest) {
                    return this.size() > JOB_HISTORY_MAX_SIZE;
                }
            });

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
        jobHistoryMap.put(uuid, job);
        if (jobInfoStore != null) {
            jobInfoStore.updateJobInfo(uuid, job.getJobInfo());
        }
        LOGGER.info("a background job was submitted : id=" + id + " uuid=" + uuid + " title=" + job.getTitle());
    }

    /**
     * @return a list of recent background jobs at most 1000
     */
    public List<BackgroundJob<?>> getRecentJobs() {
        return getBackgroundJobs();
    }

    /**
     * @param id id of the target BackgroundJob
     * @return a background job, null if it's not found
     */
    @CheckForNull
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
        getJobHistoryEntries().stream()
                .filter(entry -> jobInfoStore.shouldUpdateJobInfo(entry.getValue()))
                .forEach(entry -> jobInfoStore.updateJobInfo(entry.getKey(), entry.getValue().getJobInfo()));

    }

    private List<BackgroundJob<?>> getBackgroundJobs() {
        final Collection<BackgroundJob<?>> values = jobHistoryMap.values();
        // must synchronize copying, see Collections.synchronizedMap() javadoc
        synchronized (jobHistoryMap) {
            // returns copy of original so that it is possible to modify the original map while consuming it.
            return ImmutableList.copyOf(values);
        }
    }

    private Set<Map.Entry<UUID, BackgroundJob<?>>> getJobHistoryEntries() {
        final Set<Map.Entry<UUID, BackgroundJob<?>>> entrySet = jobHistoryMap.entrySet();
        // must synchronize copying, see Collections.synchronizedMap() javadoc
        synchronized (jobHistoryMap) {
            // returns copy of original so that it is possible to modify the original map while consuming it.
            return ImmutableSet.copyOf(entrySet);
        }
    }
}
