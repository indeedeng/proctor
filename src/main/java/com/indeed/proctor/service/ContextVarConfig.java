package com.indeed.proctor.service;

/**
 * Context variable configuration, which also requires type conversion.
 */
public class ContextVarConfig extends VarConfig {
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
