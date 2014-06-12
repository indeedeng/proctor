package com.indeed.proctor.service.config;

/**
 * Context variable configuration, which also requires type conversion.
 */
public class JsonContextVarConfig extends JsonVarConfig {
    private String type;

    // If a context var is missing during extraction, we'll just use this default.
    // If null, the user did not specify a default value for this variable.
    private String defaultValue;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
}
