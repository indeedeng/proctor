package com.indeed.proctor.service;

/**
 * Thrown when a resource was not found.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(final String message) {
        super(message);
    }
}
