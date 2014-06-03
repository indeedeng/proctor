package com.indeed.proctor.service;

import com.indeed.proctor.common.Identifiers;

import java.util.List;
import java.util.Map;

/**
 * Converts a RawQueryParameters into the correct types for easy usage.
 *
 * This is mostly cleaning up parameters and types to work with determineTestBuckets.
 */
public class ConvertedParameters {
    final private Map<String, Object> context;
    final private Identifiers identifiers;
    final private List<String> test;

    public ConvertedParameters(Map<String, Object> context, Identifiers identifiers, List<String> test) {
        this.context = context;
        this.identifiers = identifiers;
        this.test = test;
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
