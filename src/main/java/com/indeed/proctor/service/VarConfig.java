package com.indeed.proctor.service;

/**
 * Settings for the extraction of a variable in the service config file.
 */
public class VarConfig {
    private Source source;
    private String sourceKey;

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public void setSourceKey(String sourceKey) {
        this.sourceKey = sourceKey;
    }

    public boolean usesPrefix() {
        // Only certain sources should use id. and ctx. prefixes because their arbitrary names could collide with
        // our own parameter names (like ?test=one,two for test name filters. Header sources can't collide, so they
        // shouldn't use prefixes.
        return source == Source.QUERY;
    }

    /**
     * Sets the correct source key if the JSON specified none.
     *
     * This class is automatically filled in using Jackson data binding. This class is the value of a map whose
     * key is the variable name. We want that key to be the default source key so that the user doesn't have to
     * write it twice. Unfortunately, exposing this key is impossible, so the caller should call this method
     * on every object with the key immediately after data binding.
     */
    public void correctDefaultSourceKey(String varName) {
        if (sourceKey == null) {
            // JSON did not specify a source key, so the correct default is this variable's name.
            sourceKey = varName;
        }
    }
}
