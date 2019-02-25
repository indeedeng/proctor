package com.indeed.proctor.webapp.extensions;

public interface DefinitionChangeLogger {
    void logMessage(final String message);

    void addUrl(final String url, final String text, final String target);

    default void addUrl(final String url, final String text) {
        addUrl(url, text, "");
    }
}
