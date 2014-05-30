package com.indeed.proctor.service;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.Service;
import java.util.List;
import java.util.Map;

/**
 * Extracts variables from the HTTP Request according to the service configuration.
 */
public class Extractor {
    private ServiceConfig config;

    public Extractor(final ServiceConfig config) {
        this.config = config;
    }

    public RawParameters extract(final HttpServletRequest request) {
        return new RawParameters(
            extractVarsSubset(request, config.getContext(), "ctx", true),
            extractVarsSubset(request, config.getIdentifiers(), "id", false),
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
     * @param varMap A mapping obtained from this.config of var name to var configuration.
     * @param prefix Prefix for this var type (ex: "id" or "ctx")
     * @param isMissingError Whether or not a missing var constitutes an error. Identifiers are optional, so it is not
     *                       an error to omit them in the request.
     * @return A mapping of var name to string var value.
     */
    private Map<String, String> extractVarsSubset(
            final HttpServletRequest request, final Map<String, ? extends VarConfig> varMap, final String prefix,
            final boolean isMissingError) {

        final Map<String, String> ret = Maps.newHashMap();

        // Run a helper function on each variable to actually get the variable.
        // Missing attributes must throw.
        // Extra variables that shouldn't be here must throw.

        for (Map.Entry<String, ? extends VarConfig> e : varMap.entrySet()) {
            final String varName = e.getKey();
            final VarConfig varConfig = e.getValue();

            final String extractedValue = extractVar(request, varConfig, prefix);
            if (isMissingError && extractedValue == null) {
                // This is not allowed.
                throw new BadRequestException(String.format(
                        "Required parameter '%s' not found where expected. See the service config", varName));
            } else {
                ret.put(varName, extractedValue);
            }
        }

        return ret;
    }

    /**
     * Extract a specific var from the request from whatever source the varConfig specifies.
     *
     * @return The string value of the var or null if it was not found.
     */
    private String extractVar(final HttpServletRequest request, final VarConfig varConfig, final String prefix) {
        // Figure out an appropriate full source key.
        String sourceKey = varConfig.getSourceKey();
        if (varConfig.usesPrefix()) {
            sourceKey = prefix + "." + sourceKey;
        }

        // Dispatches to a function that can handle the given source.
        if (varConfig.getSource() == Source.QUERY) {
            return extractQueryVar(request, sourceKey);
        } else if (varConfig.getSource() == Source.HEADER) {
            return extractHeaderVar(request, sourceKey);
        } else {
            // Exceptional: the source specified in the file doesn't exist.
            return null;
        }
    }

    private String extractQueryVar(final HttpServletRequest request, final String sourceKey) {
        return request.getParameter(sourceKey);
    }

    private String extractHeaderVar(final HttpServletRequest request, final String sourceKey) {
        return request.getHeader(sourceKey);
    }
}
