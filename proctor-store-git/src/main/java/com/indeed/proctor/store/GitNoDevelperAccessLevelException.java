package com.indeed.proctor.store;

public class GitNoDevelperAccessLevelException extends StoreException.TestUpdateException {

    public GitNoDevelperAccessLevelException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
