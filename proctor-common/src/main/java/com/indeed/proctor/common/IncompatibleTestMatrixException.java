package com.indeed.proctor.common;

public class IncompatibleTestMatrixException extends Exception {
    public IncompatibleTestMatrixException(final String message) {
        super(message);
    }

    public IncompatibleTestMatrixException(final String message, final Throwable cause) {
        super(message, cause);
    }

    private static final long serialVersionUID = 2904964522625450905L;
}
