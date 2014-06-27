package com.indeed.proctor.service.core.var;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.indeed.proctor.service.core.config.ExtractorSource;
import com.indeed.proctor.service.core.config.JsonContextVarConfig;
import com.indeed.proctor.service.core.config.JsonVarConfig;
import com.indeed.proctor.service.core.web.BadRequestException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/**
 * Extracts variables from the HTTP Request according to the service configuration.
 *
 * Parses out the special query parameters like ctx.country and id.USER and stores them in maps.
 * Also parses the comma-separated test parameter into a List and gets the forcegroups parameter.
 *
 * This does NO conversion of types. Everything stays a string just as we got it from the request
 * because at this point we still don't know the intended types of context and id variables.
 */
public class Extractor {
    private static final String TEST_LIST_PARAM = "test";
    private static final String FORCE_GROUPS_PARAM = "prforceGroups";
    // List of all valid API parameters. This is everything the API uses without the user explicitly configuring.
    private static final Collection<String> API_QUERY_PARAMS =
            Arrays.asList(TEST_LIST_PARAM, FORCE_GROUPS_PARAM);

    private static final Splitter COMMA_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

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
            extractTest(request),
            extractForceGroups(request)
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

        paramSet.removeAll(API_QUERY_PARAMS);

        Iterator<PrefixVariable> iter = Iterators.concat(contextList.iterator(), identifierList.iterator());
        while (iter.hasNext()) {
            PrefixVariable var = iter.next();
            if (var.getSource() == ExtractorSource.QUERY) {
                paramSet.remove(var.getSourceKey());
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
     * Zero identifiers is only valid if all the tests are of RANDOM type. This is very rare.
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
    private List<String> extractTest(final HttpServletRequest request) {
        // TODO: might this come from post data?
        final String testParam = request.getParameter(TEST_LIST_PARAM);
        if (testParam == null) {
            return Collections.emptyList();
        } else {
            return Lists.newArrayList(COMMA_SPLITTER.split(testParam));
        }
    }

    private String extractForceGroups(final HttpServletRequest request) {
        // forceGroups extracted as a plain String because proctor has one method for splitting and int conversion.
        // If we did the split here like we do for extractTest(), that method wouldn't work for us.
        final String param = request.getParameter(FORCE_GROUPS_PARAM);
        if (param == null) {
            // Proctor's method can't deal with null, so it's better to return empty string if no force groups param.
            return "";
        } else {
            return param;
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
    private Map<String, String> extractAllVars(final HttpServletRequest request,
                                               final List<? extends PrefixVariable> varList,
                                               boolean isMissingError) {
        final Map<String, String> ret = Maps.newHashMap();

        for (PrefixVariable var : varList) {
            final String varName = var.getVarName();
            final String value = var.getExtractor().extract(request);
            final String defaultValue = var.getDefaultValue();

            if (value == null && defaultValue == null && isMissingError) {
                // This is not allowed for this type of variable, and there is no default to fall back on.
                throw new BadRequestException(String.format(
                        "Required variable '%s' not found by '%s'. See the service configuration.",
                        varName, var.getExtractor().toString()));
            } else if (value == null && defaultValue != null) {
                // We have a default to fall back on.
                ret.put(varName, defaultValue);
            } else if (value != null) {
                // We don't want to put nulls into our map.
                // Proctor interprets nulls correctly, but we want an accurate count of identifiers.
                ret.put(varName, value);
            }
        }

        return ret;
    }

    public final Map<String, JsonContextVarConfig> toContextJson() {
        final Map<String, JsonContextVarConfig> context = Maps.newHashMap();
        for (final ContextVariable variable : this.contextList) {
            context.put(variable.getVarName(), variable.toContextJson());
        }
        return context;
    }

    public final Map<String, JsonVarConfig> toIdentifierJson() {
        final Map<String, JsonVarConfig> identifiers = Maps.newHashMap();
        for (final Identifier identifier : this.identifierList) {
            identifiers.put(identifier.getVarName(), identifier.toIdentifierJson());
        }
        return identifiers;
    }

}
