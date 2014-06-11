package com.indeed.proctor.service.var;

/**
 * Thrown when a converter has some error during conversion that is unrecoverable.
 */
public class ValueConversionException extends Exception {
    public ValueConversionException(final String message) {
        super(message);
    }
}
