package com.indeed.proctor.service.var;

import com.indeed.proctor.service.JsonContextVarConfig;

/**
 * Implementation of PrefixVariable that also includes a ValueConverter for type conversion.
 */
public class ContextVariable extends PrefixVariable {
    private final String type;
    private final ValueConverter converter;

    public ContextVariable(final String varName, final JsonContextVarConfig varConfig) {
        super(varName, varConfig, "ctx");
        type = varConfig.getType();
        converter = ConvertUtil.createValueConverter(varConfig.getType());
    }

    public String getType() {
        return type;
    }

    public ValueConverter getConverter() {
        return converter;
    }
}
