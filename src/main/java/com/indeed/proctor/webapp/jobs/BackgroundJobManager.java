package com.indeed.proctor.webapp.jobs;

import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.indeed.proctor.webapp.util.ThreadPoolExecutorVarExports;
import com.indeed.proctor.webapp.util.threads.LogOnUncaughtExceptionHandler;
import com.indeed.util.varexport.VarExporter;
import org.apache.log4j.Logger;

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

public class BackgroundJobManager {
    private static final Logger LOGGER = Logger.getLogger(BackgroundJobManager.class);

    private final List<BackgroundJob> backgroundJobs = Lists.newLinkedList();
    private final ExecutorService service;
    private final Map<UUID, BackgroundJob> history = new MapMaker()
            .softValues()
            .makeMap();
    private final AtomicLong lastId = new AtomicLong(0);

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

    public <T> void submit(BackgroundJob<T> job) {
        final long id = lastId.incrementAndGet();
        final UUID uuid = UUID.randomUUID();
        job.setId(id);
        job.setUUID(uuid);
        Future<T> future = service.submit(job);
        job.setFuture(future);
        backgroundJobs.add(job);
        history.put(uuid, job);
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

}
