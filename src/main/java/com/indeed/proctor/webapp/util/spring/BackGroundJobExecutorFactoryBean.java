package com.indeed.proctor.webapp.util.spring;

import com.indeed.proctor.webapp.controllers.BackgroundJobExecutor;
import com.indeed.proctor.webapp.extensions.AfterBackgroundJobExecute;
import com.indeed.proctor.webapp.extensions.BeforeBackgroundJobExecute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class BackGroundJobExecutorFactoryBean extends ThreadPoolExecutorFactoryBean {
    @Autowired(required = false)
    List<BeforeBackgroundJobExecute> beforeBackgroundJobExecutes = Collections.emptyList();
    @Autowired(required = false)
    List<AfterBackgroundJobExecute> afterBackgroundJobExecutes = Collections.emptyList();

    @Override
    protected ThreadPoolExecutor createExecutor(final int corePoolSize,
                                                final int maxPoolSize,
                                                final int keepAliveSeconds,
                                                final BlockingQueue<Runnable> queue,
                                                final ThreadFactory threadFactory,
                                                final RejectedExecutionHandler rejectedExecutionHandler) {

        return new BackgroundJobExecutor(corePoolSize,
                maxPoolSize,
                keepAliveSeconds,
                TimeUnit.SECONDS,
                queue, threadFactory,
                rejectedExecutionHandler,
                beforeBackgroundJobExecutes,
                afterBackgroundJobExecutes);
    }
}
