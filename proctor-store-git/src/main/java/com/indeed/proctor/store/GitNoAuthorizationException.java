package com.indeed.proctor.store;

public class GitNoAuthorizationException extends StoreException.TestUpdateException {

    public GitNoAuthorizationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
