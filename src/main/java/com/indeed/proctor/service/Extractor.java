package com.indeed.proctor.service;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.indeed.proctor.service.var.ContextVariable;
import com.indeed.proctor.service.var.Identifier;
import com.indeed.proctor.service.var.PrefixVariable;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * Extracts variables from the HTTP Request according to the service configuration.
 */
public class Extractor {
    final List<ContextVariable> contextList;
    final List<Identifier> identifierList;

    public Extractor(final List<ContextVariable> contextList, final List<Identifier> identifierList) {
        this.contextList = contextList;
        this.identifierList = identifierList;
    }

    public RawParameters extract(final HttpServletRequest request) {
        return new RawParameters(
            extractAllVars(request, contextList, true),
            extractAllVars(request, identifierList, false),
            extractTest(request)
        );
    }

    /**
     * Parse the comma-separated tests into a List.
     *
     * If the test parameter appears multiple times, then only the first occurrence is used.
     */
    public List<String> extractTest(final HttpServletRequest request) {
        // TODO: might this come from post data?
        final String testParam = request.getParameter("test");
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

            if (isMissingError && value == null) {
                // This is not allowed for this type of variable.
                throw new BadRequestException(String.format(
                        "Required variable '%s' not found where expected. See the service configuration.", varName));
            } else {
                ret.put(varName, value);
            }
        }

        return ret;
    }
}
