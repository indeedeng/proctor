package com.indeed.proctor.pipet.core.var;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/** @author parker */
public class TestValueConverters {

    @Test
    public void testBooleanConverter() throws ValueConversionException {
        final ValueConverter<Boolean> booleanValueConverter =
                ValueConverters.booleanValueConverter();
        for (final String trueValue : new String[] {"1", "true", "TRUE", "TrUe"}) {
            doCheckConversion(booleanValueConverter, trueValue, Boolean.TRUE);
        }

        for (final String falseValue : new String[] {"", "0", "false", "garbage"}) {
            doCheckConversion(booleanValueConverter, falseValue, Boolean.FALSE);
        }
    }

    @Test
    public void testCharConverter() throws ValueConversionException {
        final ValueConverter<Character> charConverter = ValueConverters.characterValueConverter();

        doCheckConversion(charConverter, "a", 'a');
        doCheckConversion(charConverter, "A", 'A');

        doCheckConversionWithExpectedException(charConverter, "");
        doCheckConversionWithExpectedException(charConverter, "ab");
    }

    private <E> void doCheckConversion(
            final ValueConverter<E> converter, final String rawValue, final E expected)
            throws ValueConversionException {
        final E actual = converter.convert(rawValue);
        assertEquals(
                converter.getClass().getSimpleName()
                        + " should convert '"
                        + rawValue
                        + "' to "
                        + expected,
                expected,
                actual);
    }

    private <E> void doCheckConversionWithExpectedException(
            final ValueConverter<E> converter, final String rawValue) {
        try {
            converter.convert(rawValue);
            fail(
                    converter.getClass().getSimpleName()
                            + " should throw exception when converting "
                            + rawValue);
        } catch (ValueConversionException expected) {
            // expected
        }
    }
}
