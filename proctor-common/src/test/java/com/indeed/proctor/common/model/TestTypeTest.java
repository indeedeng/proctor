package com.indeed.proctor.common.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.proctor.common.Serializers;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        final TestType testType = MAPPER.readValue(json, TestType.class);
        assertEquals("FOOBAR", testType.name());
    }

    @Test
    public void testIsValidDependency() {
        final TestType testType = TestType.register("testisvalid");

        testType.addDependency(TestType.EMAIL_ADDRESS);
        testType.addDependency(TestType.ANONYMOUS_USER);

        assertTrue(TestType.ANONYMOUS_USER.isValidDependency(TestType.ANONYMOUS_USER));

        assertTrue(testType.isValidDependency(TestType.EMAIL_ADDRESS));
        assertTrue(testType.isValidDependency(TestType.ANONYMOUS_USER));
        assertFalse(testType.isValidDependency(TestType.RANDOM));
    }

    @Test
    public void testDependenciesToString() {
        final TestType testType = TestType.register("testisvalid");

        testType.addDependency(TestType.EMAIL_ADDRESS);
        testType.addDependency(TestType.ANONYMOUS_USER);

        assertEquals(testType.dependenciesToString(), "testisvalid, EMAIL, USER");
    }
}
