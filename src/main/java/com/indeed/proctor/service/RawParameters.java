package com.indeed.proctor.service;

import java.util.List;
import java.util.Map;

/**
 * Stores the query parameters for a /groups/identify request.
 *
 * Parses out the special query parameters like ctx.country and id.USER and stores them in maps.
 * Also parses the comma-separated test parameter into a List.
 *
 * This does NO conversion of types. Everything stays a string just as we got it from the request
 * because at this point we still don't know the intended types of context and id variables.
 */
public class RawParameters {
    final private Map<String, String> context;
    final private Map<String, String> identifiers;
    final private List<String> test;

    public RawParameters(Map<String, String> context, Map<String, String> identifiers, List<String> test) {
        this.context = context;
        this.identifiers = identifiers;
        this.test = test;
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
}
