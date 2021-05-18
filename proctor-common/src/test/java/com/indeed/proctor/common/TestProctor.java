package com.indeed.proctor.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestDependency;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.proctor.common.model.TestType;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author piotr
 */
public class TestProctor {
    @Test
    public void testAppendTestMatrix_emptyProctor() throws IOException {
        // Very simplistically test the appendTestMatrix output.
        // Just check that the general structure is correct (tests and audit) for an empty Proctor.
        final Writer writer = new StringWriter();
        Proctor.createEmptyProctor().appendTestMatrix(writer);

        // If JSON processing fails, readTree should throw an exception and fail the test.
        final JsonNode root = new ObjectMapper().readTree(writer.toString());

        // Matrix should only have two fields: tests, audit
        assertEquals(2, root.size());
        assertTrue(root.has("tests"));
        assertTrue(root.has("audit"));
    }

    @Test
    public void testAppendTestMatrix_audit() throws IOException {
        // Check that the audit values are correct.
        final TestMatrixArtifact matrix = new TestMatrixArtifact();

        final Audit audit = new Audit();
        audit.setVersion("10");
        audit.setUpdated(1454432430000L);
        audit.setUpdatedBy("nobody");
        matrix.setAudit(audit);

        final Writer writer = new StringWriter();
        final Proctor proctor = Proctor.construct(matrix, null, null);
        proctor.appendTestMatrix(writer);

        final JsonNode root = new ObjectMapper().readTree(writer.toString());
        assertTrue(root.has("audit"));
        final JsonNode auditNode = root.get("audit");
        assertEquals(4, auditNode.size());

        assertTrue(auditNode.has("version"));
        assertEquals("10", auditNode.get("version").textValue());
        assertTrue(auditNode.has("updated"));
        assertEquals(1454432430000L, auditNode.get("updated").longValue());
        assertTrue(auditNode.has("updatedDate"));
        assertEquals("2016-02-02T11:00-0600", auditNode.get("updatedDate").textValue());
        assertTrue(auditNode.has("updatedBy"));
        assertTrue(auditNode.get("updatedBy").isTextual());
        assertEquals("nobody", auditNode.get("updatedBy").textValue());
    }

    @Test
    public void testAppendTestMatrix_threeTests() throws IOException {
        // We tested audit above, so I'll leave it null in this matrix.
        final TestMatrixArtifact matrix = createThreeFakeTests();
        final Proctor proctor = Proctor.construct(matrix, null, RuleEvaluator.FUNCTION_MAPPER);
        final Writer writer = new StringWriter();
        proctor.appendTestMatrix(writer);

        final JsonNode root = new ObjectMapper().readTree(writer.toString());
        assertTrue(root.has("tests"));
        final JsonNode tests = root.get("tests");
        assertEquals(3, tests.size());

        assertTrue(tests.has("one"));
        assertFalse(tests.get("one").isNull());
        assertTrue(tests.has("two"));
        assertFalse(tests.get("two").isNull());
        assertTrue(tests.has("three"));
        assertFalse(tests.get("three").isNull());
    }

    @Test
    public void testAppendTestMatrixFiltered_oneTest() throws IOException {
        final JsonNode root = appendTestMatrixFiltered_processAndGetRoot(Arrays.asList("two"));

        assertTrue(root.has("tests"));
        final JsonNode tests = root.get("tests");
        assertEquals(1, tests.size());
        assertTrue(tests.has("two"));
        assertFalse(tests.get("two").isNull());
    }

    @Test
    public void testAppendTestMatrixFiltered_twoTest() throws IOException {
        final JsonNode root = appendTestMatrixFiltered_processAndGetRoot(Arrays.asList("one", "three"));

        assertTrue(root.has("tests"));
        final JsonNode tests = root.get("tests");
        assertEquals(2, tests.size());
        assertTrue(tests.has("one"));
        assertFalse(tests.get("one").isNull());
        assertTrue(tests.has("three"));
        assertFalse(tests.get("three").isNull());
    }

    @Test
    public void testAppendTestMatrixFiltered_allThreeTest() throws IOException {
        final JsonNode root = appendTestMatrixFiltered_processAndGetRoot(Arrays.asList("three", "two", "one"));

        assertTrue(root.has("tests"));
        final JsonNode tests = root.get("tests");
        assertEquals(3, tests.size());
        assertTrue(tests.has("one"));
        assertFalse(tests.get("one").isNull());
        assertTrue(tests.has("two"));
        assertFalse(tests.get("two").isNull());
        assertTrue(tests.has("three"));
        assertFalse(tests.get("three").isNull());
    }

    @Test
    public void testAppendTestMatrixFiltered_nonexistTests() throws IOException {
        // Ensure that invalid tests just return no tests at all.
        final JsonNode root = appendTestMatrixFiltered_processAndGetRoot(Arrays.asList("four", "eleventy"));

        assertTrue(root.has("tests"));
        final JsonNode tests = root.get("tests");
        assertEquals(0, tests.size());
    }

