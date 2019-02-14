package com.indeed.proctor.pipet.core.var;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.model.TestType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Stores the raw query parameters for a /groups/identify request.
 */
public class RawParameters {
    private final Map<String, String> context;
    private final Map<TestType, String> identifiers;
    private final List<String> test;
    private final String forceGroups;

    public RawParameters(final Map<String, String> context,
                         final Map<TestType, String> identifiers,
                         final List<String> test,
                         final String forceGroups) {
        this.context = ImmutableMap.copyOf(context);
        this.identifiers = ImmutableMap.copyOf(identifiers);
        this.test = test != null ? ImmutableList.copyOf(test) : null;
        this.forceGroups = forceGroups;
    }

    public Map<String, String> getContext() {
        return context;
    }

    public Map<TestType, String> getIdentifiers() {
        return identifiers;
    }

    /**
     * Returns a list of test names to filter by.
     *
     * Returns an empty list if the query parameter was present but empty (ex: "?test=")
     * Returns null if the query parameter was absent.
     */
    @Nullable
    public List<String> getTest() {
        return test;
    }

    public String getForceGroups() {
        return forceGroups;
    }
}
