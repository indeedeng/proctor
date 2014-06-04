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
                raw.getTest() // Already correct type. No processing needed.
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

            } catch (final NumberFormatException e) {
                // It would be too difficult for certain primitive ValueConverters to throw ConversionException.
                // So we handle it as a separate case.
                final ConversionException convertError = new ConversionException("Number format exception");
                convertError.setVarName(varName);
                convertError.setRawValue(rawValue);
                convertError.setType(context.getType());
                throw convertError;
            } catch (final ConversionException e) {
                e.setVarName(varName);
                e.setRawValue(rawValue);
                e.setType(context.getType());
                throw e;
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
}
