package com.indeed.proctor.service.core.web;

/**
 * Thrown when the user gives invalid request parameters to an API call.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(final String message) {
        super(message);
    }
}
