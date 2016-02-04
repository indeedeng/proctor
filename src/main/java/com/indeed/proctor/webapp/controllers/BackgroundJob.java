package com.indeed.proctor.webapp.controllers;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 */
public abstract class BackgroundJob<T> implements Callable<T> {
    private Future<T> future;
    private String status = "PENDING";
    protected final StringBuilder logBuilder = new StringBuilder();

    private Long id;

    // URL to direct users to upon completion
    private List<ResultUrl> urls = Lists.newArrayList();

    private String endMessage = "";

    public void log(String message) {
        logBuilder.append(message).append("\n");
    }

    public String getLog() {
        return logBuilder.toString();
    }

    public String getStatus() {
        if (future != null) {
            if (future.isCancelled()) {
                setStatus("CANCELLED");
            } else if (future.isDone()) {
                setStatus("DONE");
            }
        }
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Future<T> getFuture() {
        return future;
    }

    public void setFuture(Future<T> future) {
        this.future = future;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public List<ResultUrl> getUrls() {
        return urls;
    }

    public void addUrl(final String url, final String text ) {
        this.addUrl(url, text, "");
    }

    public void addUrl(final String url, final String text, final String target ) {
        this.addUrl(new ResultUrl(url, text, target));
    }

    public void addUrl(final ResultUrl url) {
        this.urls.add(url);
    }

    public String getEndMessage() {
        return endMessage;
    }

    public void setEndMessage(final String endMessage) {
        this.endMessage = endMessage;
    }

    public String toString() {
        return id + ": " + status;
    }

    public boolean isRunning() {
        return future == null || (!future.isDone() && !future.isCancelled());
    }

    public abstract String getTitle();

    public static class ResultUrl {
        private String href;
        private String text;
        private String target;

        public ResultUrl(final String href,
                         final String text,
                         final String target) {
            this.href = href;
            this.text = text;
            this.target = target;
        }

        public String getHref() {
            return href;
        }

        public String getTarget() {
            return target;
        }

        public String getText() {
            return text;
        }
    }
}
