package com.indeed.proctor.pipet.core.config;

/**
 * Exception for errors found during the processing of the pipet configuration file (before web).
 */
public class ConfigurationException extends RuntimeException {
    public ConfigurationException(final String message) {
        super(message);
    }
}
