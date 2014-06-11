package com.indeed.proctor.service.var;

import com.indeed.proctor.service.ExtractorSource;
import com.indeed.proctor.service.JsonVarConfig;

/**
 * Abstract class representing a context variable or identifier, which is passed with ctx. or id. prefixes in
 * query parameters.
 */
public abstract class PrefixVariable {
    private final String varName;
    private final String prefix;
    private final ExtractorSource source;
    private final String sourceKey;
    private final ValueExtractor extractor;

    public PrefixVariable(final String varName, final JsonVarConfig varConfig, final String prefix) {
        this.varName = varName;
        this.prefix = prefix;
        source = varConfig.getSource();
        // If the config didn't specify a source key, use the var name. This saves typing in the config file.
        sourceKey = (varConfig.getSourceKey() != null ? varConfig.getSourceKey() : varName);
        extractor = ExtractUtil.createValueExtractor(source, sourceKey, prefix);
    }

    public String getVarName() {
        return varName;
    }

    public String getPrefix() {
        return prefix;
    }

    public ExtractorSource getSource() {
        return source;
    }

    public String getSourceKey() {
        return sourceKey;
    }

    public ValueExtractor getExtractor() {
        return extractor;
    }
}
