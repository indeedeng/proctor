package com.indeed.proctor.webapp.controllers;

import com.indeed.proctor.webapp.extensions.AfterBackgroundJobExecute;
import com.indeed.proctor.webapp.extensions.BeforeBackgroundJobExecute;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class BackgroundJobExecutor extends ThreadPoolExecutor {
    private final List<BeforeBackgroundJobExecute> beforeBackgroundJobExecutes;
    private final List<AfterBackgroundJobExecute> afterBackgroundJobExecutes;

    private final Map<Runnable, BackgroundJob> backgroundJobMap = new ConcurrentHashMap<>();

    public BackgroundJobExecutor(final int corePoolSize,
                                 final int maximumPoolSize,
                                 final long keepAliveTime,
                                 final TimeUnit unit,
                                 final BlockingQueue<Runnable> workQueue,
                                 final ThreadFactory threadFactory,
                                 final RejectedExecutionHandler rejectedExecutionHandler) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, rejectedExecutionHandler,
                Collections.<BeforeBackgroundJobExecute>emptyList(), Collections.<AfterBackgroundJobExecute>emptyList());
    }

    public BackgroundJobExecutor(final int corePoolSize,
                                 final int maximumPoolSize,
                                 final long keepAliveTime,
                                 final TimeUnit unit,
                                 final BlockingQueue<Runnable> workQueue,
                                 final ThreadFactory threadFactory,
                                 final RejectedExecutionHandler rejectedExecutionHandler,
                                 final List<BeforeBackgroundJobExecute> beforeBackgroundJobExecutes,
                                 final List<AfterBackgroundJobExecute> afterBackgroundJobExecutes) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, rejectedExecutionHandler);
        this.beforeBackgroundJobExecutes = beforeBackgroundJobExecutes;
        this.afterBackgroundJobExecutes = afterBackgroundJobExecutes;
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(final Callable<T> callable) {
        final RunnableFuture<T> task = super.newTaskFor(callable);
        if (callable instanceof BackgroundJob) {
            final BackgroundJob backgroundJob = (BackgroundJob) callable;
            backgroundJobMap.put(task, backgroundJob);
        }
        return task;
    }

    @Override
    protected void beforeExecute(final Thread t, final Runnable r) {
        super.beforeExecute(t, r);
        final BackgroundJob backgroundJob = backgroundJobMap.get(r);
        if (backgroundJob != null) {
            for (final BeforeBackgroundJobExecute handler : beforeBackgroundJobExecutes) {
                handler.beforeExecute(backgroundJob);
            }
        }
    }

    @Override
    protected void afterExecute(final Runnable r, final Throwable t) {
        super.afterExecute(r, t);
        final BackgroundJob backgroundJob = backgroundJobMap.get(r);
        if (backgroundJob != null) {
            for (final AfterBackgroundJobExecute handler : afterBackgroundJobExecutes) {
                handler.afterExecute(backgroundJob, t);
            }
            backgroundJobMap.remove(r);
        }
    }
}
