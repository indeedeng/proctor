package com.indeed.proctor.common;

import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestType;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.fail;

/**
 * @author parker
 */
public class TestSerializers {
    private static final String EXAMPLE_TEST_DEFINITION = "example-test-definition.json";
    private static final String SUBRULE_TEST_DEFINITION = "subrule-example-test-definition.json";
    private static final String UNRECOGNIZED_FIELDS_TEST_DEFINITION = "unrecognized-fields-test-definition.json";

    @Test
    public void testTestDefiniton() throws IOException {
        doAssertTestDefintion(EXAMPLE_TEST_DEFINITION, Serializers.lenient());
    }

    @Test
    public void testSubruleTestDefiniton() throws IOException {
        // Tests the loading of a deprecated test-definition containing 'subrule'
        doAssertTestDefintion(SUBRULE_TEST_DEFINITION, Serializers.lenient());
    }

    @Test
    public void testUnrecognizedFieldsTestDefinition() throws Exception {
        // Expect no error and all valid fields returned correctly.
        doAssertTestDefintion(UNRECOGNIZED_FIELDS_TEST_DEFINITION, Serializers.lenient());

        try {
            doAssertTestDefintion(UNRECOGNIZED_FIELDS_TEST_DEFINITION, Serializers.strict());
            fail("Expected an exception to be thrown due to the unrecognized field");

        } catch (IOException e) {
            // expected
        }
    }

    @Test
    public void testAuditVersionAsInt() throws IOException {
        // Tests the legacy audit.version as integer
        final String AUDIT_JSON = "{ \"version\" : 56783, \"updatedBy\" : \"jenkins-build-123\", \"updated\" : 1400693905572 }";
        final Audit audit = Serializers.lenient().readValue(AUDIT_JSON, Audit.class);
        Assert.assertEquals("56783", audit.getVersion());
        Assert.assertEquals("jenkins-build-123", audit.getUpdatedBy());
        Assert.assertEquals(1400693905572L, audit.getUpdated());
    }

    @Test
    public void testAuditVersionAsString() throws IOException {
        // Tests the audit.version as String
        final String AUDIT_JSON = "{ \"version\" : \"56783\", \"updatedBy\" : \"jenkins-build-123\", \"updated\" : 1400693905572 }";
        final Audit audit = Serializers.lenient().readValue(AUDIT_JSON, Audit.class);
        Assert.assertEquals("56783", audit.getVersion());
        Assert.assertEquals("jenkins-build-123", audit.getUpdatedBy());
        Assert.assertEquals(1400693905572L, audit.getUpdated());
    }


    private void doAssertTestDefintion(final String resourceName, final ObjectMapper mapper) throws IOException {
        final InputStream input = getClass().getResourceAsStream(resourceName);
        Assert.assertNotNull("Input stream for " + resourceName + " should not be null", input);
        try {
            final TestDefinition definition = mapper.readValue(input, TestDefinition.class);
            Assert.assertEquals("1", definition.getVersion());
            Assert.assertEquals(TestType.ANONYMOUS_USER, definition.getTestType());
            Assert.assertEquals("exampletst", definition.getSalt());
            Assert.assertEquals("loggedIn", definition.getRule());
            Assert.assertTrue(definition.getDescription().startsWith("An example test"));
            Assert.assertEquals(0, definition.getSpecialConstants().size());
            Assert.assertEquals(1, definition.getConstants().size());
            Assert.assertEquals("en", definition.getConstants().get("ENGLISH"));

            Assert.assertEquals(2, definition.getBuckets().size());
            final TestBucket controlBucket = definition.getBuckets().get(0);
            Assert.assertEquals("control", controlBucket.getName());
            Assert.assertEquals(0, controlBucket.getValue());
            Assert.assertNull(controlBucket.getDescription());

            final TestBucket testBucket = definition.getBuckets().get(1);
            Assert.assertEquals("test", testBucket.getName());
            Assert.assertEquals(1, testBucket.getValue());
            Assert.assertNull(testBucket.getDescription());

            Assert.assertEquals(2, definition.getAllocations().size());
            final Allocation englishAllocation = definition.getAllocations().get(0);
            Assert.assertEquals("${lang == ENGLISH}", englishAllocation.getRule());
            Assert.assertEquals(0.25, englishAllocation.getRanges().get(0).getLength(), 1E-16);
            Assert.assertEquals(0, englishAllocation.getRanges().get(0).getBucketValue());
            Assert.assertEquals(0.75, englishAllocation.getRanges().get(1).getLength(), 1E-16);
            Assert.assertEquals(1, englishAllocation.getRanges().get(1).getBucketValue());

            final Allocation defaultAllocation = definition.getAllocations().get(1);
            Assert.assertNull(defaultAllocation.getRule());
            Assert.assertEquals(0.1, defaultAllocation.getRanges().get(0).getLength(), 1E-16);
            Assert.assertEquals(0, defaultAllocation.getRanges().get(0).getBucketValue());
            Assert.assertEquals(0.90, defaultAllocation.getRanges().get(1).getLength(), 1E-16);
            Assert.assertEquals(1, defaultAllocation.getRanges().get(1).getBucketValue());
        } finally {
            input.close();
        }

    }
}
