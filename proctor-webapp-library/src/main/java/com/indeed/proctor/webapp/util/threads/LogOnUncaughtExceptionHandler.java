package com.indeed.proctor.webapp.util.threads;

import java.lang.Thread.UncaughtExceptionHandler;

import org.apache.log4j.Logger;

public class LogOnUncaughtExceptionHandler implements UncaughtExceptionHandler {
    private Logger logger = Logger.getLogger(LogOnUncaughtExceptionHandler.class);

    public LogOnUncaughtExceptionHandler(){}


    public LogOnUncaughtExceptionHandler(final Logger logger) {
        this.logger = logger;
    }

    public void setLogger(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
        logger.error("Uncaught throwable in thread " + t.getName() + "/" + t.getId(), e);
    }
}
