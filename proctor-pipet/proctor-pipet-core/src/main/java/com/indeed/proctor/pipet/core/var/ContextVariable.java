package com.indeed.proctor.pipet.core.var;

import com.google.common.base.Preconditions;
import com.indeed.proctor.pipet.core.config.ExtractorSource;
import com.indeed.proctor.pipet.core.config.JsonContextVarConfig;

/**
 * Implementation of PrefixVariable that also includes a ValueConverter for type conversion.
 */
public class ContextVariable extends PrefixVariable {
    private final String defaultValue;
    private final ValueConverter converter;

    public ContextVariable(final String varName,
                           final ExtractorSource source,
                           final String sourceKey,
                           final ValueExtractor extractor,
                           final String defaultValue,
                           final ValueConverter converter) {
        super(varName, source, sourceKey, extractor);
        this.defaultValue = defaultValue;
        this.converter = converter;
    }

    public String getType() {
        return converter.getType().getCanonicalName();
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    public ValueConverter getConverter() {
        return converter;
    }

    public JsonContextVarConfig toContextJson() {
        final JsonContextVarConfig config = new JsonContextVarConfig();
        config.setType(getType());
        config.setDefaultValue(getDefaultValue());
        config.setSourceKey(getSourceKey());
        config.setSource(getSource());
        return config;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends PrefixVariable.Builder<ContextVariable, Builder> {

        private String defaultValue;

        private ValueConverter converter;

        public Builder() {
            setPrefix("ctx");
        }

        public Builder setDefaultValue(final String defaultValue) {
            this.defaultValue = defaultValue;
            return cast();
        }

        public Builder setConverter(final ValueConverter converter) {
            this.converter = converter;
            return cast();
        }

        public ContextVariable build() {
            Preconditions.checkNotNull(converter, "ValueConverter must be specified");
            final String varName = getVarName();
            final ExtractorSource source = getSource();
            final String sourceKey = computeSourceKey();
            final ValueExtractor extractor = getOrCreateValueExtractor();
            return new ContextVariable(
                varName,
                source,
                sourceKey,
                extractor,
                defaultValue,
                converter
            );
        }
    }
}
