package com.indeed.proctor.webapp.controllers;

import com.indeed.proctor.webapp.extensions.AfterBackgroundJobExecute;
import com.indeed.proctor.webapp.extensions.BeforeBackgroundJobExecute;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

public class BackgroundJobFactory {
    @Autowired(required=false)
    private List<BeforeBackgroundJobExecute> beforeBackgroundJobExecutes = Collections.emptyList();
    @Autowired(required=false)
    private List<AfterBackgroundJobExecute> afterBackgroundJobExecutes = Collections.emptyList();

    public interface Executor<T> {
        T execute(BackgroundJob job) throws Exception;
    }

    public <T> BackgroundJob<T> createBackgroundJob(final String jobTitle, final BackgroundJob.JobType jobType, final Executor<T> executor) {
        return new BackgroundJob<T>() {
            @Override
            public String getTitle() {
                return jobTitle;
            }

            @Override
            public JobType getJobType() {
                return jobType;
            }

            @Override
            protected T execute() throws Exception {
                return executor.execute(this);
            }

            @Override
            protected List<BeforeBackgroundJobExecute> getBeforeBackgroundJobExecutes() {
                return beforeBackgroundJobExecutes;
            }

            @Override
            protected List<AfterBackgroundJobExecute> getAfterBackgroundJobExecutes() {
                return afterBackgroundJobExecutes;
            }
        };
    }
}
