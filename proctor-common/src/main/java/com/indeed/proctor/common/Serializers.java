package com.indeed.proctor.common;

import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javax.annotation.Nonnull;

public class Serializers {
    /**
     * Get an {@link ObjectMapper} configured to do things as we want
     * @deprecated Specify whether you want {@link #lenient()} or {@link #strict()}} parsing
     */
    @Nonnull
    public static ObjectMapper getObjectMapper() {
        // By default, use the lenient mapper because blowing up is something we
        //  only want to do in controlled environments.
        return lenient();
    }

    public static ObjectMapper lenient() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(Feature.ALLOW_COMMENTS, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    /**
     * Get an {@link ObjectMapper} configured to do things as we want
     */
    @Nonnull
    public static ObjectMapper strict() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(Feature.ALLOW_COMMENTS, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        return mapper;
    }
}
