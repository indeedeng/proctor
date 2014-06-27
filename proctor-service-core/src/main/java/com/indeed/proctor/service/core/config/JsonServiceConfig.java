package com.indeed.proctor.service.core.config;

import java.util.Collections;
import java.util.Map;

/**
 * JSON configuration file that describes the extraction of context variables and identifiers and also the
 * conversion type of context variables.
 */
public class JsonServiceConfig {
    private Map<String, JsonContextVarConfig> context = Collections.emptyMap();
    private Map<String, JsonVarConfig> identifiers = Collections.emptyMap();

    public Map<String, JsonContextVarConfig> getContext() {
        return context;
    }

    public void setContext(final Map<String, JsonContextVarConfig> context) {
        this.context = context;
    }

    public Map<String, JsonVarConfig> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(final Map<String, JsonVarConfig> identifiers) {
        this.identifiers = identifiers;
    }
}
