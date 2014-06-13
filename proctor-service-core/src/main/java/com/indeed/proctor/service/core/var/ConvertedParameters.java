package com.indeed.proctor.service.core.var;

import com.indeed.proctor.common.Identifiers;

import java.util.List;
import java.util.Map;

/**
 * Converts a RawQueryParameters into the correct types for easy usage.
 *
 * This is mostly cleaning up parameters and types to work with determineTestBuckets.
 */
public class ConvertedParameters {
    private final Map<String, Object> context;
    private final Identifiers identifiers;
    private final List<String> test;
    private final Map<String, Integer> forceGroups;

    public ConvertedParameters(final Map<String, Object> context,
                               final Identifiers identifiers,
                               final List<String> test,
                               final Map<String, Integer> forceGroups) {
        this.context = context;
        this.identifiers = identifiers;
        this.test = test;
        this.forceGroups = forceGroups;
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

    public Map<String, Integer> getForceGroups() {
        return forceGroups;
    }
}
