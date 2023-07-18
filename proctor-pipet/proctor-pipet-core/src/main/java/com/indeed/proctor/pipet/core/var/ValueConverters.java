package com.indeed.proctor.pipet.core.var;

import javax.annotation.Nonnull;

/** Holds classes for all context variable conversions defined in the pipet configuration file. */
public final class ValueConverters {

    private ValueConverters() {
        throw new UnsupportedOperationException("ValueConverters should not be initialized.");
    }

    public static ValueConverter<Byte> byteValueConverter() {
        return new ByteValueConverter();
    }

    public static ValueConverter<Short> shortValueConverter() {
        return new ShortValueConverter();
    }

    public static ValueConverter<Integer> integerValueConverter() {
        return new IntegerValueConverter();
    }

    public static ValueConverter<Long> longValueConverter() {
        return new LongValueConverter();
    }

    public static ValueConverter<Float> floatValueConverter() {
        return new FloatValueConverter();
    }

    public static ValueConverter<Double> doubleValueConverter() {
        return new DoubleValueConverter();
    }

    public static ValueConverter<Boolean> booleanValueConverter() {
        return new BooleanValueConverter();
    }

    public static ValueConverter<Character> characterValueConverter() {
        return new CharacterValueConverter();
    }

    public static ValueConverter<String> stringValueConverter() {
        return new StringValueConverter();
    }

    private static class ByteValueConverter implements ValueConverter<Byte> {
        public Byte convert(@Nonnull String rawValue) throws ValueConversionException {
            try {
                return Byte.valueOf(rawValue);
            } catch (NumberFormatException e) {
                throw new ValueConversionException(e.getClass().getSimpleName(), e);
            }
        }

        @Override
        public Class<Byte> getType() {
            return Byte.class;
        }
    }

    private static class ShortValueConverter implements ValueConverter<Short> {
        public Short convert(@Nonnull String rawValue) throws ValueConversionException {
            try {
                return Short.valueOf(rawValue);
            } catch (NumberFormatException e) {
                throw new ValueConversionException(e.getClass().getSimpleName(), e);
            }
        }

        @Override
        public Class<Short> getType() {
            return Short.class;
        }
    }

    private static class IntegerValueConverter implements ValueConverter<Integer> {
        public Integer convert(@Nonnull String rawValue) throws ValueConversionException {
            try {
                return Integer.valueOf(rawValue);
            } catch (NumberFormatException e) {
                throw new ValueConversionException(e.getClass().getSimpleName(), e);
            }
        }

        @Override
        public Class<Integer> getType() {
            return Integer.class;
        }
    }

    private static class LongValueConverter implements ValueConverter<Long> {
        public Long convert(@Nonnull String rawValue) throws ValueConversionException {
            try {
                return Long.valueOf(rawValue);
            } catch (NumberFormatException e) {
                throw new ValueConversionException(e.getClass().getSimpleName(), e);
            }
        }

        @Override
        public Class<Long> getType() {
            return Long.class;
        }
    }

    private static class FloatValueConverter implements ValueConverter<Float> {
        public Float convert(@Nonnull String rawValue) throws ValueConversionException {
            try {
                return Float.valueOf(rawValue);
            } catch (NumberFormatException e) {
                throw new ValueConversionException(e.getClass().getSimpleName(), e);
            }
        }

        @Override
        public Class<Float> getType() {
            return Float.class;
        }
    }

    private static class DoubleValueConverter implements ValueConverter<Double> {
        public Double convert(@Nonnull String rawValue) throws ValueConversionException {
            try {
                return Double.valueOf(rawValue);
            } catch (NumberFormatException e) {
                throw new ValueConversionException(e.getClass().getSimpleName(), e);
            }
        }

        @Override
        public Class<Double> getType() {
            return Double.class;
        }
    }

    private static class BooleanValueConverter implements ValueConverter<Boolean> {
        public Boolean convert(@Nonnull String rawValue) {
            // valueOf matches "true" (ignoring case), but we should support "1" as well.
            if (rawValue.equals("1")) {
                return true;
            } else {
                return Boolean.valueOf(rawValue);
            }
        }

        @Override
        public Class<Boolean> getType() {
            return Boolean.class;
        }
    }

    private static class CharacterValueConverter implements ValueConverter<Character> {
        public Character convert(@Nonnull String rawValue) throws ValueConversionException {
            final int length = rawValue.length();
            if (length != 1) {
                // User should be passing in exactly one character in the request.
                throw new ValueConversionException(
                        String.format("Parameter was length %d but expecting length 1.", length));
            } else {
                return rawValue.charAt(0);
            }
        }

        @Override
        public Class<Character> getType() {
            return Character.class;
        }
    }

    private static class StringValueConverter implements ValueConverter<String> {
        public String convert(@Nonnull String rawValue) {
            return rawValue;
        }

        @Override
        public Class<String> getType() {
            return String.class;
        }
    }
}
