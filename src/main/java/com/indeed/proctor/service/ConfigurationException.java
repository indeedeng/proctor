package com.indeed.proctor.service;

/**
 * Exception for errors found during the processing of the service configuration file (before web).
 */
public class ConfigurationException extends RuntimeException {
    public ConfigurationException(String message) {
        super(message);
    }
}
