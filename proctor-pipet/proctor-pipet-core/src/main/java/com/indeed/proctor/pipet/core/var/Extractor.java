package com.indeed.proctor.pipet.core.var;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.pipet.core.config.JsonContextVarConfig;
import com.indeed.proctor.pipet.core.config.JsonVarConfig;
import com.indeed.proctor.pipet.core.web.BadRequestException;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Extracts variables from the HTTP Request according to the pipet configuration.
 *
 * <p>Parses out the special query parameters like ctx.country and id.USER and stores them in maps.
 * Also parses the comma-separated test parameter into a List and gets the forcegroups parameter.
 *
 * <p>This does NO conversion of types. Everything stays a string just as we got it from the request
 * because at this point we still don't know the intended types of context and id variables.
 */
public class Extractor {
    private static final String TEST_LIST_PARAM = "test";
    private static final String FORCE_GROUPS_PARAM = "prforceGroups";

    private static final Splitter COMMA_SPLITTER =
            Splitter.on(',').trimResults().omitEmptyStrings();

    private final List<ContextVariable> contextList;
    private final List<Identifier> identifierList;

    public Extractor(
            final List<ContextVariable> contextList, final List<Identifier> identifierList) {
        this.contextList = contextList;
        this.identifierList = identifierList;
    }

    public RawParameters extract(final HttpServletRequest request) {
        final Map<String, String> contextMap =
                extractAllVars(request, contextList, PrefixVariable::getVarName, true);
        final Map<TestType, String> identifierMap =
                extractAllVars(request, identifierList, Identifier::getTestType, false);

        checkAtLeastOneIdentifier(identifierMap);

        return new RawParameters(
                contextMap, identifierMap, extractTest(request), extractForceGroups(request));
    }

    /**
     * Checks that there is at least one identifier in the request.
     *
     * <p>Zero identifiers is only valid if all the tests are of RANDOM type. This is very rare.
     * Therefore, passing in none should be an error.
     */
    private void checkAtLeastOneIdentifier(final Map<TestType, String> identifierMap) {
        if (identifierMap.isEmpty()) {
            throw new BadRequestException("Request must have at least one identifier.");
        }
    }

    /**
     * Parse the comma-separated tests into a List.
     *
     * <p>If the test parameter appears multiple times, then only the first occurrence is used.
     *
     * <p>Return null if there was no test parameter, which means use no test filter.
     */
    private List<String> extractTest(final HttpServletRequest request) {
        // TODO: might this come from post data?
        final String testParam = request.getParameter(TEST_LIST_PARAM);
        if (testParam == null) {
            return null;
        } else {
            return Lists.newArrayList(COMMA_SPLITTER.split(testParam));
        }
    }

    private String extractForceGroups(final HttpServletRequest request) {
        // forceGroups extracted as a plain String because proctor has one method for splitting and
        // int conversion.
        // If we did the split here like we do for extractTest(), that method wouldn't work for us.
        final String param = request.getParameter(FORCE_GROUPS_PARAM);
        if (param == null) {
            // Proctor's method can't deal with null, so it's better to return empty string if no
            // force groups param.
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
     * @param mapKeyFn A function used to create key for the map returned based on the input
     *     PrefixVariable
     * @param isMissingError Whether or not a missing var constitutes an error. Identifiers are
     *     optional, so it is not an error to omit them in the request.
     * @return A mapping of var name to string var value.
     */
    private <KeyType, VariableType extends PrefixVariable> Map<KeyType, String> extractAllVars(
            final HttpServletRequest request,
            final List<VariableType> varList,
            final Function<? super VariableType, KeyType> mapKeyFn,
            final boolean isMissingError) {
        final Map<KeyType, String> ret = Maps.newHashMap();

        for (final VariableType var : varList) {
            final String varName = var.getVarName();
            final String value = var.getExtractor().extract(request);
            final String defaultValue = var.getDefaultValue();
            final KeyType mapKey = mapKeyFn.apply(var);

            if (value == null && defaultValue == null && isMissingError) {
                // This is not allowed for this type of variable, and there is no default to fall
                // back on.
                throw new BadRequestException(
                        String.format(
                                "Required variable '%s' not found by '%s'. See the pipet configuration.",
                                varName, var.getExtractor().toString()));
            } else if (value == null && defaultValue != null) {
                // We have a default to fall back on.
                ret.put(mapKey, defaultValue);
            } else if (value != null) {
                // We don't want to put nulls into our map.
                // Proctor interprets nulls correctly, but we want an accurate count of identifiers.
                ret.put(mapKey, value);
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
