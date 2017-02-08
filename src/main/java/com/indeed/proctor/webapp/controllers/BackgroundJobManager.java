package com.indeed.proctor.webapp.controllers;


import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.indeed.util.varexport.VarExporter;
import com.indeed.proctor.webapp.util.ThreadPoolExecutorVarExports;

import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

public class BackgroundJobManager {

    private final List<BackgroundJob> backgroundJobs = Lists.newLinkedList();
    private final ExecutorService service;
    private final Map<Long, BackgroundJob> history = new MapMaker()
            .softValues()
            .makeMap();
    private final AtomicLong lastId = new AtomicLong(0);

    public BackgroundJobManager(final ThreadPoolExecutor executor) {
        final VarExporter exporter = VarExporter.forNamespace(getClass().getSimpleName());
        exporter.export(new ThreadPoolExecutorVarExports(executor), "pool-");
        this.service = executor;
    }

    public <T> void submit(BackgroundJob<T> job) {
        long id = lastId.incrementAndGet();
        job.setId(id);
        Future<T> future = service.submit(job);
        job.setFuture(future);
        backgroundJobs.add(job);
        history.put(id, job);
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
    public <T> BackgroundJob<T> getJobForId(long id) {
        return history.get(id);
    }

}
