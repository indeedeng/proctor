package com.indeed.proctor.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.proctor.common.dynamic.DynamicFilter;
import com.indeed.proctor.common.dynamic.DynamicFilters;
import com.indeed.proctor.common.dynamic.TestNamePatternFilter;
import com.indeed.proctor.common.dynamic.TestNamePrefixFilter;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestDependency;
import com.indeed.proctor.common.model.TestType;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author parker
 */
public class TestSerializers {
    private static final String EXAMPLE_TEST_DEFINITION = "example-test-definition.json";
    private static final String EXAMPLE_TEST_DEFINITION_WITH_DEPENDENCY = "example-test-definition-with-dependency.json";
    private static final String SUBRULE_TEST_DEFINITION = "subrule-example-test-definition.json";
    private static final String UNRECOGNIZED_FIELDS_TEST_DEFINITION = "unrecognized-fields-test-definition.json";

    @Test
    public void testTestDefiniton() throws IOException {
        doAssertTestDefintion(EXAMPLE_TEST_DEFINITION, Serializers.lenient());
    }

    @Test
    public void testTestDefinitionWithDependency() throws IOException {
        try(InputStream input = getClass().getResourceAsStream(EXAMPLE_TEST_DEFINITION_WITH_DEPENDENCY)) {
            final TestDefinition definition = Serializers.lenient().readValue(input, TestDefinition.class);
            assertThat(definition.getDependsOn())
                    .isEqualTo(new TestDependency("another_tst", 1));
        }
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
        assertEquals("56783", audit.getVersion());
        assertEquals("jenkins-build-123", audit.getUpdatedBy());
        assertEquals(1400693905572L, audit.getUpdated());
    }

    @Test
    public void testAuditVersionAsString() throws IOException {
        // Tests the audit.version as String
        final String AUDIT_JSON = "{ \"version\" : \"56783\", \"updatedBy\" : \"jenkins-build-123\", \"updated\" : 1400693905572 }";
        final Audit audit = Serializers.lenient().readValue(AUDIT_JSON, Audit.class);
        assertEquals("56783", audit.getVersion());
        assertEquals("jenkins-build-123", audit.getUpdatedBy());
        assertEquals(1400693905572L, audit.getUpdated());
    }

    @Test
    public void testPlainNumericSerializerLenient() throws Exception {
        doTestPlainNumericSerializer(Serializers.lenient());
    }

    @Test
    public void testPlainNumericSerializerStrict() throws Exception {
        doTestPlainNumericSerializer(Serializers.strict());
    }

    private void doTestPlainNumericSerializer(final ObjectMapper mapper) throws Exception {
        assertEquals("0.0", mapper.writeValueAsString(0.0));
        assertEquals("1.0", mapper.writeValueAsString(1.0));
        assertEquals("0.1", mapper.writeValueAsString(0.1));
        assertEquals("0.00001", mapper.writeValueAsString(0.00001));
        assertEquals("100000.0", mapper.writeValueAsString(100000.0));
    }

    private void doAssertTestDefintion(final String resourceName, final ObjectMapper mapper) throws IOException {
        final InputStream input = getClass().getResourceAsStream(resourceName);
        assertNotNull("Input stream for " + resourceName + " should not be null", input);
        try {
            final TestDefinition definition = mapper.readValue(input, TestDefinition.class);
            assertEquals("1", definition.getVersion());
            assertEquals(TestType.ANONYMOUS_USER, definition.getTestType());
            assertEquals("exampletst", definition.getSalt());
            assertEquals("loggedIn", definition.getRule());
            assertTrue(definition.getDescription().startsWith("An example test"));
            assertEquals(0, definition.getSpecialConstants().size());
            assertEquals(1, definition.getConstants().size());
            assertEquals("en", definition.getConstants().get("ENGLISH"));

            assertEquals(2, definition.getBuckets().size());
            final TestBucket controlBucket = definition.getBuckets().get(0);
            assertEquals("control", controlBucket.getName());
            assertEquals(0, controlBucket.getValue());
            Assert.assertNull(controlBucket.getDescription());

            final TestBucket testBucket = definition.getBuckets().get(1);
            assertEquals("test", testBucket.getName());
            assertEquals(1, testBucket.getValue());
            Assert.assertNull(testBucket.getDescription());

            assertEquals(2, definition.getAllocations().size());
            final Allocation englishAllocation = definition.getAllocations().get(0);
            assertEquals("${lang == ENGLISH}", englishAllocation.getRule());
            assertEquals(0.25, englishAllocation.getRanges().get(0).getLength(), 1E-16);
            assertEquals(0, englishAllocation.getRanges().get(0).getBucketValue());
            assertEquals(0.75, englishAllocation.getRanges().get(1).getLength(), 1E-16);
            assertEquals(1, englishAllocation.getRanges().get(1).getBucketValue());

            final Allocation defaultAllocation = definition.getAllocations().get(1);
            Assert.assertNull(defaultAllocation.getRule());
            assertEquals(0.1, defaultAllocation.getRanges().get(0).getLength(), 1E-16);
            assertEquals(0, defaultAllocation.getRanges().get(0).getBucketValue());
            assertEquals(0.90, defaultAllocation.getRanges().get(1).getLength(), 1E-16);
            assertEquals(1, defaultAllocation.getRanges().get(1).getBucketValue());
        } finally {
            input.close();
        }

    }