    @Test
    public void testAppendTestMatrixFiltered_emptyTests() throws IOException {
        // Ensure that invalid tests just return no tests at all.
        final JsonNode root = appendTestMatrixFiltered_processAndGetRoot(Collections.<String>emptyList());

        assertTrue(root.has("tests"));
        final JsonNode tests = root.get("tests");
        assertEquals(0, tests.size());
    }

    private JsonNode appendTestMatrixFiltered_processAndGetRoot(final Collection<String> names) throws IOException {
        final TestMatrixArtifact matrix = createThreeFakeTests();
        final Proctor proctor = Proctor.construct(matrix, null, RuleEvaluator.FUNCTION_MAPPER);
        final Writer writer = new StringWriter();
        proctor.appendTestMatrixFiltered(writer, names);

        return new ObjectMapper().readTree(writer.toString());
    }

    @Test
    public void testAppendAllTests_threeTest() {
        final TestMatrixArtifact matrix = createThreeFakeTests();
        final Proctor proctor = Proctor.construct(matrix, null, RuleEvaluator.FUNCTION_MAPPER);
        final Writer writer = new StringWriter();
        proctor.appendAllTests(writer);
        // Discard trailing empty strings to simplify length testing later.
        final List<String> lines = Lists.newArrayList(
                Splitter.on("\n").trimResults().omitEmptyStrings().split(writer.toString()));

        // Confirm that all three tests show up in the output.
        // Note: theoretically, they could show up in any order and still be correct.
        // so we sort the array first (by character) so that the order is always consistent.
        // "one" < "three" < "two"
        Collections.sort(lines);

        assertEquals(3, lines.size());
        assertTrue(lines.get(0).startsWith("one :"));
        assertTrue(lines.get(1).startsWith("three :"));
        assertTrue(lines.get(2).startsWith("two :"));
    }

    @Test
    public void testAppendTestsNameFiltered_oneTest() {
        final List<String> lines = appendTestsNameFiltered_process(Arrays.asList("three"));

        assertEquals(1, lines.size());
        assertTrue(lines.get(0).startsWith("three :"));
    }

    @Test
    public void testAppendTestsNameFiltered_twoTest() {
        final List<String> lines = appendTestsNameFiltered_process(Arrays.asList("one", "two"));

        assertEquals(2, lines.size());
        assertTrue(lines.get(0).startsWith("one :"));
        assertTrue(lines.get(1).startsWith("two :"));
    }

    @Test
    public void testAppendTestsNameFiltered_allThreeTest() {
        final List<String> lines = appendTestsNameFiltered_process(Arrays.asList("one", "two", "three"));

        assertEquals(3, lines.size());
        assertTrue(lines.get(0).startsWith("one :"));
        assertTrue(lines.get(1).startsWith("three :"));
        assertTrue(lines.get(2).startsWith("two :"));
    }

    @Test
    public void testAppendTestsNameFiltered_nonexistTests() {
        final List<String> lines = appendTestsNameFiltered_process(Arrays.asList("four", "eleventy"));

        assertEquals(0, lines.size());
    }

    @Test
    public void testAppendTestsNameFiltered_emptyTests() {
        final List<String> lines = appendTestsNameFiltered_process(Collections.<String>emptyList());

        assertEquals(0, lines.size());
    }

    @Test
    public void testGetProctorTestNames() throws IOException {
        final TestMatrixArtifact matrix = createThreeFakeTests();
        final Proctor proctor = Proctor.construct(matrix, null, RuleEvaluator.FUNCTION_MAPPER);
        final Writer writer = new StringWriter();
        proctor.appendTestMatrix(writer);

        final JsonNode root = new ObjectMapper().readTree(writer.toString());
        assertTrue(root.has("tests"));
        final JsonNode tests = root.get("tests");
        assertEquals(3, tests.size());

        Set<String> testNames = proctor.getTestNames();
        assertTrue(testNames.size() == 3);
        assertTrue(testNames.contains("one"));
        assertTrue(testNames.contains("two"));
        assertTrue(testNames.contains("three"));
        assertFalse(testNames.contains("four"));
    }

    @Test
    public void testDetermineTestGroupsForRandomTestWithRandomEnabled() {
        final String testName = "example_tst";
        final RandomTestChooser testChooser = mock(RandomTestChooser.class);

        final TestMatrixArtifact matrix = createTestMatrixWithOneRandomTest(testName);

        final Proctor proctor = new Proctor(
                matrix,
                null,
                Collections.singletonMap(testName, testChooser),
                Collections.singletonList(testName)
        );

        final Identifiers identifiersWithRandom = new Identifiers(Collections.emptyMap(), true);
        final Identifiers identifiersWithoutRandom = new Identifiers(Collections.emptyMap(), false);

        final Map<String, Object> inputContext = Collections.emptyMap();
        final TestBucket testBucket = TestBucket.builder().build();
        final Allocation allocation = new Allocation();
        final TestChooser.Result result = new TestChooser.Result(
                testBucket, allocation
        );

        when(testChooser.choose(isNull(), eq(inputContext), anyMap())).thenReturn(result);

        final ProctorResult proctorResultWithRandom = proctor.determineTestGroups(
                identifiersWithRandom,
                inputContext,
                Collections.emptyMap()
        );
        final ProctorResult proctorResultWithoutRandom = proctor.determineTestGroups(
                identifiersWithoutRandom,
                inputContext,
                Collections.emptyMap()
        );

        assertThat(proctorResultWithRandom.getBuckets()).isEqualTo(Collections.singletonMap(testName, result.getTestBucket()));
        assertThat(proctorResultWithRandom.getAllocations()).isEqualTo(Collections.singletonMap(testName, result.getAllocation()));

        assertThat(proctorResultWithoutRandom.getBuckets()).isEqualTo(Collections.emptyMap());
        assertThat(proctorResultWithoutRandom.getAllocations()).isEqualTo(Collections.emptyMap());

        // choose should not be called for identifiers with randomEnabled == false.
        verify(testChooser, times(1)).choose(isNull(), eq(inputContext), anyMap());
    }

