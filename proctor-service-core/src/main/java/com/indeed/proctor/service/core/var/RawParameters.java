package com.indeed.proctor.service.core.var;

import java.util.List;
import java.util.Map;

/**
 * Stores the raw query parameters for a /groups/identify request.
 */
public class RawParameters {
    private final Map<String, String> context;
    private final Map<String, String> identifiers;
    private final List<String> test;
    private final String forceGroups;

    public RawParameters(final Map<String, String> context,
                         final Map<String, String> identifiers,
                         final List<String> test,
                         final String forceGroups) {
        this.context = context;
        this.identifiers = identifiers;
        this.test = test;
        this.forceGroups = forceGroups;
    }

    public Map<String, String> getContext() {
        return context;
    }

    public Map<String, String> getIdentifiers() {
        return identifiers;
    }

    /**
     * Returns a list of test names to filter by.
     *
     * Returns an empty list if the query parameter was present but empty (ex: "?test=")
     * Returns null if the query parameter was absent.
     */
    public List<String> getTest() {
        return test;
    }

    public String getForceGroups() {
        return forceGroups;
    }
}
