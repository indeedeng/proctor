package com.indeed.proctor.service.core.var;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterators;
import com.indeed.proctor.service.core.config.ConfigurationException;
import com.indeed.proctor.service.core.config.ExtractorSource;

import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

/**
 * Holds classes for all the different extraction sources in the ExtractorSource enum.
 */
public final class ValueExtractors {

    private ValueExtractors() {
        throw new UnsupportedOperationException("ValueExtractors should not be initialized.");
    }

    public static ValueExtractor createValueExtractor(final ExtractorSource source,
                                                      final String sourceKey) {
        Preconditions.checkNotNull(source, "ExtractorSource must be provided");
        if (source == ExtractorSource.QUERY) {
            return fromQueryParameter(sourceKey);
        } else if (source == ExtractorSource.HEADER) {
            return fromHttpHeader(sourceKey);
        } else {
            // This should be impossible if all enum values are in the above if statements.
            // If you add a new source, you need to add handling here and as an implementation of ValueExtractor.
            throw new ConfigurationException(
                    String.format("ExtractorSource '%s' in enum but lacks any extractor in ValueExtractors.", source));
        }
    }

    public static ValueExtractor fromQueryParameter(final String parameter) {
        return new QueryValueExtractor(parameter);
    }

    public static ValueExtractor fromHttpHeader(final String headerName) {
        return new HeaderValueExtractor(headerName);
    }

    public static ValueExtractor chain(final List<ValueExtractor> extractors) {
        return chain(extractors.toArray(new ValueExtractor[extractors.size()]));
    }

    public static ValueExtractor chain(final ValueExtractor... extractors) {
        Preconditions.checkArgument(extractors.length > 0, "Chained value extractors must be greater than zero");
        if (extractors.length == 1) {
            return extractors[0];
        } else {
            return new ChainedValueExtractor(extractors);
        }
    }

    private static class ChainedValueExtractor implements ValueExtractor {
        private final ValueExtractor[] extractors;

        private ChainedValueExtractor(final ValueExtractor[] extractors) {
            for( ValueExtractor extractor : extractors) {
                Preconditions.checkNotNull(extractor, "each of the chained ValueExtractors should be non-null");
            }
            this.extractors = extractors;
        }

        @Override
        public String extract(final HttpServletRequest request) {
            String result = null;
            for (final ValueExtractor extractor : extractors) {
                result = extractor.extract(request);
                if (result != null) {
                    break;
                }
            }
            return result;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("ChainedValueExtractor{");
            sb.append("chained(");
            final Iterator<ValueExtractor> iter = Iterators.forArray(extractors);
            while(iter.hasNext()) {
                final ValueExtractor extractor = iter.next();
                sb.append(extractor);
                if (iter.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append(')');
            return sb.toString();
        }
    }

    private static class QueryValueExtractor implements ValueExtractor {
        final private String sourceKey;

        public QueryValueExtractor(final String sourceKey) {
            Preconditions.checkNotNull(Strings.emptyToNull(sourceKey), "Query Parameter must not be empty");
            // Store the concatenation here so that we don't have to do it every call to extract().
            this.sourceKey = sourceKey;
        }

        public String extract(final HttpServletRequest request) {
            return request.getParameter(sourceKey);
        }

        @Override
        public String toString() {
            return sourceKey + ":" + sourceKey;
        }
    }

    private static class HeaderValueExtractor implements ValueExtractor {
        final private String sourceKey;

        public HeaderValueExtractor(final String sourceKey) {
            Preconditions.checkNotNull(Strings.emptyToNull(sourceKey), "Header Name must not be empty");
            this.sourceKey = sourceKey;
        }

        public String extract(final HttpServletRequest request) {
            return request.getHeader(sourceKey);
        }

        @Override
        public String toString() {
            return sourceKey + ":" + sourceKey;
        }
    }
}
