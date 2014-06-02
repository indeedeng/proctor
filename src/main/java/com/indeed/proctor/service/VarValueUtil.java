package com.indeed.proctor.service;

import javax.servlet.http.HttpServletRequest;

/**
 * Holds multiple classes related to the extraction and conversion of individual variable values.
 */
public class VarValueUtil {

    public static class ContextVariable extends PrefixVariable {
        final private ValueConverter converter;

        public ContextVariable(final String varName, final JsonContextVarConfig varConfig) {
            super(varName, varConfig, "ctx");
            converter = createValueConverter(varConfig.getType());
        }

        public ValueConverter getConverter() {
            return converter;
        }
    }

    public static class Identifier extends PrefixVariable {
        public Identifier(final String varName, final JsonVarConfig varConfig) {
            super(varName, varConfig, "id");
        }
    }

    /**
     * Used for functionality common to both context and identifier as they both use prefixes in query params.
     */
    public static abstract class PrefixVariable {
        final private String varName;
        final private ValueExtractor extractor;

        public PrefixVariable(final String varName, final JsonVarConfig varConfig, final String prefix) {
            this.varName = varName;

            // If the config didn't specify a source key, use the var name. This saves typing in the config file.
            final String sourceKey = (varConfig.getSourceKey() != null ? varConfig.getSourceKey() : varName);
            extractor = createValueExtractor(varConfig.getSource(), sourceKey, prefix);
        }

        public String getVarName() {
            return varName;
        }

        public ValueExtractor getExtractor() {
            return extractor;
        }
    }

    private static ValueExtractor createValueExtractor(final Source source, final String sourceKey, final String prefix) {
        if (source == Source.QUERY) {
            return new QueryValueExtractor(sourceKey, prefix);
        } else if (source == Source.HEADER) {
            return new HeaderValueExtractor(sourceKey);
        } else {
            // This should be impossible if all enum values are in the above if statements.
            // If you add a new source, you need to add handling here and as an implementation of ValueExtractor.
            return null;
        }
    }

    public static interface ValueExtractor {
        public String extract(final HttpServletRequest request);
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

    private static ValueConverter createValueConverter(final String type) {
        // Primitives
        if (type.equals("byte") || type.equals("Byte")) return new ByteValueConverter();
        if (type.equals("short") || type.equals("Short")) return new ShortValueConverter();
        if (type.equals("int") || type.equals("Integer")) return new IntegerValueConverter();
        if (type.equals("long") || type.equals("Long")) return new LongValueConverter();
        if (type.equals("float") || type.equals("Float")) return new FloatValueConverter();
        if (type.equals("double") || type.equals("Double")) return new DoubleValueConverter();
        if (type.equals("boolean") || type.equals("Boolean")) return new BooleanValueConverter();
        if (type.equals("char") || type.equals("Character")) return new CharacterValueConverter();

        if (type.equals("String")) return new StringValueConverter();

        // Custom types


        // Unrecognized type name. You should add any custom converters here and as an implementation of ValueConverter.
        return new StringValueConverter();
    }

    public static interface ValueConverter<T> {
        public T convert(String rawValue);
    }

    private static class ByteValueConverter implements ValueConverter<Byte> {
        public Byte convert(String rawValue) {
            return Byte.valueOf(rawValue);
        }
    }

    private static class ShortValueConverter implements ValueConverter<Short> {
        public Short convert(String rawValue) {
            return Short.valueOf(rawValue);
        }
    }

    private static class IntegerValueConverter implements ValueConverter<Integer> {
        public Integer convert(String rawValue) {
            return Integer.valueOf(rawValue);
        }
    }

    private static class LongValueConverter implements ValueConverter<Long> {
        public Long convert(String rawValue) {
            return Long.valueOf(rawValue);
        }
    }

    private static class FloatValueConverter implements ValueConverter<Float> {
        public Float convert(String rawValue) {
            return Float.valueOf(rawValue);
        }
    }

    private static class DoubleValueConverter implements ValueConverter<Double> {
        public Double convert(String rawValue) {
            return Double.valueOf(rawValue);
        }
    }

    private static class BooleanValueConverter implements ValueConverter<Boolean> {
        public Boolean convert(String rawValue) {
            return Boolean.valueOf(rawValue);
        }
    }

    private static class CharacterValueConverter implements ValueConverter<Character> {
        public Character convert(String rawValue) {
            return rawValue.charAt(0);
        }
    }

    private static class StringValueConverter implements ValueConverter<String> {
        public String convert(String rawValue) {
            return rawValue;
        }
    }
}
