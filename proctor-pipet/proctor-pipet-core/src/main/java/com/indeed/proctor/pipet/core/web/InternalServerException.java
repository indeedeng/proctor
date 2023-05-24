package com.indeed.proctor.pipet.core.web;

/**
 * Thrown when we encounter an internal error that is not the fault of the user or the request
 * parameters.
 */
public class InternalServerException extends RuntimeException {
    public InternalServerException(final String message) {
        super(message);
    }
}
