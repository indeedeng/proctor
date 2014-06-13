package com.indeed.proctor.service.core.var;

import com.indeed.proctor.service.core.config.JsonContextVarConfig;

/**
 * Implementation of PrefixVariable that also includes a ValueConverter for type conversion.
 */
public class ContextVariable extends PrefixVariable {
    private final String type;
    private final String defaultValue;
    private final ValueConverter converter;

    public ContextVariable(final String varName, final JsonContextVarConfig varConfig) {
        super(varName, varConfig, "ctx");
        type = varConfig.getType();
        defaultValue = varConfig.getDefaultValue();
        converter = ValueConverters.createValueConverter(varConfig.getType());
    }

    public String getType() {
        return type;
    }

    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    public ValueConverter getConverter() {
        return converter;
    }
}
