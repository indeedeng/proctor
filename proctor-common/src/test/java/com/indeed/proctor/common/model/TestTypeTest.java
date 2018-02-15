package com.indeed.proctor.common.model;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.proctor.common.Serializers;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestTypeTest {
    private static final ObjectMapper MAPPER = Serializers.lenient();

    @Test
    public void testExtensibility() {
        // Note that something has to trigger the registration of the extended test type
        final TestType ipAddress = TestType.register("IP_ADDRESS");
        assertTrue(TestType.all().contains(ipAddress));
        assertTrue(TestType.all().contains(TestType.ANONYMOUS_USER));
    }

    @Test
    public void testJsonDeserializationSingleton() throws IOException {
        String json = "\"USER\"";
        final TestType testType = MAPPER.readValue(json, TestType.class);
        final TestType testType2 = MAPPER.readValue(json, TestType.class);
        assertTrue(testType == testType2);
    }

    @Test
    public void testJson_notDefined() throws IOException {
        String json = "\"FOOBAR\"";
        try {
            MAPPER.readValue(json, TestType.class);
            fail();
        } catch (final JsonMappingException e) {
            assertTrue(e.getCause().getMessage().startsWith("Value 'FOOBAR' not one of the declared instances"));
        }
    }
}
