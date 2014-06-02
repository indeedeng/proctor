package com.indeed.proctor.service;

import com.google.common.collect.Maps;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.model.TestType;

import java.util.List;
import java.util.Map;

/**
 * Converts variables to their appropriate final types.
 *
 * Context variables are converted according to the service configuration.
 */
public class Converter {
    private final List<VarValueUtil.ContextVariable> contextList;

    public Converter(final List<VarValueUtil.ContextVariable> contextList) {
        this.contextList = contextList;
    }

    public ConvertedParameters convert(final RawParameters raw) {
        return new ConvertedParameters(
                convertContext(raw.getContext()),
                convertIdentifiers(raw.getIdentifiers()),
                raw.getTest() // Already correct type. No processing needed.
        );
    }

    private Map<String, Object> convertContext(final Map<String, String> contextValues) {
        final Map<String, Object> converted = Maps.newHashMap();

        for (VarValueUtil.ContextVariable context : contextList) {
            final String varName = context.getVarName();
            final Object value = context.getConverter().convert(contextValues.get(varName));
            converted.put(varName, value);
        }

        return converted;
    }

    private Identifiers convertIdentifiers(final Map<String, String> identifiers) {
        // Convert every key in the identifiers to its matching enum type.
        final Map<TestType, String> identMap = Maps.newHashMap();
        for (final Map.Entry<String, String> e : identifiers.entrySet()) {
            identMap.put(TestType.valueOf(e.getKey()), e.getValue());
        }
        // TODO: what is this random parameter for and should it be true or false?
        return new Identifiers(identMap);
    }
}
