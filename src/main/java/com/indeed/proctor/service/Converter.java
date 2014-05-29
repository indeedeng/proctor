package com.indeed.proctor.service;

/**
 * Converts variables to their appropriate final types.
 *
 * Context variables are converted according to the service configuration.
 */
public class Converter {
    private final ServiceConfig config;

    public Converter(final ServiceConfig config) {
        this.config = config;
    }

    public ConvertedParameters convert(final RawParameters raw) {
        return new ConvertedParameters(raw);
    }
}
