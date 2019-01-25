package com.indeed.proctor.store;

public class GitServerErrorException extends StoreException.TestUpdateException {
    public GitServerErrorException(final String message) {
        super(message);
    }

    public GitServerErrorException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
