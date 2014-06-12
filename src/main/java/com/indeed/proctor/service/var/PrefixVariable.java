package com.indeed.proctor.service.var;

import com.indeed.proctor.service.config.ExtractorSource;
import com.indeed.proctor.service.config.JsonVarConfig;

/**
 * Abstract class representing a context variable or identifier, which is passed with ctx. or id. prefixes in
 * query parameters.
 */
public abstract class PrefixVariable {
    // The name used in Proctor rule expressions and as the key in the json service configuration.
    private final String varName;

    // For ONLY query parameters, this string followed by a period is prefixed to all source keys for extraction.
    // This lets us differentiate configured variables vs. built-in API parameters like "test".
    private final String prefix;

    // Where the variable comes from in the HTTP request (QUERY or HEADER).
    private final ExtractorSource source;

    // How the variable is referenced in the HTTP request. For example, user agents are typically User-Agent.
    // This is the name we use to extract the variable from the HTTP request.
    // If the configuration did not specify a sourceKey explicitly, we default it to varName.
    private final String sourceKey;

    private final ValueExtractor extractor;

    public PrefixVariable(final String varName, final JsonVarConfig varConfig, final String prefix) {
        this.varName = varName;
        this.prefix = prefix;
        source = varConfig.getSource();
        // If the config didn't specify a source key, use the var name. This saves typing in the config file.
        sourceKey = (varConfig.getSourceKey() != null ? varConfig.getSourceKey() : varName);
        extractor = ValueExtractors.createValueExtractor(source, sourceKey, prefix);
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
