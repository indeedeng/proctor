package com.indeed.proctor.service;

import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
public class RawQueryParameters {
    final private Map<String, String> context;
    final private Map<String, String> identifiers;
    final private List<String> test;

    public RawQueryParameters(final Map<String, String> queryParams) {
        context = Maps.newHashMap();
        parseDotParameters(queryParams, "ctx", context);
        identifiers = Maps.newHashMap();
        parseDotParameters(queryParams, "id", identifiers);

        // Parse the comma-separated tests into a list.
        if (queryParams.containsKey("test")) {
            test = Lists.newArrayList(Splitter.on(',').trimResults().omitEmptyStrings().split(queryParams.get("test")));
        } else {
            test = null;
        }
    }

    /**
     * Takes all the query parameters, finds the ones that start with prefix and a period, strips it out, and places
     * the key value pair into destination.
     */
    private void parseDotParameters(
            final Map<String, String> queryParams, final String prefix, final Map<String, String> destination) {

        // Regex to match beginning of input, prefix, and period.
        for (final Map.Entry<String, String> e : Maps.filterKeys(
                queryParams, Predicates.containsPattern("\\A" + prefix + "\\.")).entrySet()) {
            // Get rid of the prefix and period character, which are always at the beginning of the string.
            final String key = e.getKey().substring(prefix.length() + 1);
            destination.put(key, e.getValue());
        }
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
