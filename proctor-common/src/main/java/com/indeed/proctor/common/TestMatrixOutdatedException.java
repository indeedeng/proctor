package com.indeed.proctor.common;

public class TestMatrixOutdatedException extends Exception {
    public TestMatrixOutdatedException(final String message) {
        super(message);
    }

    public TestMatrixOutdatedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
