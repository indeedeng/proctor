package com.indeed.proctor.service.var;

import com.indeed.proctor.service.ConfigurationException;
import com.indeed.proctor.service.ExtractorSource;

import javax.servlet.http.HttpServletRequest;

/**
 * Holds classes for all the different extraction sources in the ExtractorSource enum.
 */
public final class ExtractUtil {

    public static ValueExtractor createValueExtractor(final ExtractorSource source, final String sourceKey, final String prefix) {
        if (source == ExtractorSource.QUERY) {
            return new QueryValueExtractor(sourceKey, prefix);
        } else if (source == ExtractorSource.HEADER) {
            return new HeaderValueExtractor(sourceKey);
        } else {
            // This should be impossible if all enum values are in the above if statements.
            // If you add a new source, you need to add handling here and as an implementation of ValueExtractor.
            throw new ConfigurationException(
                    String.format("ExtractorSource '%s' in enum but lacks any Extractor in ExtractUtil.", source));
        }
    }

    private ExtractUtil() {
        throw new UnsupportedOperationException("ExtractUtil should not be initialized.");
    }

    private static class QueryValueExtractor implements ValueExtractor {
        final private String sourceKey;
        final private String prefix;

        public QueryValueExtractor(final String sourceKey, final String prefix) {
            this.sourceKey = sourceKey;
            this.prefix = prefix;
        }

        public String extract(final HttpServletRequest request) {
            return request.getParameter(prefix + "." + sourceKey);
        }
    }

    private static class HeaderValueExtractor implements ValueExtractor {
        final private String sourceKey;

        public HeaderValueExtractor(final String sourceKey) {
            this.sourceKey = sourceKey;
        }

        public String extract(final HttpServletRequest request) {
            return request.getHeader(sourceKey);
        }
    }
}
