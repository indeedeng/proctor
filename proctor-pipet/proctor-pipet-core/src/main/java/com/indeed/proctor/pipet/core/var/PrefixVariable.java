package com.indeed.proctor.pipet.core.var;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.indeed.proctor.pipet.core.config.ExtractorSource;

/**
 * Abstract class representing a context variable or identifier, which is passed with ctx. or id.
 * prefixes in query parameters.
 */
public abstract class PrefixVariable {
    // The name used in Proctor rule expressions and as the key in the json pipet configuration.
    private final String varName;

    // Where the variable comes from in the HTTP request (QUERY or HEADER).
    private final ExtractorSource source;

    // How the variable is referenced in the HTTP request. For example, user agents are typically
    // User-Agent.
    // This is the name we use to extract the variable from the HTTP request.
    // If the configuration did not specify a sourceKey explicitly, we default it to varName.
    // If the source is QUERY, then the prefix is prepended to the source key since query parameters
    // must use a prefix.
    private final String sourceKey;

    private final ValueExtractor extractor;

    protected PrefixVariable(
            final String varName,
            final ExtractorSource source,
            final String sourceKey,
            final ValueExtractor extractor) {
        Preconditions.checkArgument(
                !Strings.isNullOrEmpty(varName), "VarName cannot be empty or null");
        Preconditions.checkArgument(
                !Strings.isNullOrEmpty(sourceKey), "SourceKey cannot be empty or null");
        this.varName = varName;
        this.source = Preconditions.checkNotNull(source, "ExtractorSource is required");
        this.sourceKey = sourceKey;
        this.extractor = Preconditions.checkNotNull(extractor, "ValueExtractor is required");
    }

    public String getVarName() {
        return varName;
    }

    protected ExtractorSource getSource() {
        return source;
    }

    protected String getSourceKey() {
        return sourceKey;
    }

    // Returns null if object has no configured default value or it makes no sense for it to.
    public abstract String getDefaultValue();

    public ValueExtractor getExtractor() {
        return extractor;
    }

    public static class Builder<T extends PrefixVariable, B extends Builder<T, B>> {
        private String varName;

        private ExtractorSource source = ExtractorSource.QUERY;

        private String sourceKey;

        private String prefix;

        private ValueExtractor valueExtractor;

        protected Builder() {}

        public B setVarName(final String varName) {
            this.varName = varName;
            return cast();
        }

        protected String getVarName() {
            return varName;
        }

        public B setSource(final ExtractorSource source) {
            this.source = source;
            return cast();
        }

        protected ExtractorSource getSource() {
            return source;
        }

        public B setSourceKey(final String sourceKey) {
            this.sourceKey = sourceKey;
            return cast();
        }

        public B setPrefix(final String prefix) {
            this.prefix = prefix;
            return cast();
        }

        public B setValueExtractor(final ValueExtractor valueExtractor) {
            this.valueExtractor = valueExtractor;
            return cast();
        }

        protected String computeSourceKey() {
            final String defaultedSourceKey =
                    MoreObjects.firstNonNull(Strings.emptyToNull(sourceKey), varName);
            final String computedSourceKey;
            if (!Strings.isNullOrEmpty(prefix) && source == ExtractorSource.QUERY) {
                computedSourceKey = prefix + "." + defaultedSourceKey;
            } else {
                computedSourceKey = defaultedSourceKey;
            }
            return computedSourceKey;
        }

        protected ValueExtractor getOrCreateValueExtractor() {
            if (valueExtractor != null) {
                return valueExtractor;
            }
            final String computedSourceKey = computeSourceKey();
            return ValueExtractors.createValueExtractor(source, computedSourceKey);
        }

        protected B cast() {
            //noinspection unchecked
            return (B) this;
        }
    }
}
