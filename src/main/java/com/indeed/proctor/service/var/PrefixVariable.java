package com.indeed.proctor.service.var;

import com.indeed.proctor.service.JsonVarConfig;

/**
 * Abstract class representing a context variable or identifier, which is passed with ctx. or id. prefixes in
 * query parameters.
 */
public abstract class PrefixVariable {
    final private String varName;
    final private ExtractUtil.ValueExtractor extractor;

    public PrefixVariable(final String varName, final JsonVarConfig varConfig, final String prefix) {
        this.varName = varName;

        // If the config didn't specify a source key, use the var name. This saves typing in the config file.
        final String sourceKey = (varConfig.getSourceKey() != null ? varConfig.getSourceKey() : varName);
        extractor = ExtractUtil.createValueExtractor(varConfig.getSource(), sourceKey, prefix);
    }

    public String getVarName() {
        return varName;
    }

    public ExtractUtil.ValueExtractor getExtractor() {
        return extractor;
    }
}
