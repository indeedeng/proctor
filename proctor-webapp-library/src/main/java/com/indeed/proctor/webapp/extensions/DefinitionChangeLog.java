package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.webapp.jobs.BackgroundJob.ResultUrl;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class DefinitionChangeLog {
    private final List<String> log = new ArrayList<String>();
    private final List<Error> errors = new ArrayList<Error>();
    private final List<ResultUrl> urls = new ArrayList<ResultUrl>();

    public void logMessage(final String message) {
        log.add(message);
    }

    public void addError(final Error error) {
        if (error.getMessage() != null) {
            log.add(error.getMessage());
        }
        errors.add(error);
    }

    public void addUrl(final String url, final String text ) {
        this.addUrl(url, text, "");
    }

    public void addUrl(final String url, final String text, final String target ) {
        this.addUrl(new ResultUrl(url, text, target));
    }

    public void addUrl(final ResultUrl url){
        urls.add(url);
    }

    public List<ResultUrl> getUrls() {
        if (!urls.isEmpty()) {
            return urls;
        }
        return null;
    }

    public List<String> getLog() {
        if (!log.isEmpty()) {
            return log;
        }
        return null;
    }

    public List<Error> getErrors() {
        if (!errors.isEmpty()) {
            return errors;
        }
        return null;
    }

    public boolean isErrorsFound(){
        return !errors.isEmpty();
    }
}
