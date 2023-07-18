package com.indeed.proctor.pipet.core.config;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Settings for the extraction of a variable in the pipet config file. */
public class JsonVarConfig {
    private ExtractorSource source;
    private String sourceKey;

    public ExtractorSource getSource() {
        return source;
    }

    public void setSource(final ExtractorSource source) {
        this.source = source;
    }

    // This custom source key is set to null if the user did not include it in the pipet
    // configuration.
    // So when re-serializing it, we shouldn't include it either if it's null.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getSourceKey() {
        return sourceKey;
    }

    public void setSourceKey(final String sourceKey) {
        this.sourceKey = sourceKey;
    }
}
