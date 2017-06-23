package com.indeed.proctor.store;

public class GitNoMasterAccessLevelException extends StoreException.TestUpdateException {

    public GitNoMasterAccessLevelException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
