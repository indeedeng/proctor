package com.indeed.proctor.service.config;

/**
 * Context variable configuration, which also requires type conversion.
 */
public class JsonContextVarConfig extends JsonVarConfig {
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
