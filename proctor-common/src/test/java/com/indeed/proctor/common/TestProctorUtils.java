package com.indeed.proctor.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestDependency;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.proctor.common.model.TestType;
import org.junit.Test;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import static com.indeed.proctor.common.ProctorUtils.containsUnitlessAllocation;
import static com.indeed.proctor.common.ProctorUtils.convertContextToTestableMap;
import static com.indeed.proctor.common.ProctorUtils.isEmptyElExpression;
import static com.indeed.proctor.common.ProctorUtils.isEmptyWhitespace;
import static com.indeed.proctor.common.ProctorUtils.readSpecification;
import static com.indeed.proctor.common.ProctorUtils.removeElExpressionBraces;
import static com.indeed.proctor.common.ProctorUtils.verifyAndConsolidate;
import static com.indeed.proctor.common.ProctorUtils.verifyInternallyConsistentDefinition;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** @author parker */
public class TestProctorUtils {

    private static final String TEST_A = "testA";
    private static final String TEST_B = "testB";
    private static final String TEST_C = "testC";
    private static final String PATH_UNKNOWN_TEST_TYPE = "unknown-test-type.json";
    private static final ProvidedContext EMPTY_CONTEXT =
            ProvidedContext.forValueExpressionMap(emptyMap(), emptySet());

    /** Test that top level and allocation rules all respect the same definition of "empty" */
    @Test
    public void convertToConsumableTestDefinitionEmptyRules() {
        // all of the following "rules" should be treated as empty in the TestDefinition
        // rule: null
        // rule: ""
        // rule: " "
        final String[] emptyRules = new String[] {null, "", " "};
        final Range range = new Range(0, 1.0d);

        final String version = "100";
        final TestType testType = TestType.ANONYMOUS_USER;
        final String salt = "testsalt";
        final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
        final Map<String, Object> constants = emptyMap();
        final Map<String, Object> specialConstants = emptyMap();
        final String description = "test description";
        final List<String> metaTags = ImmutableList.of("sample_test_tag");

        for (final String tdRule : emptyRules) {
            for (final String allocRule : emptyRules) {
                final Allocation allocation = new Allocation(allocRule, singletonList(range));
                final TestDefinition testDefinition =
                        new TestDefinition(
                                version,
                                tdRule,
                                testType,
                                salt,
                                buckets,
                                singletonList(allocation),
                                false,
                                constants,
                                specialConstants,
                                description,
                                metaTags);

                final ConsumableTestDefinition ctd =
                        ConsumableTestDefinition.fromTestDefinition(testDefinition);
                assertEquals(version, ctd.getVersion());
                assertEquals(testType, ctd.getTestType());
                assertEquals(salt, ctd.getSalt());
                assertEquals(description, ctd.getDescription());
                assertEquals(0, ctd.getConstants().size());
                assertEquals(buckets, ctd.getBuckets());
                assertEquals(metaTags, ctd.getMetaTags());

                assertNull(
                        String.format(
                                "TestDefinition rule '%s' should convert to a null ConsumableTestDefinition.rule",
                                tdRule),
                        ctd.getRule());

                assertEquals(1, ctd.getAllocations().size());
                final Allocation ctdAllocation = ctd.getAllocations().get(0);
                assertNull(
                        String.format(
                                "Allocation rule '%s' should convert to a null ConsumableTestDefinition.Allocation.rule",
                                allocRule),
                        ctdAllocation.getRule());
                assertEquals(allocation.getRanges(), ctdAllocation.getRanges());
            }
        }
    }

    /** Checks that allocation and top level rules can optionally be surrounded by ${ ... } */
    @Test
    public void convertToConsumableTestDefinitionTopLevelRules() {
        // rules can optionally have the "${}" around them.
        // rule: lang == 'en'
        // rule: ${lang == 'en'}
        final Range range = new Range(0, 1.0d);

        final String version = "100";
        final TestType testType = TestType.ANONYMOUS_USER;
        final String salt = "testsalt";
        final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
        final Map<String, Object> constants = emptyMap();
        final Map<String, Object> specialConstants = emptyMap();
        final String description = "test description";
        final List<String> metaTags = emptyList();

        for (final String rule : new String[] {"lang == 'en'", "${lang == 'en'}"}) {
            final Allocation allocation = new Allocation(rule, singletonList(range));
            final TestDefinition testDefinition =
                    new TestDefinition(
                            version,
                            rule,
                            testType,
                            salt,
                            buckets,
                            singletonList(allocation),
                            false,
                            constants,
                            specialConstants,
                            description,
                            metaTags);

            final ConsumableTestDefinition ctd =
                    ConsumableTestDefinition.fromTestDefinition(testDefinition);
            assertEquals(
                    String.format(
                            "TestDefinition rule '%s' should convert to a ${lang == 'en'} ConsumableTestDefinition.rule",
                            rule),
                    "${lang == 'en'}",
                    ctd.getRule());

            assertEquals(1, ctd.getAllocations().size());
            final Allocation ctdAllocation = ctd.getAllocations().get(0);
            assertEquals(
                    String.format(
                            "Allocation rule '%s' should convert to a ${lang == 'en'} ConsumableTestDefinition.Allocation.rule",
                            rule),
                    "${lang == 'en'}",
                    ctdAllocation.getRule());
            assertEquals(allocation.getRanges(), ctdAllocation.getRanges());
        }
    }

    /**
     * Checks that top level rules respect the special constants and support rule formats that do
     * and do not contain "${}"
     */
    @Test
    public void convertToConsumableTestDefinitionTopLevelRulesSpecialConstants() {
        // Top Level rules can optionally have the "${}" around them.
        // rule: lang == 'en'
        // rule: ${lang == 'en'}
        final Range range = new Range(0, 1.0d);

        final String version = "100";
        final TestType testType = TestType.ANONYMOUS_USER;
        final String salt = "testsalt";
        final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
        final Map<String, Object> constants = emptyMap();
        final Map<String, Object> specialConstants =
                Collections.singletonMap("__COUNTRIES", Arrays.asList("US", "CA"));
        final String description = "test description";
        final List<String> metaTags = emptyList();

        final Allocation allocation = new Allocation(null, singletonList(range));

        for (final String tdRule : new String[] {"lang == 'en'", "${lang == 'en'}"}) {
            final TestDefinition testDefinition =
                    new TestDefinition(
                            version,
                            tdRule,
                            testType,
                            salt,
                            buckets,
                            singletonList(allocation),
                            false,
                            constants,
                            specialConstants,
                            description,
                            metaTags);

            final ConsumableTestDefinition ctd =
                    ConsumableTestDefinition.fromTestDefinition(testDefinition);
            assertEquals(
                    String.format(
                            "TestDefinition rule '%s' should convert to ${proctor:contains(__COUNTRIES, country) && lang == 'en'} ConsumableTestDefinition.rule",
                            tdRule),
                    "${proctor:contains(__COUNTRIES, country) && lang == 'en'}",
                    ctd.getRule());
            assertEquals(1, ctd.getConstants().size());
            assertEquals(
                    "special constants should be added to constants",
                    Arrays.asList("US", "CA"),
                    ctd.getConstants().get("__COUNTRIES"));
        }
    }

    @Test
    public void testRemoveElExpressionBraces() {
        assertNull(removeElExpressionBraces(""));
        assertNull(removeElExpressionBraces(" "));
        assertNull(removeElExpressionBraces(null));
        assertNull(removeElExpressionBraces("\t"));
        assertNull(removeElExpressionBraces(" ${} "));
        assertNull(removeElExpressionBraces(" ${ } "));
        assertEquals("a", removeElExpressionBraces("${a}"));
        assertEquals("a", removeElExpressionBraces(" ${a} "));
        assertEquals("a", removeElExpressionBraces(" ${ a } "));
        assertEquals("a", removeElExpressionBraces(" ${ a}"));
        assertEquals("a", removeElExpressionBraces("${a } "));
        assertEquals("a", removeElExpressionBraces(" a "));
        assertEquals("lang == 'en'", removeElExpressionBraces("lang == 'en'"));
        assertEquals("lang == 'en'", removeElExpressionBraces("${lang == 'en'}"));
        // whitespace should be trimmed
        assertEquals("lang == 'en'", removeElExpressionBraces("${ lang == 'en' }"));
        // whitespace should be removed around braces
        assertEquals("lang == 'en'", removeElExpressionBraces(" ${ lang == 'en' } "));
        // only single level of braces are removed
        assertEquals("${lang == 'en'}", removeElExpressionBraces("${${lang == 'en'}}"));
        // mis matched braces are not handled
        assertEquals("${lang == 'en'", removeElExpressionBraces("${lang == 'en'"));
        // mis matched braces are not handled
        assertEquals("lang == 'en'}", removeElExpressionBraces("lang == 'en'}"));
    }

    @Test
    public void testIsEmptyElExpression() {
        assertTrue(isEmptyElExpression(""));
        assertTrue(isEmptyElExpression(" "));
        assertTrue(isEmptyElExpression(null));
        assertTrue(isEmptyElExpression("\t"));
        assertTrue(isEmptyElExpression(" ${} "));
        assertTrue(isEmptyElExpression(" ${ } "));

        assertFalse(isEmptyElExpression("${a}"));
        assertFalse(isEmptyElExpression(" ${a} "));
        assertFalse(isEmptyElExpression(" ${ a } "));
        assertFalse(isEmptyElExpression(" ${ a}"));
        assertFalse(isEmptyElExpression("${a } "));
        assertFalse(isEmptyElExpression(" a "));
        assertFalse(isEmptyElExpression("lang == 'en'"));
        assertFalse(isEmptyElExpression("${lang == 'en'}"));
    }

    @Test
    public void testEmptyWhitespace() {
        assertTrue(isEmptyWhitespace(""));
        assertTrue(isEmptyWhitespace(null));
        assertTrue(isEmptyWhitespace("  "));
        assertTrue(isEmptyWhitespace("  \t"));
        assertFalse(isEmptyWhitespace(" x "));
        assertFalse(isEmptyWhitespace("/"));
    }

