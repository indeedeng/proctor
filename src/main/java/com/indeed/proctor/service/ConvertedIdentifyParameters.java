package com.indeed.proctor.service;

import com.google.common.collect.Maps;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.model.TestType;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Converts a RawQueryParameters into the correct types for easy usage.
 *
 * This is mostly cleaning up parameters and types to work with determineTestBuckets.
 */
public class ConvertedIdentifyParameters {
    final private Map<String, Object> context;
    final private Identifiers identifiers;
    final private List<String> test;

    public ConvertedIdentifyParameters(final RawQueryParameters raw)
    {
        // TODO: Actually convert to appropriate types
        // TODO: Tests with rules that cause a test to be skipped don't show up in output at all. Is this good?
        context = Collections.<String, Object>unmodifiableMap(raw.getContext());

        // Convert every key in the identifiers to its matching enum type.
        final Map<TestType, String> identMap = Maps.newHashMap();
        for (final Map.Entry<String, String> e : raw.getIdentifiers().entrySet())
        {
            identMap.put(TestType.valueOf(e.getKey()), e.getValue());
        }
        // TODO: what is this random parameter for and should it be true or false?
        identifiers = new Identifiers(identMap);

        test = raw.getTest();
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public Identifiers getIdentifiers() {
        return identifiers;
    }

    public List<String> getTest() {
        return test;
    }
}
