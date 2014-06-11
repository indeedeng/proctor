package com.indeed.proctor.service.var;

import com.indeed.proctor.service.ConfigurationException;
import com.indeed.proctor.service.useragents.UserAgent;

/**
 * Holds classes for all context variable conversions defined in the service configuration file.
 */
public class ConvertUtil {

    public static ValueConverter createValueConverter(final String type) {
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
        if (type.equals("UserAgent")) return new UserAgentValueConverter();

        // Unrecognized type name. You should add any custom converters here and as an implementation of ValueConverter.
        throw new ConfigurationException(
                String.format("Type '%s' unrecognized. ConvertUtil lacks a converter for this type.", type));
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
            // valueOf matches "true" (ignoring case), but we should support "1" as well.
            if (rawValue != null && rawValue.equals("1")) {
                return true;
            } else {
                return Boolean.valueOf(rawValue);
            }
        }
    }

    private static class CharacterValueConverter implements ValueConverter<Character> {
        public Character convert(String rawValue) {
            final int length = rawValue.length();
            if (length != 1) {
                // User should be passing in exactly one character in the request.
                throw new ConversionException(String.format(
                        "Parameter was length %d but expecting length 1.", length));
            } else {
                return rawValue.charAt(0);
            }
        }
    }

    private static class StringValueConverter implements ValueConverter<String> {
        public String convert(String rawValue) {
            return rawValue;
        }
    }

    private static class UserAgentValueConverter implements ValueConverter<UserAgent> {
        public UserAgent convert(String rawValue) {
            return UserAgent.parseUserAgentStringSafely(rawValue);
        }
    }
}
