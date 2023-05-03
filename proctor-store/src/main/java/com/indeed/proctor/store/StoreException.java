package com.indeed.proctor.store;

/**
 * @author parker
 */
public class StoreException extends Exception {
    public StoreException() {
    }

    public StoreException(String s) {
        super(s);
    }

    public StoreException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public StoreException(Throwable throwable) {
        super(throwable);
    }

    public static class ReadException extends StoreException {
        public ReadException() {
        }

        public ReadException(String s) {
            super(s);
        }

        public ReadException(String s, Throwable throwable) {
            super(s, throwable);
        }

        public ReadException(Throwable throwable) {
            super(throwable);
        }
    }

    public static class TestUpdateException extends StoreException {
        public TestUpdateException(final String message) {
            super(message);
        }

        public TestUpdateException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
