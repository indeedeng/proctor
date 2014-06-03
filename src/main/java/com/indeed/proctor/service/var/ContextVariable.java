package com.indeed.proctor.service.var;

import com.indeed.proctor.service.JsonContextVarConfig;

/**
 * Implementation of PrefixVariable that also includes a ValueConverter for type conversion.
 */
public class ContextVariable extends PrefixVariable {
    final private ConvertUtil.ValueConverter converter;

    public ContextVariable(final String varName, final JsonContextVarConfig varConfig) {
        super(varName, varConfig, "ctx");
        converter = ConvertUtil.createValueConverter(varConfig.getType());
    }

    public ConvertUtil.ValueConverter getConverter() {
        return converter;
    }
}