    @Test
    public void testNamePrefixFilterSerializeAndDeserialize() throws IOException {
        final ObjectMapper objectMapper = Serializers.lenient();
        final DynamicFilter filter = new TestNamePrefixFilter("abc");
        final String json = objectMapper.writeValueAsString(filter);
        final DynamicFilter convertedFilter = objectMapper.readValue(json, DynamicFilter.class);
        assertTrue(filter.matches("abc_z", new ConsumableTestDefinition()));
        assertTrue(convertedFilter.matches("abc_z", new ConsumableTestDefinition()));
        assertFalse(filter.matches("zzz", new ConsumableTestDefinition()));
        assertFalse(convertedFilter.matches("zzz", new ConsumableTestDefinition()));
    }

    @Test
    public void testNamePrefixFilterDeserialize() throws IOException {
        final ObjectMapper objectMapper = Serializers.lenient();
        final String json = "{\"type\": \"name_prefix\", \"prefix\": \"abc_\"}";
        final DynamicFilter filter = objectMapper.readValue(json, DynamicFilter.class);
        assertTrue(filter.matches("abc_z", new ConsumableTestDefinition()));
        assertFalse(filter.matches("zzz", new ConsumableTestDefinition()));
        assertTrue(filter instanceof TestNamePrefixFilter);
        assertEquals("abc_", ((TestNamePrefixFilter) filter).getPrefix());
    }

    @Test
    public void testNamePatternFilterSerializeAndDeserialize() throws IOException {
        final ObjectMapper objectMapper = Serializers.lenient();
        final DynamicFilter filter = new TestNamePatternFilter("abc_[a-z]+_xyz");
        final String json = objectMapper.writeValueAsString(filter);
        final DynamicFilter convertedFilter = objectMapper.readValue(json, DynamicFilter.class);
        assertTrue(filter.matches("abc_aaa_xyz", new ConsumableTestDefinition()));
        assertTrue(convertedFilter.matches("abc_aaa_xyz", new ConsumableTestDefinition()));
        assertFalse(filter.matches("abcxyz", new ConsumableTestDefinition()));
        assertFalse(convertedFilter.matches("abcxyz", new ConsumableTestDefinition()));
    }

    @Test
    public void testNamePatternFilterDeserialize() throws IOException {
        final ObjectMapper objectMapper = Serializers.lenient();
        final String json = "{\"type\": \"name_pattern\", \"regex\": \"abc_[a-z]+_xyz\"}";
        final DynamicFilter filter = objectMapper.readValue(json, DynamicFilter.class);
        assertTrue(filter.matches("abc_aaa_xyz", new ConsumableTestDefinition()));
        assertFalse(filter.matches("abcxyz", new ConsumableTestDefinition()));
        assertTrue(filter instanceof TestNamePatternFilter);
        assertEquals("abc_[a-z]+_xyz", ((TestNamePatternFilter) filter).getRegex());
    }

    @Test
    public void testEmptySpecificationDeserialize() throws IOException {
        final ObjectMapper objectMapper = Serializers.lenient();
        final String json = "{" +
                "\"tests\": {}," +
                "\"providedContext\": {}" +
                "}";
        final ProctorSpecification specification = objectMapper.readValue(json, ProctorSpecification.class);
        assertTrue(specification.getTests().isEmpty());
        assertTrue(specification.getProvidedContext().isEmpty());
        assertEquals(new DynamicFilters(), specification.getDynamicFilters());
    }

    @Test
    public void testDynamicFilterSpecificationDeserialize() throws IOException {
        final ObjectMapper objectMapper = Serializers.lenient();
        final String json = "{" +
                "\"tests\": {}, " +
                "\"providedContext\": {}, " +
                "\"dynamicFilters\": [" +
                "{\"type\": \"name_prefix\", \"prefix\": \"abc_\"}, " +
                "{\"type\": \"name_pattern\", \"regex\": \"abc_[a-z]+_xyz\"}" +
                "]" +
                "}";
        final ProctorSpecification specification = objectMapper.readValue(json, ProctorSpecification.class);
        assertTrue(specification.getTests().isEmpty());
        assertTrue(specification.getProvidedContext().isEmpty());
        assertEquals(
                new DynamicFilters(
                        Arrays.asList(
                                new TestNamePrefixFilter("abc_"),
                                new TestNamePatternFilter("abc_[a-z]+_xyz")
                        )
                ),
                specification.getDynamicFilters()
        );
    }

}
