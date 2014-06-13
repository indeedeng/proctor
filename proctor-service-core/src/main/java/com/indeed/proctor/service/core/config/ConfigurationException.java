package com.indeed.proctor.service.core.config;

/**
 * Exception for errors found during the processing of the service configuration file (before web).
 */
public class ConfigurationException extends RuntimeException {
    public ConfigurationException(final String message) {
        super(message);
    }
}
