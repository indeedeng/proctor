package com.indeed.proctor.service;

import com.google.common.collect.Maps;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.service.var.ContextVariable;
import com.indeed.proctor.service.var.ConversionException;

import java.util.List;
import java.util.Map;

/**
 * Converts variables to their appropriate final types.
 *
 * Context variables are converted according to the service configuration.
 */
public class Converter {
    private final List<ContextVariable> contextList;

    public Converter(final List<ContextVariable> contextList) {
        this.contextList = contextList;
    }

    public ConvertedParameters convert(final RawParameters raw) {
        return new ConvertedParameters(
                convertContext(raw.getContext()),
                convertIdentifiers(raw.getIdentifiers()),
                raw.getTest(), // Already correct type. No processing needed.
                convertForceGroups(raw.getForceGroups())
        );
    }

    private Map<String, Object> convertContext(final Map<String, String> contextValues) {
        final Map<String, Object> converted = Maps.newHashMap();

        for (ContextVariable context : contextList) {
            final String varName = context.getVarName();
            final String rawValue = contextValues.get(varName);

            try {
                final Object value = context.getConverter().convert(rawValue);
                converted.put(varName, value);
            } catch (final ConversionException e) {
                // When debugging, users are likely to get conversion errors due to typos or misunderstandings.
                // Include as much information as possible so they can figure out what they did wrong.
                throw new BadRequestException(
                        String.format("Could not convert raw value '%s' to type '%s' for context variable '%s': %s",
                                rawValue, context.getType(), varName, e.getMessage()));
            }
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

    /**
     * forceGroups should be a mapping of test name to integer bucket value.
     */
    private Map<String, Integer> convertForceGroups(final Map<String, String> forceGroups) {
        final Map<String, Integer> ret = Maps.newHashMap();
        for (Map.Entry<String, String> entry : forceGroups.entrySet()) {
            try {
                ret.put(entry.getKey(), Integer.valueOf(entry.getValue()));
            } catch (NumberFormatException e) {
                throw new BadRequestException(String.format(
                        "Could not convert force groups parameter '%s' with value '%s' into an Integer. " +
                        "Force parameters must be integer bucket values.", entry.getKey(), entry.getValue()));
            }
        }
        return ret;
    }
}
