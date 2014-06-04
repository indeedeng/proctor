package com.indeed.proctor.service;

/**
 * Settings for the extraction of a variable in the service config file.
 */
public class JsonVarConfig {
    private Source source;
    private String sourceKey;

    public Source getSource() {
        return source;
    }

    public void setSource(final Source source) {
        this.source = source;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public void setSourceKey(final String sourceKey) {
        this.sourceKey = sourceKey;
    }
}
