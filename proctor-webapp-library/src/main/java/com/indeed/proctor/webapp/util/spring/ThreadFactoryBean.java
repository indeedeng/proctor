package com.indeed.proctor.webapp.util.spring;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.indeed.proctor.webapp.util.threads.LogOnUncaughtExceptionHandler;
import org.springframework.beans.factory.FactoryBean;

import java.util.concurrent.ThreadFactory;

/**
 */
public class ThreadFactoryBean implements FactoryBean<ThreadFactory> {
    private String nameFormat;

    @Override
    public ThreadFactory getObject() throws Exception {
        return new ThreadFactoryBuilder()
                .setNameFormat(this.nameFormat)
                .setUncaughtExceptionHandler(new LogOnUncaughtExceptionHandler())
                .build();
    }

    @Override
    public Class<?> getObjectType() {
        return ThreadFactory.class;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }

    public void setNameFormat(final String nameFormat) {
        this.nameFormat = nameFormat;
    }
}
