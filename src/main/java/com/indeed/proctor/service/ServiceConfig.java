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

    public void correctDefaultSourceKeys() {
        for (Map.Entry<String, ? extends VarConfig> e : context.entrySet()) {
            e.getValue().correctDefaultSourceKey(e.getKey());
        }
        for (Map.Entry<String, ? extends VarConfig> e : identifiers.entrySet()) {
            e.getValue().correctDefaultSourceKey(e.getKey());
        }
    }
}
