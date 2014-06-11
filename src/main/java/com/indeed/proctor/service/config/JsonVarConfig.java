package com.indeed.proctor.service.config;

import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Settings for the extraction of a variable in the service config file.
 */
public class JsonVarConfig {
    private ExtractorSource source;
    private String sourceKey;

    public ExtractorSource getSource() {
        return source;
    }

    public void setSource(final ExtractorSource source) {
        this.source = source;
    }

    // This custom source key is set to null if the user did not include it in the service configuration.
    // So when re-serializing it, we shouldn't include it either if it's null.
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    public String getSourceKey() {
        return sourceKey;
    }

    public void setSourceKey(final String sourceKey) {
        this.sourceKey = sourceKey;
    }
}
