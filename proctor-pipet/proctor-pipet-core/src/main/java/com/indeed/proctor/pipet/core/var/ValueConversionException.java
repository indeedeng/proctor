package com.indeed.proctor.pipet.core.var;

/**
 * Thrown when a converter has some error during conversion that is unrecoverable.
 */
public class ValueConversionException extends Exception {
    public ValueConversionException(final String message) {
        super(message);
    }

    public ValueConversionException(final String s, final Throwable throwable) {
        super(s, throwable);
    }
}
