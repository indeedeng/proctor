package com.indeed.proctor.common;

import org.codehaus.jackson.JsonParser.Feature;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

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
        mapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(Feature.ALLOW_COMMENTS, true);
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    /**
     * Get an {@link ObjectMapper} configured to do things as we want
     */
    @Nonnull
    public static ObjectMapper strict() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(Feature.ALLOW_COMMENTS, true);
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        return mapper;
    }
}
