package com.indeed.proctor.service.core.web;

/**
 *
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(final String message) {
        super(message);
    }
}
