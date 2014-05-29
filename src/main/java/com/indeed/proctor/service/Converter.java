package com.indeed.proctor.service;

import com.google.common.collect.Maps;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.model.TestType;

import java.util.Map;

/**
 * Converts variables to their appropriate final types.
 *
 * Context variables are converted according to the service configuration.
 */
public class Converter {
    private final ServiceConfig config;

    public Converter(final ServiceConfig config) {
        this.config = config;
    }

    public ConvertedParameters convert(final RawParameters raw) {
        return new ConvertedParameters(
                convertContext(raw.getContext()),
                convertIdentifiers(raw.getIdentifiers()),
                raw.getTest() // Already correct type. No processing needed.
        );
    }

    private Map<String, Object> convertContext(final Map<String, String> context) {
        final Map<String, Object> converted = Maps.newHashMap();
        final Map<String, ContextVarConfig> contextConfigMap = config.getContext();

        for (Map.Entry<String, String> e : context.entrySet()) {
            final String varName = e.getKey();
            final String varValue = e.getValue();

            converted.put(varName, convertType(varValue, contextConfigMap.get(varName).getType()));
        }

        return converted;
    }

    private Object convertType(final String val, final String type) {
        // Primitives
        if (type.equals("byte") || type.equals("Byte")) return Byte.parseByte(val);
        if (type.equals("short") || type.equals("Short")) return Short.parseShort(val);
        if (type.equals("int") || type.equals("Integer")) return Integer.parseInt(val);
        if (type.equals("long") || type.equals("Long")) return Long.parseLong(val);
        if (type.equals("float") || type.equals("Float")) return Float.parseFloat(val);
        if (type.equals("double") || type.equals("Double")) return Double.parseDouble(val);
        if (type.equals("boolean") || type.equals("Boolean")) return Boolean.parseBoolean(val);
        if (type.equals("char") || type.equals("Character")) return val.charAt(0);

        if (type.equals("String")) return val;

        // Then it must be a custom type.
        return val;
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
