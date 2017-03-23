package com.indeed.proctor.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.text.DecimalFormat;

public class Serializers {
    /**
     * Customized serializer class for suppressing scientific notation.
     * @author yosukey
     */
    private static class PlainNumericSerializer extends JsonSerializer<Double> {
        private final DecimalFormat decimalFormat;

        private PlainNumericSerializer() {
            this.decimalFormat = new DecimalFormat("0.0");
            decimalFormat.setMaximumFractionDigits(10);
        }

        @Override
        public void serialize(final Double aDouble, final JsonGenerator jsonGenerator, final SerializerProvider serializerProvider)
                throws IOException, JsonProcessingException {
            jsonGenerator.writeNumber(decimalFormat.format(aDouble));
        }
    }

    /**
     * @return an {@link ObjectMapper} configured to do things as we want
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
        final SimpleModule module = new SimpleModule();
        final PlainNumericSerializer plainNumericSerializer = new PlainNumericSerializer();
        module.addSerializer(double.class, plainNumericSerializer);
        module.addSerializer(Double.class, plainNumericSerializer);
        mapper.registerModule(module);
        return mapper;
    }

    /**
     * @return an {@link ObjectMapper} configured to do things as we want
     */
    @Nonnull
    public static ObjectMapper strict() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(Feature.ALLOW_COMMENTS, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        final SimpleModule module = new SimpleModule();
        final PlainNumericSerializer plainNumericSerializer = new PlainNumericSerializer();
        module.addSerializer(double.class, plainNumericSerializer);
        module.addSerializer(Double.class, plainNumericSerializer);
        mapper.registerModule(module);
        return mapper;
    }
}
