package com.indeed.proctor.webapp.tags;


import com.indeed.proctor.common.Serializers;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

public final class JSONFunctions {

    private static final ObjectMapper OBJECT_MAPPER = Serializers.strict();

    public static String prettyPrintJSON(Object o) throws IOException, JsonGenerationException, JsonMappingException {
        return OBJECT_MAPPER.defaultPrettyPrintingWriter().writeValueAsString(o);
    }
}