    @Test
    public void verifyAndConsolidateShouldTestAllocationSum() {
        final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
        final Map<String, TestSpecification> requiredTests =
                ImmutableMap.of(TEST_A, transformTestBuckets(buckets));
        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets,
                            // Allocations do not add up to 1
                            fromCompactAllocationFormat(
                                    "ruleA|-1:0.5,0:0.5,1:0.0", "-1:0.0,0:0.0,1:0.0")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // verifyAndConsolidate should not throw an error because the 'invalidbuckets' test is
            // not required.
            assertEquals(1, matrix.getTests().size());
            assertValid(
                    "invalid test not required, sum{allocations} < 1.0",
                    matrix,
                    Collections.emptyMap());
            assertEquals(
                    "non-required tests should be removed from the matrix",
                    0,
                    matrix.getTests().size());
        }
        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets,
                            // Allocations do not add up to 1
                            fromCompactAllocationFormat(
                                    "ruleA|-1:0.5,0:0.5,1:0.0", "-1:0.0,0:0.0,1:0.0")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // verifyAndConsolidate should throw an error because the 'invalidbuckets' test is
            // required.
            assertEquals(1, matrix.getTests().size());
            assertInvalid(
                    "bucket allocation sums are unchecked, sum{allocations} < 1.0",
                    matrix,
                    requiredTests);
            assertEquals(
                    "required tests should not be removed from the matrix",
                    1,
                    matrix.getTests().size());
        }
        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets,
                            // Allocations do not add up to 1
                            fromCompactAllocationFormat(
                                    "ruleA|-1:0.5,0:0.5,1:0.0", "-1:0.5,0:0.5,1:0.5")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertEquals(1, matrix.getTests().size());
            // verifyAndConsolidate should not throw an error because the 'invalidbuckets' test is
            // not required.
            assertValid(
                    "invalid test not required, sum{allocations} > 1.0",
                    matrix,
                    Collections.emptyMap());
            assertEquals(
                    "non-required tests should be removed from the matrix",
                    0,
                    matrix.getTests().size());
        }
        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets,
                            // Allocations do not add up to 1
                            fromCompactAllocationFormat(
                                    "ruleA|-1:0.5,0:0.5,1:0.0", "-1:0.5,0:0.5,1:0.5")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertEquals(1, matrix.getTests().size());
            // verifyAndConsolidate should throw an error because the 'testa' test is required.
            assertInvalid(
                    "bucket allocation sums are unchecked, sum{allocations} > 1.0",
                    matrix,
                    requiredTests);
            assertEquals(
                    "required tests should not be removed from the matrix",
                    1,
                    matrix.getTests().size());
        }
        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets,
                            // Allocations add up to 1.0
                            fromCompactAllocationFormat(
                                    "ruleA|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertEquals(1, matrix.getTests().size());
            assertValid(
                    "bucket allocation sums are unchecked, sum{allocations} == 1.0",
                    matrix,
                    Collections.emptyMap());
            assertEquals(
                    "non-required tests should be removed from the matrix",
                    0,
                    matrix.getTests().size());
        }
        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets,
                            // Allocations add up to 1.0
                            fromCompactAllocationFormat(
                                    "ruleA|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertEquals(1, matrix.getTests().size());
            assertValid(
                    "bucket allocation sums are unchecked, sum{allocations} == 1.0",
                    matrix,
                    requiredTests);
            assertEquals(
                    "required tests should not be removed from the matrix",
                    1,
                    matrix.getTests().size());
        }
    }

    @Test
    public void testELValidity_inProctorBuilderAllocationRules()
            throws IncompatibleTestMatrixException {

        // testing invalid allocation rule
        final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
        final ConsumableTestDefinition testDefInVal =
                constructDefinition(
                        buckets,
                        fromCompactAllocationFormat(
                                "${b4t#+=}|-1:0.5,0:0.5,1:0.0",
                                "-1:0.25,0:0.5,1:0.25")); // invalid EL, nonsense rule

        assertThatThrownBy(
                        () ->
                                verifyInternallyConsistentDefinition(
                                        "testELevalInval",
                                        "test el recognition - inval",
                                        testDefInVal))
                .isInstanceOf(IncompatibleTestMatrixException.class)
                .hasMessage(
                        "Invalid allocation rule in testELevalInval: Unable to evaluate rule ${b4t#+=} due to a syntax error. "
                                + "Check that your rule is in the correct format and returns a boolean. For more information "
                                + "read: https://opensource.indeedeng.io/proctor/docs/test-rules/.");

        // testing valid functions pass with proctor included functions (will throw exception if
        // can't find) and backwards compatibility
        final ConsumableTestDefinition testDefVal1 =
                constructDefinition(
                        buckets,
                        fromCompactAllocationFormat(
                                "${proctor:now()==indeed:now()}|-1:0.5,0:0.5,1:0.0",
                                "-1:0.25,0:0.5,1:0.25"));
        verifyInternallyConsistentDefinition(
                "testELevalProctor", "test el recognition", testDefVal1);
    }

    @Test
    public void testELValidity_inProctorBuilderTestRule() throws IncompatibleTestMatrixException {
        final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
        { // Testing syntax validation with a test rule
            final ConsumableTestDefinition testDefInValTestRule =
                    constructDefinition(
                            buckets,
                            fromCompactAllocationFormat(
                                    "${proctor:now()>-1}|-1:0.5,0:0.5,1:0.0",
                                    "-1:0.25,0:0.5,1:0.25"));
            testDefInValTestRule.setRule("${b4t#+=}");

            assertThatThrownBy(
                            () ->
                                    verifyInternallyConsistentDefinition(
                                            "testELevalInValTestRule",
                                            "test el recognition - inval test rule",
                                            testDefInValTestRule))
                    .isInstanceOf(IncompatibleTestMatrixException.class)
                    .hasMessage(
                            "Invalid rule in testELevalInValTestRule: Unable to evaluate rule ${b4t#+=} due to a syntax error. "
                                    + "Check that your rule is in the correct format and returns a boolean. For more information "
                                    + "read: https://opensource.indeedeng.io/proctor/docs/test-rules/.");
        }

        { // testing the test rule el function recognition with assignment instead of equals
            final ConsumableTestDefinition testDefInValTestRule =
                    constructDefinition(
                            buckets,
                            fromCompactAllocationFormat(
                                    "${true}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));
            testDefInValTestRule.setRule("${proctor:now()=indeed:now()}");

            assertThatThrownBy(
                            () ->
                                    verifyInternallyConsistentDefinition(
                                            "testELevalInValTestRule",
                                            "test el recognition - inval test rule",
                                            testDefInValTestRule))
                    .isInstanceOf(IncompatibleTestMatrixException.class)
                    .hasMessage(
                            "Invalid rule in testELevalInValTestRule: Unable to evaluate rule ${proctor:now()=indeed:now()} due to a syntax error. "
                                    + "Check that your rule is in the correct format and returns a boolean. For more information "
                                    + "read: https://opensource.indeedeng.io/proctor/docs/test-rules/.");
        }

        { // testing the test rule el function recognition
            final ConsumableTestDefinition testDefValTestRule =
                    constructDefinition(
                            buckets,
                            fromCompactAllocationFormat(
                                    "${true}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));
            testDefValTestRule.setRule("${proctor:now()==indeed:now()}");
            verifyInternallyConsistentDefinition(
                    "testELevalValTestRule",
                    "test el recognition - val test rule and functions",
                    testDefValTestRule);
        }
    }

    @Test
    public void testProvidedContextConversion() throws IncompatibleTestMatrixException {
        final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
        { // verify primitive types convert correctly
            final ConsumableTestDefinition testDef =
                    constructDefinition(
                            buckets,
                            fromCompactAllocationFormat(
                                    "${time eq ''}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));
            final Map<String, String> providedContextString = new HashMap<>();
            providedContextString.put("time", "String");
            final ProvidedContext providedContext =
                    convertContextToTestableMap(providedContextString);
            verifyInternallyConsistentDefinition(
                    "testProvidedContextConversion",
                    "test Provided Context Conversion String",
                    testDef,
                    providedContext);
            // checking to make sure it can evaluate with converted provided context
        }
        { // verify primitive types convert correctly
            final ConsumableTestDefinition testDef =
                    constructDefinition(
                            buckets,
                            fromCompactAllocationFormat(
                                    "${time eq 0}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));
            final Map<String, String> providedContextInteger = new HashMap<>();
            providedContextInteger.put("time", "int");
            final ProvidedContext providedContext =
                    convertContextToTestableMap(providedContextInteger);
            verifyInternallyConsistentDefinition(
                    "testProvidedContextConversion",
                    "test Provided Context Conversion Integer",
                    testDef,
                    providedContext);
            // checking to make sure it can evaluate with converted provided context
        }
        { // verify primitive types convert correctly
            final ConsumableTestDefinition testDef =
                    constructDefinition(
                            buckets,
                            fromCompactAllocationFormat(
                                    "${time eq ''}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));
            final Map<String, String> providedContextChar = new HashMap<>();
            providedContextChar.put("time", "char");
            final ProvidedContext providedContext =
                    convertContextToTestableMap(providedContextChar);
            verifyInternallyConsistentDefinition(
                    "testProvidedContextConversion",
                    "test Provided Context Conversion Char",
                    testDef,
                    providedContext);
            // checking to make sure it can evaluate with converted provided context
        }
        { // verify primitive types convert correctly
            final ConsumableTestDefinition testDef =
                    constructDefinition(
                            buckets,
                            fromCompactAllocationFormat(
                                    "${time}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));
            final Map<String, String> providedContextBoolean = new HashMap<>();
            providedContextBoolean.put("time", "Boolean");
            final ProvidedContext providedContext =
                    convertContextToTestableMap(providedContextBoolean);
            verifyInternallyConsistentDefinition(
                    "testProvidedContextConversion",
                    "test Provided Context Conversion Boolean",
                    testDef,
                    providedContext);
            // checking to make sure it can evaluate with converted provided context
        }
        { // verify User Defined enum Classes convert correctly
            final ConsumableTestDefinition testDef =
                    constructDefinition(
                            buckets,
                            fromCompactAllocationFormat(
                                    "${time eq 'SPADES'}|-1:0.5,0:0.5,1:0.0",
                                    "-1:0.25,0:0.5,1:0.25"));
            final Map<String, String> providedContextClass = new HashMap<>();
            providedContextClass.put("time", "com.indeed.proctor.common.TestEnumType");
            final ProvidedContext providedContext =
                    convertContextToTestableMap(providedContextClass);
            verifyInternallyConsistentDefinition(
                    "testProvidedContextConversion",
                    "test Provided Context Conversion Class",
                    testDef,
                    providedContext);
            // checking to make sure it can evaluate with converted provided context
        }
        { // verify enums are actually used and an error is thrown with a nonexistent constant
            final ConsumableTestDefinition testDef =
                    constructDefinition(
                            buckets,
                            fromCompactAllocationFormat(
                                    "${time eq 'SP'}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));
            final Map<String, String> providedContextClass = new HashMap<>();
            providedContextClass.put("time", "com.indeed.proctor.common.TestEnumType");
            final ProvidedContext providedContext =
                    convertContextToTestableMap(providedContextClass);

            assertThatThrownBy(
                            () ->
                                    verifyInternallyConsistentDefinition(
                                            "testProvidedContextConversion",
                                            "test Provided Context Conversion Class",
                                            testDef,
                                            providedContext))
                    .as("expected IncompatibleTestMatrixException due to nonexistent enum constant")
                    .isInstanceOf(IncompatibleTestMatrixException.class)
                    .hasMessage(
                            "Invalid allocation rule in testProvidedContextConversion: "
                                    + "Failed to evaluate a rule ${time eq 'SP'}: "
                                    + "Cannot convert [SP] of type [class java.lang.String] to [class com.indeed.proctor.common.TestEnumType]");
        }
        { // verify class names are verified correctly
            final Map<String, String> providedContextBadClass = new HashMap<>();
            providedContextBadClass.put("time", "com.indeed.proctor.common.TestRulesCla");
            final ProvidedContext providedContext =
                    convertContextToTestableMap(providedContextBadClass);
            assertTrue(providedContext.shouldEvaluate());
            assertEquals(Sets.newHashSet("time"), providedContext.getUninstantiatedIdentifiers());
            /* Class was not found, add 'time' to uninstantiated identifiers */
        }
        { // Verify rule when unable to instantiate class
            final ConsumableTestDefinition testDef =
                    constructDefinition(
                            buckets,
                            fromCompactAllocationFormat(
                                    "${time.yes eq 'SP'}|-1:0.5,0:0.5,1:0.0",
                                    "-1:0.25,0:0.5,1:0.25"));
            final Map<String, String> providedContextNoConstructor = new HashMap<>();
            providedContextNoConstructor.put(
                    "time", "com.indeed.proctor.common.AbstractProctorLoader");
            final ProvidedContext providedContext =
                    convertContextToTestableMap(providedContextNoConstructor);
            assertTrue(providedContext.shouldEvaluate());
            verifyInternallyConsistentDefinition(
                    "testProvidedContextConversion",
                    "test Provided Context Conversion Class",
                    testDef,
                    providedContext);
            /* Should ignore checking failure because time was not instantiated */
        }
        { // Verify rule when class not found
            final ConsumableTestDefinition testDef =
                    constructDefinition(
                            buckets,
                            fromCompactAllocationFormat(
                                    "${time.yes eq 'SP'}|-1:0.5,0:0.5,1:0.0",
                                    "-1:0.25,0:0.5,1:0.25"));
            final Map<String, String> providedContextNoClass = new HashMap<>();
            providedContextNoClass.put("time", "com.indeed.proctor.common.NotFoundClass");
            final ProvidedContext providedContext =
                    convertContextToTestableMap(providedContextNoClass);
            assertTrue(providedContext.shouldEvaluate());
            verifyInternallyConsistentDefinition(
                    "testProvidedContextConversion",
                    "test Provided Context Conversion Class",
                    testDef,
                    providedContext);
            /* Should ignore checking failure because time was not instantiated */
        }
        {
            final ConsumableTestDefinition testDef =
                    constructDefinition(
                            fromCompactBucketFormat(
                                    "inactive:-1,control:0,test:1",
                                    Payload.EMPTY_PAYLOAD,
                                    Payload.EMPTY_PAYLOAD,
                                    Payload.EMPTY_PAYLOAD),
                            fromCompactAllocationFormat(
                                    "${time.yes eq 'SP'}|-1:0.5,0:0.5,1:0.0",
                                    "-1:0.25,0:0.5,1:0.25"));
            final Map<String, String> providedContextNoClass = new HashMap<>();
            providedContextNoClass.put("time", "com.indeed.proctor.common.NotFoundClass");
            final ProvidedContext providedContext =
                    convertContextToTestableMap(providedContextNoClass);
            assertTrue(providedContext.shouldEvaluate());
            verifyInternallyConsistentDefinition(
                    "testProvidedContextConversion",
                    "test Provided Context Conversion Class",
                    testDef,
                    providedContext);
        }
    }

    @Test
    public void testELValidity_atTestMatrixLoadTime()
            throws IncompatibleTestMatrixException, IOException {
        final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
        {
            // testing recognition of test constants
            final ConsumableTestDefinition testDefValConstants =
                    constructDefinition(
                            buckets,
                            fromCompactAllocationFormat(
                                    "${proctor:now()>time}|-1:0.5,0:0.5,1:0.0",
                                    "-1:0.25,0:0.5,1:0.25"));
            final Map<String, Object> providedConstantsVal = new HashMap<>();
            providedConstantsVal.put("time", "1");
            testDefValConstants.setConstants(providedConstantsVal);
            final Map<String, String> providedContext = emptyMap();
            verifyInternallyConsistentDefinition(
                    "testELevalwithcontext",
                    "test context recognition",
                    testDefValConstants,
                    convertContextToTestableMap(providedContext));
        }
        { // test if the providedContext is read in correctly
            final ConsumableTestDefinition testDefValConstants2 =
                    constructDefinition(
                            buckets,
                            fromCompactAllocationFormat(
                                    "${proctor:now()>time}|-1:0.5,0:0.5,1:0.0",
                                    "-1:0.25,0:0.5,1:0.25"));
            final ObjectMapper objectMapper = new ObjectMapper();
            final ProctorSpecification spec =
                    objectMapper.readValue(
                            getClass().getResourceAsStream("no-context-specification.json"),
                            ProctorSpecification.class);
            final Map<String, String> providedContext2 =
                    spec.getProvidedContext(); // needs to read in empty provided context as
            // Collections.emptyMap() and not null

            assertThatThrownBy(
                            () ->
                                    verifyInternallyConsistentDefinition(
                                            "testELevalwithcontext",
                                            "test context recognition",
                                            testDefValConstants2,
                                            convertContextToTestableMap(providedContext2)))
                    .isInstanceOf(IncompatibleTestMatrixException.class)
                    .hasMessage(
                            "Invalid allocation rule in testELevalwithcontext: "
                                    + "The variable time is defined in rule ${proctor:now()>time}, however it is not defined in the application's "
                                    + "test specification. Add the variable to your application's providedContext.json or remove it from the rule, "
                                    + "or if the application should not load your test report the issue to the Proctor team.");
        }
        { // test that an error is thrown with missing providedContext
            final ConsumableTestDefinition testDef =
                    constructDefinition(
                            buckets,
                            fromCompactAllocationFormat(
                                    "${time eq ''}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));

            assertThatThrownBy(
                            () ->
                                    verifyInternallyConsistentDefinition(
                                            "testProvidedContextMissing",
                                            "test Provided Context Missing",
                                            testDef,
                                            EMPTY_CONTEXT))
                    .as("expected IncompatibleTestMatrixException due to missing provided Context")
                    .isInstanceOf(IncompatibleTestMatrixException.class)
                    .hasMessage(
                            "Invalid allocation rule in testProvidedContextMissing: "
                                    + "The variable time is defined in rule ${time eq ''}, however it is not defined in the "
                                    + "application's test specification. Add the variable to your application's providedContext.json or "
                                    + "remove it from the rule, or if the application should not load your test report the issue to the Proctor team.");
        }
        { // testing recognition of providedContext in testRule
            final Map<String, String> providedContextVal = new HashMap<>();
            providedContextVal.put("time", "Integer");
            final ConsumableTestDefinition testDefValContextTestRule =
                    constructDefinition(
                            buckets,
                            fromCompactAllocationFormat(
                                    "${proctor:now()>-1}|-1:0.5,0:0.5,1:0.0",
                                    "-1:0.25,0:0.5,1:0.25"));
            testDefValContextTestRule.setRule("${proctor:now()>time}");
            verifyInternallyConsistentDefinition(
                    "testELevalwithcontext",
                    "test context recognition in test rule",
                    testDefValContextTestRule,
                    convertContextToTestableMap(providedContextVal));
        }
        { // testing that invalid properties are recognized
            final ConsumableTestDefinition testDef =
                    constructDefinition(
                            buckets,
                            fromCompactAllocationFormat(
                                    "${ua.iPad}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));
            final Map<String, String> providedContextClass = new HashMap<>();
            providedContextClass.put("ua", "com.indeed.proctor.common.TestRulesClass");
            final ProvidedContext providedContext =
                    convertContextToTestableMap(providedContextClass);
            assertThatThrownBy(
                            () ->
                                    verifyInternallyConsistentDefinition(
                                            "testProvidedContextConversion",
                                            "test Provided Context Conversion Class",
                                            testDef,
                                            providedContext))
                    .as("expected IncompatibleTestMatrixException due to missing attribute")
                    .isInstanceOf(IncompatibleTestMatrixException.class)
                    .hasMessage(
                            "Invalid allocation rule in testProvidedContextConversion: "
                                    + "Failed to evaluate a rule ${ua.iPad}: "
                                    + "Property [iPad] not found on type [com.indeed.proctor.common.TestRulesClass]");
        }
        { // testing that valid properties are recognized
            final ConsumableTestDefinition testDef =
                    constructDefinition(
                            buckets,
                            fromCompactAllocationFormat(
                                    "${ua.IPad}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));
            final Map<String, String> providedContextClass = new HashMap<>();
            providedContextClass.put("ua", "com.indeed.proctor.common.TestRulesClass");
            final ProvidedContext providedContext =
                    convertContextToTestableMap(providedContextClass);
            verifyInternallyConsistentDefinition(
                    "testProvidedContextConversion",
                    "test Provided Context Conversion Class",
                    testDef,
                    providedContext);
        }
        { // testing that invalid functions are recognized
            final ConsumableTestDefinition testDef =
                    constructDefinition(
                            buckets,
                            fromCompactAllocationFormat(
                                    "${proctor:notafunction()}|-1:0.5,0:0.5,1:0.0",
                                    "-1:0.25,0:0.5,1:0.25"));

            assertThatThrownBy(
                            () ->
                                    verifyInternallyConsistentDefinition(
                                            "testProvidedContextConversion",
                                            "test Provided Context Conversion Class",
                                            testDef,
                                            EMPTY_CONTEXT))
                    .as("expected IncompatibleTestMatrixException due to missing function")
                    .isInstanceOf(IncompatibleTestMatrixException.class)
                    .hasMessage(
                            "Invalid allocation rule in testProvidedContextConversion: Unable to evaluate rule ${proctor:notafunction()} due to a syntax error. "
                                    + "Check that your rule is in the correct format and returns a boolean. For more information "
                                    + "read: https://opensource.indeedeng.io/proctor/docs/test-rules/.");
        }
    }

    @Test
    public void testVerify_MapPayloadHasNullSchema() {
        final List<TestBucket> buckets =
                fromCompactBucketFormat("control:0", new Payload(ImmutableMap.of("val1", 1.0)));

        final Map<String, TestSpecification> requiredTests =
                ImmutableMap.of(
                        TEST_A,
                        transformTestBuckets(
                                buckets, PayloadType.MAP, /* schema */ null, /* validator */ null));
        final Map<String, ConsumableTestDefinition> tests =
                ImmutableMap.of(
                        TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("0:1")));

        final ProctorLoadResult result =
                ProctorUtils.verify(
                        constructArtifact(tests),
                        "[ testcase: schema is null ]",
                        requiredTests,
                        RuleEvaluator.FUNCTION_MAPPER,
                        convertContextToTestableMap(emptyMap()),
                        Collections.emptySet());
        assertEquals(ImmutableSet.of(TEST_A), result.getTestsWithErrors());
        assertEquals(
                IncompatibleTestMatrixException.class,
                result.getTestErrorMap().get(TEST_A).getClass());
        assertEquals(
                "The bucket definition of test testA has no payload, but the application is expecting one. Add a payload "
                        + "to your test definition, or if there should not be one, remove it from the application's Proctor "
                        + "specification. You can copy the Proctor specification from the specification tab for the test "
                        + "on Proctor Webapp and add it to the application's json file that contains the test specification.",
                result.getTestErrorMap().get(TEST_A).getMessage());
    }

    @Test
    public void verifyAndConsolidateShouldFailIfMissingDefaultAllocation() {
        final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
        final Map<String, TestSpecification> requiredTests =
                ImmutableMap.of(TEST_A, transformTestBuckets(buckets));
        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets,
                            // Allocations all have rules
                            fromCompactAllocationFormat(
                                    "ruleA|-1:0.0,0:0.0,1:1.0", "ruleB|-1:0.5,0:0.5,1:0.0")));

            assertValid(
                    "test missing empty rule is not required",
                    constructArtifact(tests),
                    Collections.emptyMap());
            assertMissing(
                    "test missing empty rule is required",
                    constructArtifact(Collections.emptyMap()),
                    requiredTests);
        }
        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets,
                            // non-final allocation lacks a non-empty rule
                            fromCompactAllocationFormat(
                                    "|-1:0.0,0:0.0,1:1.0", "-1:0.5,0:0.5,1:0.0")));

            assertInvalid(
                    "non-final rule lacks non-empty rule", constructArtifact(tests), requiredTests);
            assertValid(
                    "non-final rule lacks non-empty rule is allowed when not required",
                    constructArtifact(tests),
                    Collections.emptyMap());
        }
        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets,
                            fromCompactAllocationFormat(
                                    "ruleA|-1:0.0,0:0.0,1:1.0", "|-1:0.5,0:0.5,1:0.0")));

            assertValid(
                    "allocation with '' rule is valid for final last allocation",
                    constructArtifact(tests),
                    requiredTests);
        }
        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets,
                            fromCompactAllocationFormat(
                                    "ruleA|-1:0.0,0:0.0,1:1.0", "ruleB|-1:0.5,0:0.5,1:0.0")));

            assertValid(
                    "allocation with non-empty rule is valid for final last allocation",
                    constructArtifact(tests),
                    requiredTests);
        }
        // NOTE: the two test below illustrate current behavior.
        // The "${}" rule is treated as non-empty for validation.
        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets,
                            // non-final allocation lacks a non-empty rule
                            fromCompactAllocationFormat(
                                    "${}|-1:0.0,0:0.0,1:1.0", "-1:0.5,0:0.5,1:0.0")));

            assertInvalid(
                    "non-final rule of '${}' should be treated as empty rule",
                    constructArtifact(tests),
                    requiredTests);
        }
        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("${}|-1:0.5,0:0.5,1:0.0")));

            assertValid(
                    "allocation with '${}' rule is valid for final last allocation",
                    constructArtifact(tests),
                    requiredTests);
        }
    }

    @Test
    public void verifyAndConsolidateShouldFailIfNoAllocations() {
        final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");

        final Map<String, TestSpecification> requiredTests =
                ImmutableMap.of(TEST_A, transformTestBuckets(buckets));
        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            tests.put(TEST_A, constructDefinition(buckets, emptyList()));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertValid("test missing allocations is not required", matrix, Collections.emptyMap());
        }
        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            tests.put(TEST_A, constructDefinition(buckets, emptyList()));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertInvalid("test missing allocations is required", matrix, requiredTests);
        }
    }

    @Test
    public void verifyAndConsolidateShouldFailIfDuplicatedBucketIsGiven() {
        final List<TestBucket> buckets = fromCompactBucketFormat("zero:0,one:1");
        final List<TestBucket> bucketsInSpec =
                fromCompactBucketFormat("zero:0,one:1,another_one:1"); // using same bucket value 1
        final Map<String, TestSpecification> requiredTests =
                ImmutableMap.of(TEST_A, transformTestBuckets(bucketsInSpec));
        final Map<String, ConsumableTestDefinition> tests =
                ImmutableMap.of(
                        TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("0:1.0")));
        final TestMatrixArtifact matrix = constructArtifact(tests);

        assertEquals(1, matrix.getTests().size());
        assertInvalid(
                "duplicate bucket value in spec should cause invalid error", matrix, requiredTests);
    }

    @Test
    public void unknownBucketWithAllocationGreaterThanZero() {
        // The test-matrix has 3 buckets
        final List<TestBucket> buckets = fromCompactBucketFormat("zero:0,one:1,two:2");
        // The proctor-specification only knows about two of the buckets
        final TestSpecification testSpecification =
                transformTestBuckets(fromCompactBucketFormat("zero:0,one:1"));
        final Map<String, TestSpecification> requiredTests =
                ImmutableMap.of(TEST_A, testSpecification);

        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            // Allocation of bucketValue=2 is > 0
            final ConsumableTestDefinition testDefinition =
                    constructDefinition(buckets, fromCompactAllocationFormat("0:0,1:0,2:1.0"));
            tests.put(TEST_A, testDefinition);

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // externally inconsistent matrix
            assertInvalid(
                    "allocation for externally unknown bucket (two) > 0", matrix, requiredTests);
            assertEquals(
                    "trivially expected only one test in the matrix", 1, matrix.getTests().size());
            final List<Allocation> allocations =
                    matrix.getTests().values().iterator().next().getAllocations();
            assertEquals(
                    "trivially expected only one allocation in the test (same as source)",
                    1,
                    allocations.size());
            final List<Range> ranges = allocations.iterator().next().getRanges();
            assertEquals(
                    "Expected the ranges to be reduced from 3 to 1, since only the fallback value is now present",
                    1,
                    ranges.size());
            final Range onlyRange = ranges.iterator().next();
            assertEquals(
                    "Should have adopted the fallback value from the test spec",
                    onlyRange.getBucketValue(),
                    testSpecification.getFallbackValue());
            assertEquals(
                    "Trivially should have been set to 100% fallback",
                    1.0, onlyRange.getLength(), 0.005);
        }
        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            // Allocation of bucketValue=2 is == 0
            tests.put(
                    TEST_A,
                    constructDefinition(buckets, fromCompactAllocationFormat("0:0.5,1:0.5,2:0")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertValid(
                    "allocation for externally unknown bucket (two) == 0", matrix, requiredTests);
        }
    }

    @Test
    public void noBucketsSpecified() {
        // The test-matrix has 3 buckets
        final List<TestBucket> buckets = fromCompactBucketFormat("zero:0,one:1,two:2");
        // The proctor-specification does not specify any buckets
        final TestSpecification testSpecification = transformTestBuckets(emptyList());
        final Map<String, TestSpecification> requiredTests =
                ImmutableMap.of(TEST_A, testSpecification);

        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            // Allocation of bucketValue=2 is > 0
            final ConsumableTestDefinition testDefinition =
                    constructDefinition(buckets, fromCompactAllocationFormat("0:0,1:0,2:1.0"));
            tests.put(TEST_A, testDefinition);

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertValid(
                    "allocation for externally unknown bucket (two) > 0 when no buckets are specified",
                    matrix,
                    requiredTests);
        }
    }

    @Test
    public void internallyUnknownBucketWithAllocationGreaterThanZero() {
        // The test-matrix has 3 buckets
        final List<TestBucket> buckets = fromCompactBucketFormat("zero:0,one:1,two:2");
        final Map<String, TestSpecification> requiredTests =
                ImmutableMap.of(TEST_A, transformTestBuckets(buckets));

        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            // Allocation has 4 buckets, bucket with non-zero allocation in an unknown bucket
            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("0:0,1:0,2:0.5,3:0.5")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally inconsistent matrix
            assertInvalid(
                    "allocation for internally unknown bucket (three) > 0", matrix, requiredTests);
        }
        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            // There is an unknown bucket with non-zero allocation
            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("0:0,1:0,2:0.5,3:0.5")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally inconsistent matrix
            assertInvalid(
                    "allocation for internally unknown bucket (three) == 0", matrix, requiredTests);
        }
    }

    @Test
    public void requiredTestBucketsMissing() {
        // The test-matrix has fewer buckets than the required tests
        final List<TestBucket> buckets_matrix = fromCompactBucketFormat("zero:0,one:1");
        final List<TestBucket> buckets_required =
                fromCompactBucketFormat("zero:0,one:1,two:2,three:3");
        final Map<String, TestSpecification> requiredTests =
                ImmutableMap.of(TEST_A, transformTestBuckets(buckets_required));

        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            tests.put(
                    TEST_A,
                    constructDefinition(buckets_matrix, fromCompactAllocationFormat("0:0,1:1.0")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally inconsistent matrix
            assertValid("test-matrix has a subset of required buckets", matrix, requiredTests);
        }
    }

    @Test
    public void bucketsNameAndValuesShouldBeConsistent() {
        {
            final Map<String, TestSpecification> requiredTests =
                    ImmutableMap.of(
                            TEST_A, transformTestBuckets(fromCompactBucketFormat("zero:0,one:1")));
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            // The Bucket Names and Values intentionally do not match
            tests.put(
                    TEST_A,
                    constructDefinition(
                            fromCompactBucketFormat("zero:1,one:0"),
                            fromCompactAllocationFormat("0:0,1:1.0")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertValid(
                    "test-matrix has a different {bucketValue} -> {bucketName} mapping than required Tests",
                    matrix,
                    requiredTests);
        }
        {
            final Map<String, TestSpecification> requiredTests =
                    ImmutableMap.of(
                            TEST_A,
                            transformTestBuckets(fromCompactBucketFormat("zero:0,one:1,two:2")));
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            // The Bucket Names and Values intentionally do not match
            tests.put(
                    TEST_A,
                    constructDefinition(
                            fromCompactBucketFormat("zero:0,one:2"),
                            fromCompactAllocationFormat("0:0,2:1.0")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertValid(
                    "test-matrix has a different {bucketValue} -> {bucketName} mapping than required Tests",
                    matrix,
                    requiredTests);
        }
    }

    @Test
    public void requiredTestIsMissing() {
        // The test-matrix has 3 buckets
        final List<TestBucket> buckets_A = fromCompactBucketFormat("zero:0,one:1,two:2");
        final TestSpecification testSpecA = transformTestBuckets(buckets_A);

        final List<TestBucket> buckets_B = fromCompactBucketFormat("foo:0,bar:1");
        final TestSpecification testSpecB = transformTestBuckets(buckets_B);
        testSpecB.setFallbackValue(-2); // unusual value;

        final Map<String, TestSpecification> requiredTests =
                ImmutableMap.of(TEST_A, testSpecA, TEST_B, testSpecB);

        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            tests.put(
                    TEST_A,
                    constructDefinition(buckets_A, fromCompactAllocationFormat("0:0,1:0.5,2:0.5")));

            // Artifact only has 1 of the 2 required tests
            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally inconsistent matrix
            assertNull(
                    "missing testB should not be present prior to consolidation",
                    matrix.getTests().get(TEST_B));
            assertMissing("required testB is missing from the test matrix", matrix, requiredTests);

            final ConsumableTestDefinition consolidatedTestB = matrix.getTests().get(TEST_B);
            assertNotNull(
                    "autogenerated testB definition missing from consolidated matrix",
                    consolidatedTestB);
            assertEquals(
                    "autogenerated testB definition should have used custom fallback value",
                    testSpecB.getFallbackValue(),
                    consolidatedTestB.getAllocations().get(0).getRanges().get(0).getBucketValue());
        }
        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            tests.put(
                    TEST_A,
                    constructDefinition(buckets_A, fromCompactAllocationFormat("0:0,1:0.5,2:0.5")));
            tests.put(
                    TEST_B,
                    constructDefinition(buckets_B, fromCompactAllocationFormat("0:0.5,1:0.5")));

            // Artifact both of the required tests
            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid("both required tests are present in the matrix", matrix, requiredTests);
        }
        {
            final Map<String, TestSpecification> only_TestA_Required =
                    ImmutableMap.of(TEST_A, transformTestBuckets(buckets_A));
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            tests.put(
                    TEST_A,
                    constructDefinition(buckets_A, fromCompactAllocationFormat("0:0,1:0.5,2:0.5")));
            // Intentionally making the non-required test B allocation sum to 0.5
            tests.put(
                    TEST_B,
                    constructDefinition(buckets_B, fromCompactAllocationFormat("0:0,1:0.5")));

            // Artifact both of the required tests
            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertEquals(2, matrix.getTests().size());
            assertValid("required test A is present in the matrix", matrix, only_TestA_Required);

            assertEquals(
                    "Only required test A should remain in the matrix",
                    1,
                    matrix.getTests().size());
            assertTrue(
                    "Only required test A should remain in the matrix",
                    matrix.getTests().containsKey(TEST_A));
            assertFalse(
                    "Only required test A should remain in the matrix",
                    matrix.getTests().containsKey(TEST_B));
        }
    }

    @Test
    public void verifyBucketPayloads() {
        {
            final List<TestBucket> buckets =
                    fromCompactBucketFormat(
                            "inactive:-1,control:0,test:1",
                            new Payload(-1L),
                            new Payload(1L),
                            new Payload(1L));

            final Map<String, TestSpecification> requiredTests =
                    ImmutableMap.of(
                            TEST_A, transformTestBuckets(buckets, PayloadType.LONG_VALUE, null));
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();

            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid("all payloads of the same type", matrix, requiredTests);
        }
        {
            final List<TestBucket> buckets =
                    fromCompactBucketFormat("inactive:-1,control:0,test:1");
            setPayload(buckets, 0, new Payload(-1L));
            // bucket 1 is missing a payload here.
            setPayload(buckets, 2, new Payload(1L));

            final Map<String, TestSpecification> requiredTests =
                    ImmutableMap.of(
                            TEST_A, transformTestBuckets(buckets, PayloadType.LONG_VALUE, null));
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();

            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid("not all payloads of the test defined", matrix, requiredTests);
        }
        {
            final List<TestBucket> buckets =
                    fromCompactBucketFormat(
                            "inactive:-1,control:0,test:1",
                            new Payload("inact"),
                            new Payload("foo"),
                            new Payload("bar"));

            final Map<String, TestSpecification> requiredTests =
                    ImmutableMap.of(
                            TEST_A, transformTestBuckets(buckets, PayloadType.LONG_VALUE, null));
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();

            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid("all payloads of the wrong type", matrix, requiredTests);
        }
        {
            final List<TestBucket> buckets =
                    fromCompactBucketFormat(
                            "inactive:-1,control:0,test:1",
                            new Payload(-1L),
                            new Payload(0L),
                            new Payload("foo"));

            final Map<String, TestSpecification> requiredTests =
                    ImmutableMap.of(
                            TEST_A, transformTestBuckets(buckets, PayloadType.LONG_VALUE, null));
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();

            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally inconsistent matrix: different payload types in test
            assertInvalid("all payloads not of the same type", matrix, requiredTests);
        }
        {
            final List<TestBucket> buckets =
                    fromCompactBucketFormat(
                            "inactive:-1,control:0,test:1",
                            // empty arrays are allowed.
                            new Payload(new String[] {}),
                            new Payload(new String[] {"foo", "bar"}),
                            new Payload(new String[] {"baz", "quux", "xyzzy"}));

            final Map<String, TestSpecification> requiredTests =
                    ImmutableMap.of(
                            TEST_A, transformTestBuckets(buckets, PayloadType.STRING_ARRAY, null));
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();

            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid("vector payloads can be different lengths", matrix, requiredTests);
        }
        {
            final List<TestBucket> buckets =
                    fromCompactBucketFormat(
                            "inactive:-1,control:0,test:1",
                            new Payload(
                                    ImmutableMap.of(
                                            "val1",
                                            1.0,
                                            "val2",
                                            "one",
                                            "val3",
                                            new ArrayList<String>())),
                            new Payload(
                                    ImmutableMap.of(
                                            "val1",
                                            2.0,
                                            "val2",
                                            "tw",
                                            "val3",
                                            new ArrayList<String>() {
                                                {
                                                    add("a");
                                                    add("c");
                                                }
                                            })),
                            new Payload(
                                    ImmutableMap.of(
                                            "val1",
                                            2.0,
                                            "val2",
                                            "th",
                                            "val3",
                                            new ArrayList<String>() {
                                                {
                                                    add("foo");
                                                    add("bar");
                                                }
                                            })));

            final Map<String, TestSpecification> requiredTests =
                    ImmutableMap.of(
                            TEST_A,
                            transformTestBuckets(
                                    buckets,
                                    PayloadType.MAP,
                                    ImmutableMap.of(
                                            "val1",
                                            PayloadType.DOUBLE_VALUE,
                                            "val2",
                                            PayloadType.STRING_VALUE,
                                            "val3",
                                            PayloadType.STRING_ARRAY),
                                    null));
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();

            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertValid(
                    "correct allocation and object, map with double values", matrix, requiredTests);
        }
        {
            final List<TestBucket> buckets =
                    fromCompactBucketFormat(
                            "inactive:-1,control:0,test:1",
                            new Payload(ImmutableMap.of("val1", 1.0, "val2", 3.0, "val3", 1.0)),
                            new Payload(ImmutableMap.of("val1", 2.0, "val3", 1.0, "val4", 3.0)),
                            new Payload(ImmutableMap.of("val1", 2.0, "val2", 2.0, "val3", 2.0)));
            final Map<String, TestSpecification> requiredTests =
                    ImmutableMap.of(
                            TEST_A,
                            transformTestBuckets(
                                    buckets,
                                    PayloadType.MAP,
                                    ImmutableMap.of(
                                            "val1",
                                            PayloadType.DOUBLE_VALUE,
                                            "val2",
                                            PayloadType.DOUBLE_VALUE,
                                            "val3",
                                            PayloadType.DOUBLE_VALUE),
                                    null));

            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();

            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid(
                    "map payloads can't have different variable names", matrix, requiredTests);
        }
        {
            final List<TestBucket> buckets =
                    fromCompactBucketFormat(
                            "inactive:-1,control:0,test:1",
                            new Payload(ImmutableMap.of("val1", 1.0, "val2", "yea1", "val3", 1.0)),
                            new Payload(ImmutableMap.of("val1", 2.0, "val2", "yea2", "val3", 3.0)),
                            new Payload(ImmutableMap.of("val1", 2.0, "val2", "yea3", "val3", 2.0)));

            final Map<String, TestSpecification> requiredTests =
                    ImmutableMap.of(
                            TEST_A,
                            transformTestBuckets(
                                    buckets,
                                    PayloadType.MAP,
                                    ImmutableMap.of(
                                            "val1",
                                            PayloadType.DOUBLE_VALUE,
                                            "val2",
                                            PayloadType.DOUBLE_VALUE,
                                            "val3",
                                            PayloadType.DOUBLE_VALUE),
                                    null));
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();

            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid(
                    "map payloads can't have different variable types than specified",
                    matrix,
                    requiredTests);
        }
        {
            final List<TestBucket> buckets =
                    fromCompactBucketFormat(
                            "inactive:-1,control:0,test:1",
                            new Payload(ImmutableMap.of("val1", 1.0, "val2", 3.0, "val3", 1.0)),
                            new Payload(ImmutableMap.of("val1", 2.0, "val2", 1.0, "val3", 3.0)),
                            new Payload(ImmutableMap.of("val1", 2.0, "val2", 2.0, "val3", 2.0)));

            final Map<String, TestSpecification> requiredTests =
                    ImmutableMap.of(
                            TEST_A,
                            transformTestBuckets(
                                    buckets,
                                    PayloadType.MAP,
                                    ImmutableMap.of(
                                            "val1",
                                            PayloadType.DOUBLE_VALUE,
                                            "val2",
                                            PayloadType.DOUBLE_VALUE,
                                            "val3",
                                            PayloadType.DOUBLE_VALUE,
                                            "val4",
                                            PayloadType.DOUBLE_ARRAY),
                                    null));
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();

            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid(
                    "map payloads can't have less variable types than specified",
                    matrix,
                    requiredTests);
        }
        {
            final List<TestBucket> buckets =
                    fromCompactBucketFormat(
                            "inactive:-1,control:0,test:1",
                            new Payload(ImmutableMap.of("val1", 1.0, "val2", 3.0, "val3", 1.0)),
                            new Payload(
                                    ImmutableMap.of(
                                            "val1",
                                            2.0,
                                            "val2",
                                            new ArrayList<Double>() {
                                                {
                                                    add(1.0D);
                                                }
                                            },
                                            "val3",
                                            3.0)),
                            new Payload(ImmutableMap.of("val1", 2.0, "val2", 2.0, "val3", 2.0)));

            final Map<String, TestSpecification> requiredTests =
                    ImmutableMap.of(
                            TEST_A,
                            transformTestBuckets(
                                    buckets,
                                    PayloadType.MAP,
                                    ImmutableMap.of(
                                            "val1",
                                            PayloadType.DOUBLE_VALUE,
                                            "val2",
                                            PayloadType.DOUBLE_VALUE,
                                            "val3",
                                            PayloadType.DOUBLE_VALUE),
                                    null));
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();

            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid(
                    "map payloads can't have different variable types than specified -- an array instead of a single value",
                    matrix,
                    requiredTests);
        }
        {
            final List<TestBucket> buckets =
                    fromCompactBucketFormat(
                            "inactive:-1,control:0,test:1",
                            new Payload(
                                    ImmutableMap.of(
                                            "val1",
                                            1.0,
                                            "val2",
                                            ImmutableMap.<String, Object>of("a", 1, "b", 2),
                                            "val3",
                                            1.0)),
                            new Payload(
                                    ImmutableMap.of(
                                            "val1",
                                            2.0,
                                            "val2",
                                            ImmutableMap.<String, Object>of("c", 3, "d", 4),
                                            "val3",
                                            3.0)),
                            new Payload(
                                    ImmutableMap.of(
                                            "val1",
                                            2.0,
                                            "val2",
                                            ImmutableMap.<String, Object>of("e", 5, "f", 6),
                                            "val3",
                                            2.0)));

            final Map<String, TestSpecification> requiredTests =
                    ImmutableMap.of(
                            TEST_A,
                            transformTestBuckets(
                                    buckets,
                                    PayloadType.MAP,
                                    ImmutableMap.of(
                                            "val1",
                                            PayloadType.DOUBLE_VALUE,
                                            "val2",
                                            PayloadType.MAP,
                                            "val3",
                                            PayloadType.DOUBLE_VALUE),
                                    null));
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();

            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid("map payloads can't nested map payloads", matrix, requiredTests);
        }
        {
            final List<TestBucket> buckets =
                    fromCompactBucketFormat(
                            "inactive:-1,control:0,test:1",
                            new Payload(
                                    ImmutableMap.of(
                                            "val1",
                                            1.0,
                                            "val2",
                                            "one",
                                            "val3",
                                            new ArrayList<String>())),
                            new Payload(
                                    ImmutableMap.of(
                                            "val1",
                                            2.0,
                                            "val2",
                                            "tw",
                                            "val3",
                                            new ArrayList<String>() {
                                                {
                                                    add("a");
                                                    add("c");
                                                }
                                            })),
                            new Payload(
                                    ImmutableMap.of(
                                            "val1",
                                            2.0,
                                            "val2",
                                            "th",
                                            "val3",
                                            new ArrayList() {
                                                {
                                                    add(2.1D);
                                                    add("bar");
                                                }
                                            })));

            final Map<String, TestSpecification> requiredTests =
                    ImmutableMap.of(
                            TEST_A,
                            transformTestBuckets(
                                    buckets,
                                    PayloadType.MAP,
                                    ImmutableMap.of(
                                            "val1",
                                            PayloadType.DOUBLE_VALUE,
                                            "val2",
                                            PayloadType.STRING_VALUE,
                                            "val3",
                                            PayloadType.STRING_ARRAY),
                                    null));
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();

            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertInvalid(
                    "map payload nested arrays can't have multiple types", matrix, requiredTests);
        }
    }

    @Test
    public void verifyBucketPayloadValueValidators() {
        {
            final List<TestBucket> buckets =
                    fromCompactBucketFormat(
                            "inactive:-1,control:0,test:1",
                            new Payload(0D),
                            new Payload(10D),
                            new Payload(20D));

            final Map<String, TestSpecification> requiredTests =
                    ImmutableMap.of(
                            TEST_A,
                            transformTestBuckets(
                                    buckets, PayloadType.DOUBLE_VALUE, "${value >= 0}"));
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();

            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid("doubleValue: all payload values pass validation", matrix, requiredTests);
        }
        {
            final List<TestBucket> buckets =
                    fromCompactBucketFormat(
                            "inactive:-1,control:0,test:1",
                            new Payload(0D),
                            new Payload(10D),
                            new Payload(-1D));

            final Map<String, TestSpecification> requiredTests =
                    ImmutableMap.of(
                            TEST_A,
                            transformTestBuckets(
                                    buckets, PayloadType.DOUBLE_VALUE, "${value >= 0}"));
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();

            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid(
                    "doubleValue: a payload value doesn't pass validation", matrix, requiredTests);
        }
        {
            final List<TestBucket> buckets =
                    fromCompactBucketFormat(
                            "inactive:-1,control:0,test:1",
                            new Payload(0L),
                            new Payload(10L),
                            new Payload(20L));

            final Map<String, TestSpecification> requiredTests =
                    ImmutableMap.of(
                            TEST_A,
                            transformTestBuckets(buckets, PayloadType.LONG_VALUE, "${value >= 0}"));
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();

            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid("longValue: all payload values pass validation", matrix, requiredTests);
        }
        {
            final List<TestBucket> buckets =
                    fromCompactBucketFormat(
                            "inactive:-1,control:0,test:1",
                            new Payload(0L),
                            new Payload(10L),
                            new Payload(-1L));

            final Map<String, TestSpecification> requiredTests =
                    ImmutableMap.of(
                            TEST_A,
                            transformTestBuckets(buckets, PayloadType.LONG_VALUE, "${value >= 0}"));
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();

            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid(
                    "longValue: a payload value doesn't pass validation", matrix, requiredTests);
        }
        {
            final List<TestBucket> buckets =
                    fromCompactBucketFormat(
                            "inactive:-1,control:0,test:1",
                            new Payload("inactive"),
                            new Payload("foo"),
                            new Payload("bar"));

            final Map<String, TestSpecification> requiredTests =
                    ImmutableMap.of(
                            TEST_A,
                            transformTestBuckets(
                                    buckets, PayloadType.STRING_VALUE, "${value >= \"b\"}"));
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();

            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid("stringValue: all payload values pass validation", matrix, requiredTests);
        }
        {
            final List<TestBucket> buckets =
                    fromCompactBucketFormat(
                            "inactive:-1,control:0,test:1",
                            new Payload("inactive"),
                            new Payload("foo"),
                            new Payload("abba"));

            final Map<String, TestSpecification> requiredTests =
                    ImmutableMap.of(
                            TEST_A,
                            transformTestBuckets(
                                    buckets, PayloadType.STRING_VALUE, "${value >= \"b\"}"));
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();

            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid(
                    "stringValue: a payload value doesn't pass validation", matrix, requiredTests);
        }
        {
            final List<TestBucket> buckets =
                    fromCompactBucketFormat(
                            "inactive:-1,control:0,test:1",
                            new Payload(ImmutableMap.of("val1", 1.0, "val2", 3.0, "val3", 1.0)),
                            new Payload(ImmutableMap.of("val1", 2.0, "val2", 4.0, "val3", 1.0)),
                            new Payload(ImmutableMap.of("val1", 2.0, "val2", 2.0, "val3", 2.0)));

            final Map<String, TestSpecification> requiredTests =
                    ImmutableMap.of(
                            TEST_A,
                            transformTestBuckets(
                                    buckets,
                                    PayloadType.MAP,
                                    ImmutableMap.of(
                                            "val1",
                                            PayloadType.DOUBLE_VALUE,
                                            "val2",
                                            PayloadType.DOUBLE_VALUE,
                                            "val3",
                                            PayloadType.DOUBLE_VALUE),
                                    "${val1 + val2 + val3 < 10}"));
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();

            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid("map: a payload value that should pass validation", matrix, requiredTests);
        }
        {
            final List<TestBucket> buckets =
                    fromCompactBucketFormat(
                            "inactive:-1,control:0,test:1",
                            new Payload(ImmutableMap.of("val1", 1.0, "val2", 3.0, "val3", 1.0)),
                            new Payload(ImmutableMap.of("val1", 2.0, "val2", 4.0, "val3", 1.0)),
                            new Payload(ImmutableMap.of("val1", 2.0, "val2", 6.0, "val3", 2.0)));

            final Map<String, TestSpecification> requiredTests =
                    ImmutableMap.of(
                            TEST_A,
                            transformTestBuckets(
                                    buckets,
                                    PayloadType.MAP,
                                    ImmutableMap.of(
                                            "val1",
                                            PayloadType.DOUBLE_VALUE,
                                            "val2",
                                            PayloadType.DOUBLE_VALUE,
                                            "val3",
                                            PayloadType.DOUBLE_VALUE),
                                    "${val1 + val2 + val3 < 10}"));
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();

            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid("map: a payload value doesn't pass validation", matrix, requiredTests);
        }
    }

    @Test
    public void verifyPayloadDeploymentScenerios() {
        {
            // Proctor should not break if it consumes a test matrix
            // that has a payload even if it's not expecting one.
            // (So long as all the payloads are of the same type.)
            final List<TestBucket> buckets =
                    fromCompactBucketFormat(
                            "inactive:-1,control:0,test:1",
                            new Payload(-1L),
                            new Payload(0L),
                            new Payload(1L));

            final Map<String, TestSpecification> requiredTests =
                    ImmutableMap.of(
                            TEST_A, transformTestBuckets(buckets)); // no payload type specified
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();

            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid(
                    "no payload expected; payloads supplied of the same type",
                    matrix,
                    requiredTests);

            // Should have gotten back a test.
            assertEquals(
                    "required tests should not be removed from the matrix",
                    1,
                    matrix.getTests().size());

            // Make sure we don't have any payloads in the resulting tests.
            for (final Entry<String, ConsumableTestDefinition> next :
                    matrix.getTests().entrySet()) {
                final ConsumableTestDefinition testDefinition = next.getValue();

                for (final TestBucket bucket : testDefinition.getBuckets()) {
                    assertNull(bucket.getPayload());
                }
            }
        }
        {
            final List<TestBucket> buckets =
                    fromCompactBucketFormat(
                            "inactive:-1,control:0,test:1",
                            new Payload(-1L),
                            new Payload(0L),
                            new Payload("foo"));

            final Map<String, TestSpecification> requiredTests =
                    ImmutableMap.of(
                            TEST_A, transformTestBuckets(buckets)); // no payload type specified
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();

            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally inconsistent matrix: different payload types in test
            assertInvalid(
                    "no payload expected; payloads supplied of different types",
                    matrix,
                    requiredTests);
        }
        {
            // Proctor should not break if it consumes a test matrix
            // with no payloads when it is expecting one.
            final List<TestBucket> buckets =
                    fromCompactBucketFormat("inactive:-1,control:0,test:1");

            final Map<String, TestSpecification> requiredTests =
                    ImmutableMap.of(
                            TEST_A, transformTestBuckets(buckets, PayloadType.LONG_VALUE, null));
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();

            tests.put(
                    TEST_A,
                    constructDefinition(
                            buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid("payload expected; no payloads supplied", matrix, requiredTests);
            // Should have gotten back a test.
            assertEquals(
                    "required tests should not be removed from the matrix",
                    1,
                    matrix.getTests().size());
        }
    }

    @Test
    public void testCompactAllocationFormat() {
        //        List<Allocation> allocations_empty = fromCompactAllocationFormat("");
        //        assertEquals(0, allocations_empty.size());
        final double DELTA = 0;

        final List<Allocation> allocations =
                fromCompactAllocationFormat("ruleA|10:0,11:0.5,12:0.5", "0:0,1:0.5,2:0.5");
        assertEquals(2, allocations.size());
        {
            final Allocation allocationA = allocations.get(0);
            assertEquals("ruleA", allocationA.getRule());
            assertEquals(3, allocationA.getRanges().size());
            assertEquals(10, allocationA.getRanges().get(0).getBucketValue());
            assertEquals(0, allocationA.getRanges().get(0).getLength(), DELTA);
            assertEquals(11, allocationA.getRanges().get(1).getBucketValue());
            assertEquals(0.5, allocationA.getRanges().get(1).getLength(), DELTA);
            assertEquals(12, allocationA.getRanges().get(2).getBucketValue());
            assertEquals(0.5, allocationA.getRanges().get(2).getLength(), DELTA);
        }
        {
            final Allocation allocationB = allocations.get(1);
            assertNull(allocationB.getRule());
            assertEquals(3, allocationB.getRanges().size());
            assertEquals(0, allocationB.getRanges().get(0).getBucketValue());
            assertEquals(0, allocationB.getRanges().get(0).getLength(), DELTA);
            assertEquals(1, allocationB.getRanges().get(1).getBucketValue());
            assertEquals(0.5, allocationB.getRanges().get(1).getLength(), DELTA);
            assertEquals(2, allocationB.getRanges().get(2).getBucketValue());
            assertEquals(0.5, allocationB.getRanges().get(2).getLength(), DELTA);
        }
    }

    @Test
    public void testCompactBucketFormatHelperMethods() {
        //        List<TestBucket> buckets_empty = fromCompactBucketFormat("");
        //        assertEquals(0, buckets_empty.size());
        final List<TestBucket> buckets = fromCompactBucketFormat("zero:0,one:1,two:2");

        assertEquals(3, buckets.size());
        assertEquals("zero", buckets.get(0).getName());
        assertEquals(0, buckets.get(0).getValue());
        assertEquals("one", buckets.get(1).getName());
        assertEquals(1, buckets.get(1).getValue());
        assertEquals("two", buckets.get(2).getName());
        assertEquals(2, buckets.get(2).getValue());
    }

    @Test
    public void testUnrecognizedTestType() throws Exception {
        final InputStream input =
                Preconditions.checkNotNull(
                        getClass().getResourceAsStream(PATH_UNKNOWN_TEST_TYPE),
                        "Missing test definition");
        final ConsumableTestDefinition test =
                Serializers.lenient().readValue(input, ConsumableTestDefinition.class);

        final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
        final Map<String, TestSpecification> requiredTests =
                ImmutableMap.of(TEST_A, transformTestBuckets(buckets));
        final Map<String, ConsumableTestDefinition> tests = ImmutableMap.of(TEST_A, test);

        {
            final TestMatrixArtifact matrix = constructArtifact(tests);

            // verifyAndConsolidate should not throw an error because the 'invalidbuckets' test is
            // not required.
            assertEquals(1, matrix.getTests().size());
            assertInvalid(
                    "Test not recognized, replaced with 'invalid' marker test",
                    matrix,
                    requiredTests);
            assertEquals(1, matrix.getTests().size());
            final ConsumableTestDefinition replacement =
                    matrix.getTests().values().iterator().next();
            assertEquals(TestType.RANDOM, replacement.getTestType());
            assertEquals(
                    "Expected buckets to expand to include all buckets from the specification",
                    buckets.size(),
                    replacement.getBuckets().size());
            assertEquals(-1, replacement.getBuckets().iterator().next().getValue());
        }

        final TestType unrecognizedTestType = TestType.register("UNRECOGNIZED");
        {
            final TestMatrixArtifact matrix = constructArtifact(tests);

            // verifyAndConsolidate should not throw an error because the 'invalidbuckets' test is
            // not required.
            assertEquals(1, matrix.getTests().size());
            final ConsumableTestDefinition original = matrix.getTests().values().iterator().next();
            assertEquals(
                    "Expected only the control and test buckets", 2, original.getBuckets().size());

            assertValid("Test now valid", matrix, requiredTests);
            assertEquals(1, matrix.getTests().size());
            final ConsumableTestDefinition stillOriginal =
                    matrix.getTests().values().iterator().next();
            assertEquals(unrecognizedTestType, stillOriginal.getTestType());
            assertEquals(
                    "Expected buckets to expand to include all buckets from the specification",
                    buckets.size(),
                    stillOriginal.getBuckets().size());
        }
    }

    @Test
    public void testVerifyAndConsolidateShouldResolveDynamicTests() {
        final String testC = "testc";
        final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
        final ConsumableTestDefinition definition =
                constructDefinition(
                        fromCompactBucketFormat("inactive:-1,control:0,test:1"),
                        fromCompactAllocationFormat("1:1.0"));
        tests.put(TEST_A, definition);
        tests.put(TEST_B, definition);
        tests.put(testC, definition);

        final TestMatrixArtifact matrix = constructArtifact(tests);

        final Map<String, TestSpecification> requiredTests =
                ImmutableMap.of(TEST_A, new TestSpecification());

        final Set<String> dynamicTests = Sets.newHashSet(TEST_B);

        final ProctorLoadResult proctorLoadResult =
                verifyAndConsolidate(
                        matrix,
                        "",
                        requiredTests,
                        RuleEvaluator.FUNCTION_MAPPER,
                        ProvidedContext.nonEvaluableContext(),
                        dynamicTests);

        assertTrue("missing tests should be empty", proctorLoadResult.getMissingTests().isEmpty());
        assertTrue(
                "invalid tests should be empty", proctorLoadResult.getTestsWithErrors().isEmpty());
        assertTrue("TEST_A should be resolved", matrix.getTests().containsKey(TEST_A));
        assertTrue("TEST_B should be resolved", matrix.getTests().containsKey(TEST_B));
        assertFalse("testC should not be resolved", matrix.getTests().containsKey(testC));
    }

    @Test
    public void testVerifyAndConsolidateShouldNotRecordInvalidDynamicTests() {
        final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
        final ConsumableTestDefinition definition =
                constructDefinition(
                        fromCompactBucketFormat("inactive:-1,control:0,test:1"),
                        singletonList(
                                new Allocation(
                                        "${unknownField==\"abc\"}",
                                        singletonList(new Range(1, 1.0)))));
        tests.put(TEST_A, definition);
        tests.put(TEST_B, definition);

        final TestMatrixArtifact matrix = constructArtifact(tests);

        final Map<String, TestSpecification> requiredTests =
                ImmutableMap.of(TEST_A, new TestSpecification());

        final Set<String> dynamicTests = Sets.newHashSet(TEST_B);

        final ProctorLoadResult proctorLoadResult =
                verifyAndConsolidate(
                        matrix,
                        "",
                        requiredTests,
                        RuleEvaluator.FUNCTION_MAPPER,
                        EMPTY_CONTEXT,
                        dynamicTests);

        assertEquals(
                "missing tests should be empty",
                Collections.emptySet(),
                proctorLoadResult.getMissingTests());
        assertEquals(
                "only required test (TEST_A) should be error",
                Sets.newHashSet(TEST_A),
                proctorLoadResult.getTestsWithErrors());
        assertEquals(
                "only required test (TEST_A) should be resolved.",
                Sets.newHashSet(TEST_A),
                matrix.getTests().keySet());
        assertEquals(
                "invalid required test (TEST_A) should contain dummy test definition.",
                TestType.RANDOM,
                matrix.getTests().get(TEST_A).getTestType());
    }

    @Test
    public void testVerifyAndConsolidateShouldNotRemovePayloadOfDynamicTests() {
        final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
        final ConsumableTestDefinition definitionA =
                constructDefinition(
                        fromCompactBucketFormat("inactive:-1,control:0,test:1"),
                        fromCompactAllocationFormat("1:1.0"));

        definitionA.setBuckets(
                definitionA.getBuckets().stream()
                        .map(
                                bucket ->
                                        TestBucket.builder()
                                                .from(bucket)
                                                .payload(new Payload((long) bucket.getValue()))
                                                .build())
                        .collect(Collectors.toList()));

        final ConsumableTestDefinition definitionB =
                constructDefinition(
                        fromCompactBucketFormat("inactive:-1,control:0,test:1"),
                        fromCompactAllocationFormat("1:1.0"));

        definitionB.setBuckets(
                definitionB.getBuckets().stream()
                        .map(
                                bucket ->
                                        TestBucket.builder()
                                                .from(bucket)
                                                .payload(new Payload((long) bucket.getValue()))
                                                .build())
                        .collect(Collectors.toList()));

        tests.put(TEST_A, definitionA);
        tests.put(TEST_B, definitionB);

        final TestMatrixArtifact matrix = constructArtifact(tests);

        final Map<String, TestSpecification> requiredTests =
                ImmutableMap.of(TEST_A, new TestSpecification());

        final Set<String> dynamicTests = Sets.newHashSet(TEST_B);

        final ProctorLoadResult proctorLoadResult =
                verifyAndConsolidate(
                        matrix,
                        "",
                        requiredTests,
                        RuleEvaluator.FUNCTION_MAPPER,
                        ProvidedContext.nonEvaluableContext(),
                        dynamicTests);

        assertTrue("missing tests should be empty", proctorLoadResult.getMissingTests().isEmpty());
        assertTrue(
                "invalid tests should be empty", proctorLoadResult.getTestsWithErrors().isEmpty());
        assertTrue("TEST_A should be resolved", matrix.getTests().containsKey(TEST_A));
        assertTrue("TEST_B should be resolved", matrix.getTests().containsKey(TEST_B));
        for (final TestBucket bucket : matrix.getTests().get(TEST_A).getBuckets()) {
            assertNull(
                    "payload should be removed if test is not dynamically resolved"
                            + " and specification don't have payload type",
                    bucket.getPayload());
        }
        for (final TestBucket bucket : matrix.getTests().get(TEST_B).getBuckets()) {
            assertNotNull(
                    "payload should NOT be removed if test is dynamically resolved",
                    bucket.getPayload().getLongValue());
        }
    }

    @Test
    public void testVerifyAndConsolidateShouldDetectCircularDependency() {
        final ConsumableTestDefinition definitionA =
                constructDefinition(
                        fromCompactBucketFormat("inactive:-1,control:0,test:1"),
                        fromCompactAllocationFormat("1:1.0"));
        final ConsumableTestDefinition definitionB =
                constructDefinition(
                        fromCompactBucketFormat("inactive:-1,control:0,test:1"),
                        fromCompactAllocationFormat("1:1.0"));
        final ConsumableTestDefinition definitionC =
                constructDefinition(
                        fromCompactBucketFormat("inactive:-1,control:0,test:1"),
                        fromCompactAllocationFormat("1:1.0"));
        definitionA.setSalt("&a");
        definitionB.setSalt("&b");
        definitionC.setSalt("&c");
        definitionA.setDependsOn(new TestDependency(TEST_B, 1));
        definitionB.setDependsOn(new TestDependency(TEST_A, 1));
        definitionC.setDependsOn(new TestDependency(TEST_B, 1));
        final Map<String, ConsumableTestDefinition> tests =
                ImmutableMap.of(
                        TEST_A, definitionA,
                        TEST_B, definitionB,
                        TEST_C, definitionC);
        final TestMatrixArtifact matrix = constructArtifact(tests);

        final ProctorLoadResult proctorLoadResult =
                verifyAndConsolidate(
                        matrix,
                        "",
                        ImmutableMap.of(TEST_A, new TestSpecification()),
                        RuleEvaluator.FUNCTION_MAPPER,
                        ProvidedContext.nonEvaluableContext(),
                        ImmutableSet.of(TEST_B, TEST_C));

        assertThat(proctorLoadResult.getTestsWithErrors()).containsExactly(TEST_A);
        assertThat(proctorLoadResult.getDynamicTestWithErrors()).containsExactly(TEST_B, TEST_C);

        assertThat(proctorLoadResult.getTestErrorMap())
                .hasEntrySatisfying(
                        TEST_A, x -> assertThat(x).hasMessageContaining("circular dependency"));
        assertThat(proctorLoadResult.getDynamicTestErrorMap())
                .hasEntrySatisfying(
                        TEST_B, x -> assertThat(x).hasMessageContaining("circular dependency"))
                .hasEntrySatisfying(
                        TEST_C, x -> assertThat(x).hasMessageContaining("circular dependency"));
    }

    @Test
    public void testVerifyAndConsolidateShouldDetectUnknownTestNameDependency() {
        final ConsumableTestDefinition definitionA =
                constructDefinition(
                        fromCompactBucketFormat("inactive:-1,control:0,test:1"),
                        fromCompactAllocationFormat("1:1.0"));
        final ConsumableTestDefinition definitionB =
                constructDefinition(
                        fromCompactBucketFormat("inactive:-1,control:0,test:1"),
                        fromCompactAllocationFormat("1:1.0"));
        definitionA.setSalt("&a");
        definitionB.setSalt("&b");
        definitionA.setDependsOn(new TestDependency("___unknown_test", 1));
        definitionB.setDependsOn(new TestDependency(TEST_A, 1));
        final Map<String, ConsumableTestDefinition> tests =
                ImmutableMap.of(
                        TEST_A, definitionA,
                        TEST_B, definitionB);
        final TestMatrixArtifact matrix = constructArtifact(tests);

        final ProctorLoadResult proctorLoadResult =
                verifyAndConsolidate(
                        matrix,
                        "",
                        ImmutableMap.of(TEST_A, new TestSpecification()),
                        RuleEvaluator.FUNCTION_MAPPER,
                        ProvidedContext.nonEvaluableContext(),
                        ImmutableSet.of(TEST_B));

        assertThat(proctorLoadResult.getTestsWithErrors()).containsExactly(TEST_A);
        assertThat(proctorLoadResult.getDynamicTestWithErrors()).containsExactly(TEST_B);

        assertThat(proctorLoadResult.getTestErrorMap())
                .hasEntrySatisfying(
                        TEST_A,
                        x ->
                                assertThat(x)
                                        .hasMessageContaining(
                                                "depends on an unknown or incompatible test")
                                        .hasMessageContaining("___unknown_test"));
        assertThat(proctorLoadResult.getDynamicTestErrorMap())
                .hasEntrySatisfying(
                        TEST_B,
                        x -> assertThat(x).hasMessageContaining("depends on an invalid test"));
    }

    @Test
    public void testVerifyAndConsolidateShouldDetectUnknownTestBucketDependency() {
        final ConsumableTestDefinition definitionA =
                constructDefinition(
                        fromCompactBucketFormat("inactive:-1,control:0,test:1"),
                        fromCompactAllocationFormat("1:1.0"));
        final ConsumableTestDefinition definitionB =
                constructDefinition(
                        fromCompactBucketFormat("inactive:-1,control:0,test:1"),
                        fromCompactAllocationFormat("1:1.0"));
        definitionA.setSalt("&a");
        definitionB.setSalt("&b");
        definitionB.setDependsOn(new TestDependency(TEST_A, 100));
        final Map<String, ConsumableTestDefinition> tests =
                ImmutableMap.of(
                        TEST_A, definitionA,
                        TEST_B, definitionB);
        final TestMatrixArtifact matrix = constructArtifact(tests);

        final ProctorLoadResult proctorLoadResult =
                verifyAndConsolidate(
                        matrix,
                        "",
                        ImmutableMap.of(TEST_A, new TestSpecification()),
                        RuleEvaluator.FUNCTION_MAPPER,
                        ProvidedContext.nonEvaluableContext(),
                        ImmutableSet.of(TEST_B));

        assertThat(proctorLoadResult.getTestsWithErrors()).isEmpty();
        assertThat(proctorLoadResult.getDynamicTestWithErrors()).containsExactly(TEST_B);

        assertThat(proctorLoadResult.getDynamicTestErrorMap())
                .hasEntrySatisfying(
                        TEST_B,
                        x -> assertThat(x).hasMessageContaining("depends on an undefined bucket"));
    }

    @Test
    public void testVerifyAndConsolidateShouldDetectDependencyWithDifferentTestType() {
        final ConsumableTestDefinition definitionA =
                constructDefinition(
                        fromCompactBucketFormat("inactive:-1,control:0,test:1"),
                        fromCompactAllocationFormat("1:1.0"));
        final ConsumableTestDefinition definitionB =
                constructDefinition(
                        fromCompactBucketFormat("inactive:-1,control:0,test:1"),
                        fromCompactAllocationFormat("1:1.0"));
        definitionA.setTestType(TestType.ANONYMOUS_USER);
        definitionB.setTestType(TestType.AUTHENTICATED_USER);
        definitionA.setSalt("&a");
        definitionB.setSalt("&b");
        definitionB.setDependsOn(new TestDependency(TEST_A, 100));
        final Map<String, ConsumableTestDefinition> tests =
                ImmutableMap.of(
                        TEST_A, definitionA,
                        TEST_B, definitionB);
        final TestMatrixArtifact matrix = constructArtifact(tests);

        final ProctorLoadResult proctorLoadResult =
                verifyAndConsolidate(
                        matrix,
                        "",
                        ImmutableMap.of(TEST_A, new TestSpecification()),
                        RuleEvaluator.FUNCTION_MAPPER,
                        ProvidedContext.nonEvaluableContext(),
                        ImmutableSet.of(TEST_B));

        assertThat(proctorLoadResult.getTestsWithErrors()).isEmpty();
        assertThat(proctorLoadResult.getDynamicTestWithErrors()).containsExactly(TEST_B);

        assertThat(proctorLoadResult.getDynamicTestErrorMap())
                .hasEntrySatisfying(
                        TEST_B, x -> assertThat(x).hasMessageContaining("different test type"));
    }

    @Test
    public void testVerifyAndConsolidateShouldDetectDependencyWithSameSalt() {
        final ConsumableTestDefinition definitionA =
                constructDefinition(
                        fromCompactBucketFormat("inactive:-1,control:0,test:1"),
                        fromCompactAllocationFormat("1:1.0"));
        final ConsumableTestDefinition definitionB =
                constructDefinition(
                        fromCompactBucketFormat("inactive:-1,control:0,test:1"),
                        fromCompactAllocationFormat("1:1.0"));
        definitionA.setSalt("&a");
        definitionB.setSalt("&a");
        definitionB.setDependsOn(new TestDependency(TEST_A, 100));
        final Map<String, ConsumableTestDefinition> tests =
                ImmutableMap.of(
                        TEST_A, definitionA,
                        TEST_B, definitionB);
        final TestMatrixArtifact matrix = constructArtifact(tests);

        final ProctorLoadResult proctorLoadResult =
                verifyAndConsolidate(
                        matrix,
                        "",
                        ImmutableMap.of(TEST_A, new TestSpecification()),
                        RuleEvaluator.FUNCTION_MAPPER,
                        ProvidedContext.nonEvaluableContext(),
                        ImmutableSet.of(TEST_B));

        assertThat(proctorLoadResult.getTestsWithErrors()).isEmpty();
        assertThat(proctorLoadResult.getDynamicTestWithErrors()).containsExactly(TEST_B);

        assertThat(proctorLoadResult.getDynamicTestErrorMap())
                .hasEntrySatisfying(TEST_B, x -> assertThat(x).hasMessageContaining("same salt"));
    }

    @Test
    public void testVerifyAndConsolidateShouldDetectDependencyOnImcompatibleTest() {
        final ConsumableTestDefinition definitionA =
                constructDefinition(
                        fromCompactBucketFormat("inactive:-1,control:0,test:1"),
                        fromCompactAllocationFormat("1:1.0"));
        final ConsumableTestDefinition definitionB =
                constructDefinition(
                        fromCompactBucketFormat("inactive:-1,control:0,test:1"),
                        fromCompactAllocationFormat("1:1.0"));
        definitionA.setSalt("&a");
        definitionB.setSalt("&b");
        definitionB.setDependsOn(new TestDependency(TEST_A, 100));
        final Map<String, ConsumableTestDefinition> tests =
                ImmutableMap.of(
                        TEST_A, definitionA,
                        TEST_B, definitionB);
        final TestMatrixArtifact matrix = constructArtifact(tests);

        final ProctorLoadResult proctorLoadResult =
                verifyAndConsolidate(
                        matrix,
                        "",
                        // knows only control bucket so incompatible with definition
                        ImmutableMap.of(
                                TEST_A, transformTestBuckets(fromCompactBucketFormat("control:0"))),
                        RuleEvaluator.FUNCTION_MAPPER,
                        ProvidedContext.nonEvaluableContext(),
                        ImmutableSet.of(TEST_B));

        assertThat(proctorLoadResult.getTestsWithErrors()).containsExactly(TEST_A);
        assertThat(proctorLoadResult.getDynamicTestWithErrors()).containsExactly(TEST_B);

        assertThat(proctorLoadResult.getTestErrorMap())
                .hasEntrySatisfying(
                        TEST_A,
                        x ->
                                assertThat(x)
                                        .hasMessageContaining(
                                                "Proctor specification in your application does not contain bucket"));
        assertThat(proctorLoadResult.getDynamicTestErrorMap())
                .hasEntrySatisfying(
                        TEST_B,
                        x ->
                                assertThat(x)
                                        .hasMessageContaining(
                                                "depends on an unknown or incompatible test")
                                        .hasMessageContaining(TEST_A));
    }

    @Test
    public void testReadSpecification() {
        final String specString =
                "{\"tests\": "
                        + "{\"account1_tst\": {\"buckets\": {\"inactive\": -1, \"control\": 0, \"test\": 1},\"fallbackValue\": -1, \"payload\": {\"type\": \"stringValue\", \"allowForce\": true}}},"
                        + "\"providedContext\": {}}";

        final InputStream stream =
                new ByteArrayInputStream(specString.getBytes(StandardCharsets.UTF_8));

        final ProctorSpecification proctorSpecification = readSpecification(stream);

        assertThat(proctorSpecification.getTests().get("account1_tst").getPayload().getAllowForce())
                .isTrue();
    }

    @Test
    public void testContainsUnitlessAllocation() {
        final ConsumableTestDefinition td =
                ConsumableTestDefinition.fromTestDefinition(
                        TestDefinition.builder()
                                .setTestType(TestType.ANONYMOUS_USER)
                                .setSalt("test")
                                .setEnableUnitlessAllocations(true)
                                .setAllocations(
                                        ImmutableList.of(
                                                new Allocation(
                                                        "missingExperimentalUnit && country == 'US'",
                                                        ImmutableList.of(
                                                                new Range(0, 0),
                                                                new Range(-1, 0),
                                                                new Range(1, 1)))))
                                .build());
        assertThat(containsUnitlessAllocation(td)).isTrue();
    }

    @Test
    public void testContainUnitlessAllocation_ruleWithNoMissingExperimentalUnit() {
        final ConsumableTestDefinition td =
                ConsumableTestDefinition.fromTestDefinition(
                        TestDefinition.builder()
                                .setTestType(TestType.ANONYMOUS_USER)
                                .setSalt("test")
                                .setEnableUnitlessAllocations(true)
                                .setAllocations(
                                        ImmutableList.of(
                                                new Allocation(
                                                        "country == 'US'",
                                                        ImmutableList.of(
                                                                new Range(0, 0),
                                                                new Range(-1, 0),
                                                                new Range(1, 1)))))
                                .build());
        assertThat(containsUnitlessAllocation(td)).isFalse();
    }

    @Test
    public void testContainUnitlessAllocation_noEnabled() {
        final ConsumableTestDefinition td =
                ConsumableTestDefinition.fromTestDefinition(
                        TestDefinition.builder()
                                .setTestType(TestType.ANONYMOUS_USER)
                                .setSalt("test")
                                .setEnableUnitlessAllocations(false)
                                .setAllocations(
                                        ImmutableList.of(
                                                new Allocation(
                                                        "missingExperimentalUnit && country == 'US'",
                                                        ImmutableList.of(
                                                                new Range(0, 0),
                                                                new Range(-1, 0),
                                                                new Range(1, 1)))))
                                .build());
        assertThat(containsUnitlessAllocation(td)).isFalse();
    }

    @Test
    public void testContainUnitlessAllocation_enabledNoAllocations() {
        final ConsumableTestDefinition td =
                ConsumableTestDefinition.fromTestDefinition(
                        TestDefinition.builder()
                                .setTestType(TestType.ANONYMOUS_USER)
                                .setSalt("test")
                                .setEnableUnitlessAllocations(true)
                                .setAllocations(
                                        ImmutableList.of(
                                                new Allocation(
                                                        null,
                                                        ImmutableList.of(
                                                                new Range(0, 0),
                                                                new Range(-1, 0),
                                                                new Range(1, 1)))))
                                .build());
        assertThat(containsUnitlessAllocation(td)).isFalse();
    }

    @Test
    public void verifyAndConsolidateShouldTestUnitlessAllocation() {
        final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
        final Map<String, TestSpecification> requiredTests =
                ImmutableMap.of(TEST_A, transformTestBuckets(buckets));
        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            final ConsumableTestDefinition td =
                    constructDefinition(
                            buckets,
                            fromCompactAllocationFormat("missingExperimentalUnit|-1:0,0:0,1:1.0"));
            td.setEnableUnitlessAllocations(true);
            tests.put(TEST_A, td);

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // verifyAndConsolidate should not throw an error
            assertEquals(1, matrix.getTests().size());
            assertValid("Unitless allocation test should be included", matrix, requiredTests);
            assertEquals(
                    "required tests should not be removed from the matrix",
                    1,
                    matrix.getTests().size());
        }

        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            final ConsumableTestDefinition td =
                    constructDefinition(
                            buckets,
                            fromCompactAllocationFormat(
                                    "missingExperimentalUnit|-1:0,0:0.5,1:0.5"));
            td.setEnableUnitlessAllocations(true);
            tests.put(TEST_A, td);

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // verifyAndConsolidate should throw an error because missing allocation requires only
            // one bucket set to 100%
            assertEquals(1, matrix.getTests().size());
            assertInvalid(
                    "invalid test not required, unitless allocation must have one bucket set to 100%",
                    matrix, requiredTests);
            assertEquals(
                    "required tests should not be removed from the matrix",
                    1,
                    matrix.getTests().size());
        }

        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            final ConsumableTestDefinition td =
                    constructDefinition(
                            buckets,
                            fromCompactAllocationFormat(
                                    "missingExperimentalUnit|-1:0,0:0.5,1:0.5"));
            td.setEnableUnitlessAllocations(false);
            tests.put(TEST_A, td);

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // verifyAndConsolidate should not throw an error because unitless allocation not
            // enabled
            assertEquals(1, matrix.getTests().size());
            assertValid(
                    "valid tests required, since unitless allocation not enabled",
                    matrix,
                    requiredTests);
            assertEquals(
                    "required tests should not be removed from the matrix",
                    1,
                    matrix.getTests().size());
        }

        {
            final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
            final ConsumableTestDefinition td =
                    constructDefinition(
                            buckets,
                            fromCompactAllocationFormat(
                                    "!missingExperimentalUnit|-1:0,0:0.5,1:0.5"));
            td.setEnableUnitlessAllocations(true);
            tests.put(TEST_A, td);

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // verifyAndConsolidate should not throw an error because !missingExperimentalUnit
            assertEquals(1, matrix.getTests().size());
            assertValid(
                    "valid tests required, since unitless allocation not enabled",
                    matrix,
                    requiredTests);
            assertEquals(
                    "required tests should not be removed from the matrix",
                    1,
                    matrix.getTests().size());
        }
    }

    private static void setPayload(
            final List<TestBucket> buckets, final int index, final Payload newPayload) {
        buckets.set(
                index, TestBucket.builder().from(buckets.get(index)).payload(newPayload).build());
    }

    /* Test Helper Methods Below */

    private void assertInvalid(
            final String msg,
            final TestMatrixArtifact matrix,
            final Map<String, TestSpecification> requiredTests) {
        assertErrorCreated(false, true, msg, matrix, requiredTests);
    }

    private void assertMissing(
            final String msg,
            final TestMatrixArtifact matrix,
            final Map<String, TestSpecification> requiredTests) {
        assertErrorCreated(true, false, msg, matrix, requiredTests);
    }

    private void assertValid(
            final String msg,
            final TestMatrixArtifact matrix,
            final Map<String, TestSpecification> requiredTests) {
        assertErrorCreated(false, false, msg, matrix, requiredTests);
    }

    private void assertErrorCreated(
            final boolean hasMissing,
            final boolean hasInvalid,
            final String msg,
            final TestMatrixArtifact matrix,
            final Map<String, TestSpecification> requiredTests) {
        final ProctorLoadResult proctorLoadResult =
                verifyAndConsolidate(
                        matrix,
                        "[ testcase: " + msg + " ]",
                        requiredTests,
                        RuleEvaluator.FUNCTION_MAPPER);

        final Set<String> missingTests = proctorLoadResult.getMissingTests();
        assertEquals(msg + " missing tests is not empty", hasMissing, !missingTests.isEmpty());

        final Set<String> testsWithErrors = proctorLoadResult.getTestsWithErrors();
        assertEquals(msg + " invalid tests is not empty", hasInvalid, !testsWithErrors.isEmpty());
    }

    private TestMatrixArtifact constructArtifact(
            final Map<String, ConsumableTestDefinition> tests) {
        final TestMatrixArtifact matrix = new TestMatrixArtifact();

        matrix.setAudit(constructAudit());

        matrix.setTests(tests);
        return matrix;
    }

    private Audit constructAudit() {
        final Audit audit = new Audit();
        audit.setVersion("1");
        audit.setUpdatedBy("unit test");
        audit.setUpdated(1337133701337L);
        return audit;
    }

    public static ConsumableTestDefinition constructDefinition(
            final List<TestBucket> buckets, final List<Allocation> allocations) {

        final ConsumableTestDefinition test = new ConsumableTestDefinition();
        test.setVersion(""); // don't care about version for this test
        test.setSalt(null); // don't care about salt for this test
        test.setRule(null); // don't care about rule for this test
        test.setTestType(TestType.ANONYMOUS_USER); // don't really care, but need a valid value
        test.setConstants(Collections.emptyMap()); // don't care about constants for this test

        test.setBuckets(buckets);
        test.setAllocations(allocations);
        return test;
    }

    /*  *********************************************************************
       The compact format is used because it's easier to quickly list bucket
       allocations and bucket values than to use string JSON
    *  ********************************************************************* */

    public static List<Allocation> fromCompactAllocationFormat(final String... allocations) {
        final List<String> allocationList = new ArrayList<>(allocations.length);
        allocationList.addAll(Arrays.asList(allocations));
        return fromCompactAllocationFormat(allocationList);
    }

    private static List<Allocation> fromCompactAllocationFormat(final List<String> allocations) {
        final List<Allocation> allocationList = new ArrayList<>(allocations.size());
        // rule|0:0,0:.0.1,0:.2
        for (final String allocation : allocations) {
            final int separatorPosition = allocation.lastIndexOf('|');
            final String rule;
            final String sRanges;
            if (separatorPosition < 0) {
                rule = null;
                sRanges = allocation;
            } else {
                rule = allocation.substring(0, separatorPosition);
                sRanges = allocation.substring(separatorPosition + 1);
            }
            final String[] allRanges = sRanges.split(",");
            final List<Range> ranges = new ArrayList<>(allRanges.length);
            for (final String sRange : allRanges) {
                // Could handle index-out of bounds + number formatting exception better.
                final String[] rangeParts = sRange.split(":");
                ranges.add(
                        new Range(
                                Integer.parseInt(rangeParts[0], 10),
                                Double.parseDouble(rangeParts[1])));
            }
            allocationList.add(new Allocation(rule, ranges));
        }
        return allocationList;
    }

    public static List<TestBucket> fromCompactBucketFormat(
            final String sBuckets, final Payload... payloads) {
        final String[] bucketParts = sBuckets.split(",");
        if ((payloads.length > 0) && (bucketParts.length != payloads.length)) {
            throw new IllegalArgumentException(
                    "array lengths dont match " + sBuckets + " " + Arrays.toString(payloads));
        }
        final List<TestBucket> buckets = new ArrayList<>(bucketParts.length);
        for (int i = 0; i < bucketParts.length; i++) {
            // Could handle index-out of bounds + number formatting exception better.
            final String[] nameAndValue = bucketParts[i].split(":");
            final Payload payload = (payloads.length == 0) ? null : payloads[i];
            buckets.add(
                    new TestBucket(
                            nameAndValue[0],
                            Integer.parseInt(nameAndValue[1]),
                            "bucket " + i,
                            payload));
        }
        return buckets;
    }

    private TestSpecification transformTestBuckets(final List<TestBucket> testBuckets) {
        final TestSpecification testSpec = new TestSpecification();
        final Map<String, Integer> buckets = new LinkedHashMap<>();
        for (final TestBucket b : testBuckets) {
            buckets.put(b.getName(), b.getValue());
        }
        testSpec.setBuckets(buckets);
        return testSpec;
    }

    private TestSpecification transformTestBuckets(
            final List<TestBucket> testBuckets,
            final PayloadType payloadType,
            @Nullable final Map<String, PayloadType> schema,
            final String validator) {
        final TestSpecification testSpec = transformTestBuckets(testBuckets);
        final PayloadSpecification payloadSpec = new PayloadSpecification();
        payloadSpec.setType(payloadType.payloadTypeName);
        payloadSpec.setValidator(validator);
        if (schema == null) {
            payloadSpec.setSchema(null);
        } else {
            payloadSpec.setSchema(
                    schema.entrySet().stream()
                            .collect(
                                    Collectors.toMap(
                                            Entry::getKey, e -> e.getValue().payloadTypeName)));
        }
        testSpec.setPayload(payloadSpec);
        return testSpec;
    }

    private TestSpecification transformTestBuckets(
            final List<TestBucket> testBuckets,
            final PayloadType payloadType,
            final String validator) {
        final TestSpecification testSpec = transformTestBuckets(testBuckets);
        final PayloadSpecification payloadSpec = new PayloadSpecification();
        payloadSpec.setType(payloadType.payloadTypeName);
        payloadSpec.setValidator(validator);
        testSpec.setPayload(payloadSpec);
        return testSpec;
    }
}