    @Test
    public void testDetermineTestGroupsFilteringTestsWithDependency() {
        // if test X depends Y and only X is given to filter,
        // it should be able to evaluate X.

        final TestBucket testBucket = new TestBucket("active", 1, "");
        final Allocation allocation = new Allocation(
                "",
                ImmutableList.of(new Range(1, 1.0))
        );

        final ConsumableTestDefinition testDefinitionX = ConsumableTestDefinition.fromTestDefinition(
                TestDefinition.builder()
                        .setSalt("&X")
                        .setTestType(TestType.ANONYMOUS_USER)
                        .setDependsOn(new TestDependency("Y", 1))
                        .addBuckets(testBucket)
                        .addAllocations(allocation)
                        .build()
        );
        final ConsumableTestDefinition testDefinitionY = ConsumableTestDefinition.fromTestDefinition(
                TestDefinition.builder()
                        .setSalt("&Y")
                        .setTestType(TestType.ANONYMOUS_USER)
                        .addBuckets(testBucket)
                        .addAllocations(allocation)
                        .build()
        );

        final Map<String, ConsumableTestDefinition> tests = ImmutableMap.of(
                "X", testDefinitionX,
                "Y", testDefinitionY
        );
        final TestMatrixArtifact matrix = new TestMatrixArtifact();
        matrix.setTests(tests);
        matrix.setAudit(new Audit());

        final Proctor proctor = Proctor.construct(
                matrix,
                ProctorLoadResult.emptyResult(),
                RuleEvaluator.defaultFunctionMapperBuilder().build()
        );

        final ProctorResult proctorResult = proctor.determineTestGroups(
                Identifiers.of(TestType.ANONYMOUS_USER, "cookie"),
                Collections.emptyMap(),
                Collections.emptyMap(),
                ImmutableList.of("X")
        );

        assertThat(proctorResult.getBuckets())
                .containsOnlyKeys("X")
                .containsEntry("X", testBucket);
        assertThat(proctorResult.getAllocations())
                .containsOnlyKeys("X")
                .containsEntry("X", allocation);
        assertThat(proctorResult.getTestDefinitions())
                .containsOnlyKeys("X", "Y")
                .containsEntry("X", testDefinitionX)
                .containsEntry("Y", testDefinitionY); // keeping Y for backward compatibility
    }

    private static TestMatrixArtifact createTestMatrixWithOneRandomTest(final String testName) {
        final TestMatrixArtifact matrix = new TestMatrixArtifact();
        final ConsumableTestDefinition testDefinition = new ConsumableTestDefinition();
        testDefinition.setTestType(TestType.RANDOM);
        matrix.setTests(ImmutableMap.of(testName, testDefinition));
        matrix.setAudit(new Audit());

        return matrix;
    }

    // Helper function to get the output from appendTestsNameFiltered
    private List<String> appendTestsNameFiltered_process(final Collection<String> names) {
        final TestMatrixArtifact matrix = createThreeFakeTests();
        final Proctor proctor = Proctor.construct(matrix, null, RuleEvaluator.FUNCTION_MAPPER);
        final Writer writer = new StringWriter();
        proctor.appendTestsNameFiltered(writer, names);
        final List<String> lines = Lists.newArrayList(
                Splitter.on("\n").trimResults().omitEmptyStrings().split(writer.toString()));
        Collections.sort(lines);
        return lines;
    }

    private TestMatrixArtifact createThreeFakeTests() {
        final TestMatrixArtifact matrix = new TestMatrixArtifact();
        final Map<String, ConsumableTestDefinition> testMap = Maps.newHashMap();
        testMap.put("one", new ConsumableTestDefinition());
        testMap.put("two", new ConsumableTestDefinition());
        testMap.put("three", new ConsumableTestDefinition());

        // For appendTests to work, the testType property must not be null.
        testMap.get("one").setTestType(TestType.RANDOM);
        testMap.get("two").setTestType(TestType.RANDOM);
        testMap.get("three").setTestType(TestType.RANDOM);

        matrix.setTests(testMap);
        return matrix;
    }
}
