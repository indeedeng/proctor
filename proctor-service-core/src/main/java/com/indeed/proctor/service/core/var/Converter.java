package com.indeed.proctor.service.core.var;

import com.google.common.collect.Maps;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.consumer.ProctorConsumerUtils;
import com.indeed.proctor.service.core.web.BadRequestException;
import com.indeed.proctor.service.core.web.InternalServerException;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Converts RawQueryParameters into their correct, final types.
 *
 * Context variables are converted according to the service configuration.
 *
 * This is mostly cleaning up parameters and types to work with determineTestBuckets.
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

            if (rawValue == null) {
                // This shouldn't happen for context variables.
                // Extractor should not allow contexts to be null or should replace them with defaults.
                throw new InternalServerException(String.format(
                        "Context variable '%s' is null. This may be a server bug.", varName));
            }

            try {
                final Object value = context.getConverter().convert(rawValue);
                converted.put(varName, value);
            } catch (final ValueConversionException e) {
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
        final Collection<TestType> registered = TestType.all();
        for (final Map.Entry<String, String> e : identifiers.entrySet()) {
            final TestType testType = lookupTestType(registered, e.getKey());
            if (testType == null) {
                throw new BadRequestException(
                        String.format("Could not convert identifier '%s' to TestType", e.getKey()));
            }
            identMap.put(testType, e.getValue());
        }
        return new Identifiers(identMap, true);
    }

    /**
     * Looks up the TestType given a string representation of the test type.
     * Likely belongs in TestType proper
     * @param registered
     * @param testType
     * @return
     */
    private static TestType lookupTestType(final Collection<TestType> registered,
                                           final String testType) {
        // we cannot use registered.contains(TestType) because the TestType constructor is private
        for (final TestType type : registered) {
            if (type.name().equals(testType)) {
                return type;
            }
        }
        return null;
    }

    /**
     * forceGroups should be a mapping of test name to integer bucket value.
     */
    private Map<String, Integer> convertForceGroups(final String forceGroups) {
        // Same format as Proctor's force groups parameter.
        // The client can store this force parameter in a cookie and not worry about parsing it at all.
        // NOTE: this is technically a @VisibleForTesting method!!
        return ProctorConsumerUtils.parseForceGroupsList(forceGroups);
    }
}
