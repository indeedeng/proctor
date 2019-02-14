package com.indeed.proctor.pipet.core.var;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.Identifiers;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * The final variables used for determineTestBuckets().
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
        this.context = ImmutableMap.copyOf(context);
        this.identifiers = identifiers;
        this.test = test != null ? ImmutableList.copyOf(test) : null;
        this.forceGroups = ImmutableMap.copyOf(forceGroups);
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public Identifiers getIdentifiers() {
        return identifiers;
    }

    @Nullable
    public List<String> getTest() {
        return test;
    }

    public Map<String, Integer> getForceGroups() {
        return forceGroups;
    }
}
