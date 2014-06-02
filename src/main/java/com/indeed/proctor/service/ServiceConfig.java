package com.indeed.proctor.service;

import java.util.Map;

/**
 * JSON configuration file that describes the extraction of context variables and identifiers and also the
 * conversion type of context variables.
 */
public class ServiceConfig {
    private Map<String, ContextVarConfig> context;
    private Map<String, VarConfig> identifiers;

    public Map<String, ContextVarConfig> getContext() {
        return context;
    }

    public void setContext(Map<String, ContextVarConfig> context) {
        this.context = context;
    }

    public Map<String, VarConfig> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(Map<String, VarConfig> identifiers) {
        this.identifiers = identifiers;
    }
}
