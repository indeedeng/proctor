package com.indeed.proctor.webapp.tags;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.proctor.common.PayloadType;
import com.indeed.proctor.common.Serializers;
import com.indeed.proctor.common.model.Payload;

import java.io.IOException;
import java.util.Optional;

/** @author pwp */
public class PayloadFunctions {
    private static final ObjectMapper OBJECT_MAPPER = Serializers.strict();
    private static final String[] PROCTOR_PAYLOAD_TYPES =
            PayloadType.allTypeNames().toArray(new String[PayloadType.allTypeNames().size()]);

    /**
     * If o is a proctor Payload object, then print just the contents of the payload, otherwise just
     * pretty print the object.
     */
    public static String prettyPrintJSONPayloadContents(final Object o)
            throws IOException, JsonGenerationException, JsonMappingException {
        if (o != null && o instanceof Payload) {
            final Payload p = (Payload) o;
            return OBJECT_MAPPER
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(p.fetchAValue());
        } else {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(o);
        }
    }

    /**
     * If o is a proctor Payload object, then return its type as a string, otherwise return "none".
     */
    public static String printPayloadType(final Object o)
            throws IOException, JsonGenerationException, JsonMappingException {
        return Optional.ofNullable(o)
                .filter(ob -> ob instanceof Payload)
                .flatMap(ob -> ((Payload) ob).fetchPayloadType())
                .map(p -> p.payloadTypeName)
                .orElse("none");
    }

    public static String[] allPayloadTypeStrings() {
        return PROCTOR_PAYLOAD_TYPES;
    }
}
