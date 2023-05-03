package com.indeed.proctor.webapp.util;

import com.indeed.util.varexport.Export;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
* @author parker
 * Common VarExports for ThreadPoolExecutor
*/
public class ThreadPoolExecutorVarExports {
    private final ThreadPoolExecutor executor;

    public ThreadPoolExecutorVarExports(final ThreadPoolExecutor executor) {
        this.executor = executor;
    }

    @Export(name = "total-task-count")
    public long getTaskCount() {
        return executor.getTaskCount();
    }

    @Export(name = "completed-task-count")
    public long getCompletedTaskCount() {
        return executor.getCompletedTaskCount();
    }

    @Export(name = "active-threads")
    public long getActiveCount() {
        return executor.getActiveCount();
    }

    @Export(name = "pool-size")
    public long getPoolSize() {
        return executor.getPoolSize();
    }

    @Export(name = "max-pool-size")
    public long getMaximumPoolSize() {
        return executor.getMaximumPoolSize();
    }

    @Export(name = "queue-remaining-capacity")
    public int getQueueRemainingCapacity() {
        return executor.getQueue().remainingCapacity();
    }

    @Export(name = "core-pool-size")
    public long getCorePoolSize() {
        return executor.getCorePoolSize();
    }

    @Export(name = "keep-alive-time")
    public String getKeepAliveTime() {
        return executor.getKeepAliveTime(TimeUnit.SECONDS) + " seconds";
    }

    @Export(name = "queue-size")
    public int getQueueSize() {
        return executor.getQueue().size();
    }

    @Export(name = "shutdown")
    public boolean isShutdown() {
        return executor.isShutdown();
    }
}
