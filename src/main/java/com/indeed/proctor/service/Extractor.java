package com.indeed.proctor.service;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.indeed.proctor.service.var.ContextVariable;
import com.indeed.proctor.service.var.Identifier;
import com.indeed.proctor.service.var.PrefixVariable;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Extracts variables from the HTTP Request according to the service configuration.
 */
public class Extractor {
    private final String TEST_LIST_PARAM = "test";

    private final List<ContextVariable> contextList;
    private final List<Identifier> identifierList;

    public Extractor(final List<ContextVariable> contextList, final List<Identifier> identifierList) {
        this.contextList = contextList;
        this.identifierList = identifierList;
    }

    public RawParameters extract(final HttpServletRequest request) {
        checkForUnrecognizedParameters(request.getParameterNames());

        final Map<String, String> contextMap = extractAllVars(request, contextList, true);
        final Map<String, String> identifierMap = extractAllVars(request, identifierList, false);

        checkAtLeastOneIdentifier(identifierMap);

        return new RawParameters(
            contextMap,
            identifierMap,
            extractTest(request)
        );
    }

    /**
     * Checks that all the parameters in our request are valid. If the user passed in something we don't recognize,
     * we throw because they made an error in their request.
     */
    private void checkForUnrecognizedParameters(final Enumeration<String> paramNames) {
        final Set<String> paramSet = new HashSet<String>(Collections.list(paramNames));

        // Iterate through all possible parameters and remove them from the set.
        // It doesn't matter if we remove optional or non-existent parameters. remove() returns false in that case.

        paramSet.remove(TEST_LIST_PARAM);

        Iterator<PrefixVariable> iter = Iterators.concat(contextList.iterator(), identifierList.iterator());
        while (iter.hasNext()) {
            PrefixVariable var = iter.next();
            if (var.getSource() == Source.QUERY) {
                paramSet.remove(var.getPrefix() + "." + var.getSourceKey());
                // If the parameter doesn't exist, then an error will be thrown during extraction later on.
            }
        }

        // All that remains in paramSet are query parameters that our config had no knowledge of.
        if (!paramSet.isEmpty()) {
            throw new BadRequestException(String.format(
                    "Unrecognized query parameters: %s", Joiner.on(", ").join(paramSet))
            );
        }
    }

    /**
     * Checks that there is at least one identifier in the request.
     *
     * It's very rare for any test groups to be returned if no identifiers were passed in at all.
     * Therefore, passing in none should be an error.
     */
    private void checkAtLeastOneIdentifier(final Map<String, String> identifierMap) {
        if (identifierMap.isEmpty()) {
            throw new BadRequestException("Request must have at least one identifier.");
        }
    }

    /**
     * Parse the comma-separated tests into a List.
     *
     * If the test parameter appears multiple times, then only the first occurrence is used.
     */
    public List<String> extractTest(final HttpServletRequest request) {
        // TODO: might this come from post data?
        final String testParam = request.getParameter(TEST_LIST_PARAM);
        if (testParam == null) {
            return null;
        } else {
            return Lists.newArrayList(Splitter.on(',').trimResults().omitEmptyStrings().split(testParam));
        }
    }

    /**
     * Extracts vars from either the context vars or the identifiers according to a configuration.
     *
     * @param request The HTTP request.
     * @param varList The list of variables to process.
     * @param isMissingError Whether or not a missing var constitutes an error. Identifiers are optional, so it is not
     *                       an error to omit them in the request.
     * @return A mapping of var name to string var value.
     */
    private Map<String, String> extractAllVars(
            final HttpServletRequest request, final List<? extends PrefixVariable> varList, boolean isMissingError) {

        final Map<String, String> ret = Maps.newHashMap();

        for (PrefixVariable var : varList) {
            final String varName = var.getVarName();
            final String value = var.getExtractor().extract(request);

            if (value == null && isMissingError) {
                // This is not allowed for this type of variable.
                throw new BadRequestException(String.format(
                        "Required variable '%s' not found where expected. See the service configuration.", varName));
            } else if (value != null) {
                // We don't want to put nulls into our map.
                // Proctor interprets nulls correctly, but we want an accurate count of identifiers.
                ret.put(varName, value);
            }
        }

        return ret;
    }
}
