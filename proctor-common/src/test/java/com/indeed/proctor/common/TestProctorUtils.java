package com.indeed.proctor.common;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.proctor.common.model.TestType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import javax.el.ValueExpression;
import java.io.IOException;
import java.io.InputStream;
import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.proctor.common.model.TestType;
import org.junit.Test;

import javax.el.ELException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * @author parker
 */
public class TestProctorUtils {

    private static final String TEST_A = "testA";
    private static final String TEST_B = "testB";
    private static final String PATH_UNKNOWN_TEST_TYPE = "unknown-test-type.json";

    /**
     * Test that top level and allocation rules all respect the same definition
     * of "empty"
     */
    @Test
    public void convertToConsumableTestDefinitionEmptyRules() {
        // all of the following "rules" should be treated as empty in the TestDefinition
        // rule: null
        // rule: ""
        // rule: " "
        final String[] emptyRules = new String[] { null, "", " " };
        final Range range = new Range(0, 1.0d);

        final String version = "100";
        final TestType testType = TestType.ANONYMOUS_USER;
        final String salt = "testsalt";
        final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
        final Map<String, Object> constants = Collections.emptyMap();
        final Map<String, Object> specialConstants = Collections.emptyMap();
        final String description = "test description";

        for(final String tdRule : emptyRules) {
            for(String allocRule : emptyRules) {
                final Allocation allocation = new Allocation(allocRule, Collections.singletonList(range));
                final TestDefinition testDefinition = new TestDefinition(
                    version,
                    tdRule,
                    testType,
                    salt,
                    buckets,
                    Collections.singletonList(allocation),
                    constants,
                    specialConstants,
                    description
                );

                final ConsumableTestDefinition ctd = ProctorUtils.convertToConsumableTestDefinition(testDefinition);
                assertEquals(version, ctd.getVersion());
                assertEquals(testType, ctd.getTestType());
                assertEquals(salt, ctd.getSalt());
                assertEquals(description, ctd.getDescription());
                assertEquals(0, ctd.getConstants().size());
                assertEquals(buckets, ctd.getBuckets());

                assertEquals(String.format("TestDefinition rule '%s' should convert to a null ConsumableTestDefinition.rule", tdRule), null, ctd.getRule());

                assertEquals(1, ctd.getAllocations().size());
                final Allocation ctdAllocation = ctd.getAllocations().get(0);
                assertEquals(String.format("Allocation rule '%s' should convert to a null ConsumableTestDefinition.Allocation.rule", allocRule), null, ctdAllocation.getRule());
                assertEquals(allocation.getRanges(), ctdAllocation.getRanges());
            }
        }
    }


    /**
     * Checks that allocation and top level rules can optionally be surrounded by ${ ... }
     */
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
        final Map<String, Object> constants = Collections.emptyMap();
        final Map<String, Object> specialConstants = Collections.emptyMap();
        final String description = "test description";


        for(final String rule : new String[] { "lang == 'en'", "${lang == 'en'}"})
        {
            final Allocation allocation = new Allocation(rule, Collections.singletonList(range));
            final TestDefinition testDefinition = new TestDefinition(
                version,
                rule,
                testType,
                salt,
                buckets,
                Collections.singletonList(allocation),
                constants,
                specialConstants,
                description
            );

            final ConsumableTestDefinition ctd = ProctorUtils.convertToConsumableTestDefinition(testDefinition);
            assertEquals(String.format("TestDefinition rule '%s' should convert to a ${lang == 'en'} ConsumableTestDefinition.rule", rule), "${lang == 'en'}", ctd.getRule());

            assertEquals(1, ctd.getAllocations().size());
            final Allocation ctdAllocation = ctd.getAllocations().get(0);
            assertEquals(String.format("Allocation rule '%s' should convert to a ${lang == 'en'} ConsumableTestDefinition.Allocation.rule", rule), "${lang == 'en'}", ctdAllocation.getRule());
            assertEquals(allocation.getRanges(), ctdAllocation.getRanges());
        }
    }

    /**
     * Checks that top level rules respect the special constants and support
     * rule formats that do and do not contain "${}"
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
        final Map<String, Object> constants = Collections.emptyMap();
        final Map<String, Object> specialConstants = Collections.<String, Object>singletonMap("__COUNTRIES", Lists.newArrayList("US", "CA"));
        final String description = "test description";

        final Allocation allocation = new Allocation(null, Collections.singletonList(range));

        for(final String tdRule : new String[] { "lang == 'en'", "${lang == 'en'}"})
        {
            final TestDefinition testDefinition = new TestDefinition(
                version,
                tdRule,
                testType,
                salt,
                buckets,
                Collections.singletonList(allocation),
                constants,
                specialConstants,
                description
            );

            final ConsumableTestDefinition ctd = ProctorUtils.convertToConsumableTestDefinition(testDefinition);
            assertEquals(String.format("TestDefinition rule '%s' should convert to ${proctor:contains(__COUNTRIES, country) && lang == 'en'} ConsumableTestDefinition.rule", tdRule), "${proctor:contains(__COUNTRIES, country) && lang == 'en'}", ctd.getRule());
            assertEquals(1, ctd.getConstants().size());
            assertEquals("special constants should be added to constants", Lists.newArrayList("US", "CA"), ctd.getConstants().get("__COUNTRIES"));
        }
    }

    @Test
    public void testRemoveElExpressionBraces() {
        assertEquals(null, ProctorUtils.removeElExpressionBraces(""));
        assertEquals(null, ProctorUtils.removeElExpressionBraces(" "));
        assertEquals(null, ProctorUtils.removeElExpressionBraces(null));
        assertEquals(null, ProctorUtils.removeElExpressionBraces("\t"));
        assertEquals(null, ProctorUtils.removeElExpressionBraces(" ${} "));
        assertEquals(null, ProctorUtils.removeElExpressionBraces(" ${ } "));
        assertEquals("a", ProctorUtils.removeElExpressionBraces("${a}"));
        assertEquals("a", ProctorUtils.removeElExpressionBraces(" ${a} "));
        assertEquals("a", ProctorUtils.removeElExpressionBraces(" ${ a } "));
        assertEquals("a", ProctorUtils.removeElExpressionBraces(" ${ a}"));
        assertEquals("a", ProctorUtils.removeElExpressionBraces("${a } "));
        assertEquals("a", ProctorUtils.removeElExpressionBraces(" a "));
        assertEquals("lang == 'en'", ProctorUtils.removeElExpressionBraces("lang == 'en'"));
        assertEquals("lang == 'en'", ProctorUtils.removeElExpressionBraces("${lang == 'en'}"));
        // whitespace should be trimmed
        assertEquals("lang == 'en'", ProctorUtils.removeElExpressionBraces("${ lang == 'en' }"));
        // whitespace should be removed around braces
        assertEquals("lang == 'en'", ProctorUtils.removeElExpressionBraces(" ${ lang == 'en' } "));
        // only single level of braces are removed
        assertEquals("${lang == 'en'}", ProctorUtils.removeElExpressionBraces("${${lang == 'en'}}"));
        // mis matched braces are not handled
        assertEquals("${lang == 'en'", ProctorUtils.removeElExpressionBraces("${lang == 'en'"));
        // mis matched braces are not handled
        assertEquals("lang == 'en'}", ProctorUtils.removeElExpressionBraces("lang == 'en'}"));
    }

    @Test
    public void testEmptyWhitespace() {
        assertTrue(ProctorUtils.isEmptyWhitespace(""));
        assertTrue(ProctorUtils.isEmptyWhitespace(null));
        assertTrue(ProctorUtils.isEmptyWhitespace("  "));
        assertTrue(ProctorUtils.isEmptyWhitespace("  \t"));
        assertFalse(ProctorUtils.isEmptyWhitespace(" x "));
        assertFalse(ProctorUtils.isEmptyWhitespace("/"));
    }


    @Test
    public void verifyAndConsolidateShouldTestAllocationSum() throws IncompatibleTestMatrixException {
        List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
        Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets));
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets,
                                                      // Allocations do not add up to 1
                                                      fromCompactAllocationFormat("ruleA|-1:0.5,0:0.5,1:0.0", "-1:0.0,0:0.0,1:0.0")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // verifyAndConsolidate should not throw an error because the 'invalidbuckets' test is not required.
            assertEquals(1, matrix.getTests().size());
            assertValid("invalid test not required, sum{allocations} < 1.0", matrix, Collections.<String, TestSpecification>emptyMap());
            assertEquals("non-required tests should be removed from the matrix", 0, matrix.getTests().size());
        }
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets,
                                                      // Allocations do not add up to 1
                                                      fromCompactAllocationFormat("ruleA|-1:0.5,0:0.5,1:0.0", "-1:0.0,0:0.0,1:0.0")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // verifyAndConsolidate should throw an error because the 'invalidbuckets' test is required.
            assertEquals(1, matrix.getTests().size());
            assertInvalid("bucket allocation sums are unchecked, sum{allocations} < 1.0", matrix, requiredTests);
            assertEquals("required tests should not be removed from the matrix", 1, matrix.getTests().size());
        }
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets,
                                                      // Allocations do not add up to 1
                                                      fromCompactAllocationFormat("ruleA|-1:0.5,0:0.5,1:0.0", "-1:0.5,0:0.5,1:0.5")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertEquals(1, matrix.getTests().size());
            // verifyAndConsolidate should not throw an error because the 'invalidbuckets' test is not required.
            assertValid("invalid test not required, sum{allocations} > 1.0", matrix, Collections.<String, TestSpecification>emptyMap());
            assertEquals("non-required tests should be removed from the matrix", 0, matrix.getTests().size());
        }
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets,
                                                      // Allocations do not add up to 1
                                                      fromCompactAllocationFormat("ruleA|-1:0.5,0:0.5,1:0.0", "-1:0.5,0:0.5,1:0.5")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertEquals(1, matrix.getTests().size());
            // verifyAndConsolidate should throw an error because the 'testa' test is required.
            assertInvalid("bucket allocation sums are unchecked, sum{allocations} > 1.0", matrix, requiredTests);
            assertEquals("required tests should not be removed from the matrix", 1, matrix.getTests().size());
        }
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets,
                                                      // Allocations add up to 1.0
                                                      fromCompactAllocationFormat("ruleA|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertEquals(1, matrix.getTests().size());
            assertValid("bucket allocation sums are unchecked, sum{allocations} == 1.0", matrix, Collections.<String, TestSpecification>emptyMap());
            assertEquals("non-required tests should be removed from the matrix", 0, matrix.getTests().size());
        }
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets,
                                                      // Allocations add up to 1.0
                                                      fromCompactAllocationFormat("ruleA|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertEquals(1, matrix.getTests().size());
            assertValid("bucket allocation sums are unchecked, sum{allocations} == 1.0", matrix, requiredTests);
            assertEquals("required tests should not be removed from the matrix", 1, matrix.getTests().size());
        }
    }

    @Test
    public void testELValidity_inProctorBuilderAllocationRules() throws IncompatibleTestMatrixException {

        //testing invalid allocation rule
        final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
        final ConsumableTestDefinition testDefInVal = constructDefinition(buckets,
                fromCompactAllocationFormat("${b4t#+=}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25")); // invalid EL, nonsense rule
        try {
            ProctorUtils.verifyInternallyConsistentDefinition("testELevalInval", "test el recognition - inval", testDefInVal);
            fail("expected IncompatibleTestMatrixException");
        } catch (IncompatibleTestMatrixException e) {
            //expected
        }

        //testing valid functions pass with proctor included functions (will throw exception if can't find) and backwards compatibility
        final ConsumableTestDefinition testDefVal1 = constructDefinition(buckets,
                fromCompactAllocationFormat("${proctor:now()==indeed:now()}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));
        ProctorUtils.verifyInternallyConsistentDefinition("testELevalProctor", "test el recognition", testDefVal1);

    }
    @Test
    public void testELValidity_inProctorBuilderTestRule() throws IncompatibleTestMatrixException {
        //Testing syntax validation with a test rule
        final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
        final ConsumableTestDefinition testDefInValTestRule = constructDefinition(buckets,
                fromCompactAllocationFormat("${proctor:now()>-1}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));
        testDefInValTestRule.setRule("${b4t#+=}");
        try {
            ProctorUtils.verifyInternallyConsistentDefinition("testELevalInValTestRule", "test el recognition - inval test rule", testDefInValTestRule);
            fail("expected IncompatibleTestMatrixException");
        } catch (IncompatibleTestMatrixException e) {
            //expected
        }

        //testing the test rule el function recognition
        final ConsumableTestDefinition testDefValTestRule = constructDefinition(buckets,
                fromCompactAllocationFormat("${true}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));
        testDefValTestRule.setRule("${proctor:now()==indeed:now()}");
        ProctorUtils.verifyInternallyConsistentDefinition("testELevalValTestRule", "test el recognition - val test rule and functions", testDefValTestRule);


    }

    @Test
    public void testProvidedContextConversion() throws IncompatibleTestMatrixException {
        final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
        { //verify primitive types convert correctly
            final ConsumableTestDefinition testDef = constructDefinition(buckets,
                    fromCompactAllocationFormat("${time eq ''}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));
            final Map<String, String> providedContextString = new HashMap<String, String>();
            providedContextString.put("time", "String");
            final ProvidedContext providedContext = ProctorUtils.convertContextToTestableMap(providedContextString);
            ProctorUtils.verifyInternallyConsistentDefinition("testProvidedContextConversion", "test Provided Context Conversion String", testDef, RuleEvaluator.FUNCTION_MAPPER, providedContext);
            //checking to make sure it can evaluate with converted provided context
        }
        { //verify primitive types convert correctly
            final ConsumableTestDefinition testDef = constructDefinition(buckets,
                    fromCompactAllocationFormat("${time eq 0}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));
            final Map<String, String> providedContextInteger = new HashMap<String, String>();
            providedContextInteger.put("time", "int");
            final ProvidedContext providedContext = ProctorUtils.convertContextToTestableMap(providedContextInteger);
            ProctorUtils.verifyInternallyConsistentDefinition("testProvidedContextConversion", "test Provided Context Conversion Integer", testDef, RuleEvaluator.FUNCTION_MAPPER, providedContext);
            //checking to make sure it can evaluate with converted provided context
        }
        { //verify primitive types convert correctly
            final ConsumableTestDefinition testDef = constructDefinition(buckets,
                    fromCompactAllocationFormat("${time eq ''}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));
            final Map<String, String> providedContextChar = new HashMap<String, String>();
            providedContextChar.put("time", "char");
            final ProvidedContext providedContext = ProctorUtils.convertContextToTestableMap(providedContextChar);
            ProctorUtils.verifyInternallyConsistentDefinition("testProvidedContextConversion", "test Provided Context Conversion Char", testDef, RuleEvaluator.FUNCTION_MAPPER, providedContext);
            //checking to make sure it can evaluate with converted provided context
        }
        { //verify primitive types convert correctly
            final ConsumableTestDefinition testDef = constructDefinition(buckets,
                    fromCompactAllocationFormat("${time}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));
            final Map<String, String> providedContextBoolean = new HashMap<String, String>();
            providedContextBoolean.put("time", "Boolean");
            final ProvidedContext providedContext = ProctorUtils.convertContextToTestableMap(providedContextBoolean);
            ProctorUtils.verifyInternallyConsistentDefinition("testProvidedContextConversion", "test Provided Context Conversion Boolean", testDef, RuleEvaluator.FUNCTION_MAPPER, providedContext);
            //checking to make sure it can evaluate with converted provided context
        }
        { //verify User Defined enum Classes convert correctly
            final ConsumableTestDefinition testDef = constructDefinition(buckets,
                    fromCompactAllocationFormat("${time eq 'SPADES'}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));
            final Map<String, String> providedContextClass = new HashMap<String, String>();
            providedContextClass.put("time", "com.indeed.proctor.common.TestEnumType");
            final ProvidedContext providedContext = ProctorUtils.convertContextToTestableMap(providedContextClass);
            ProctorUtils.verifyInternallyConsistentDefinition("testProvidedContextConversion", "test Provided Context Conversion Class", testDef, RuleEvaluator.FUNCTION_MAPPER, providedContext);
            //checking to make sure it can evaluate with converted provided context
        }
        { //verify enums are actually used and an error is thrown with a nonexistent constant
            final ConsumableTestDefinition testDef = constructDefinition(buckets,
                    fromCompactAllocationFormat("${time eq 'SP'}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));
            final Map<String, String> providedContextClass = new HashMap<String, String>();
            providedContextClass.put("time", "com.indeed.proctor.common.TestEnumType");
            try {
                final ProvidedContext providedContext = ProctorUtils.convertContextToTestableMap(providedContextClass);
                ProctorUtils.verifyInternallyConsistentDefinition("testProvidedContextConversion", "test Provided Context Conversion Class", testDef, RuleEvaluator.FUNCTION_MAPPER, providedContext);
                fail("expected IncompatibleTestMatrixException due to nonexistent enum constant");
            } catch (IncompatibleTestMatrixException e) {
                //expected
            }
        }
        { //verify class names are verified correctly
            final Map<String, String> providedContextBadClass = new HashMap<String, String>();
            providedContextBadClass.put("time", "com.indeed.proctor.common.TestRulesCla");
            final ProvidedContext providedContext = ProctorUtils.convertContextToTestableMap(providedContextBadClass);
            assertFalse(providedContext.isEvaluable());
            //false due to bad classname
        }
        { //verify missing constructors are verified correctly
            final Map<String, String> providedContextNoConstructor = new HashMap<String, String>();
            providedContextNoConstructor.put("time", "com.indeed.proctor.common.AbstractProctorLoader");
            final ProvidedContext providedContext = ProctorUtils.convertContextToTestableMap(providedContextNoConstructor);
            assertFalse(providedContext.isEvaluable());
            //false due to no default constructor
        }
    }

    @Test
    public void testELValidity_atTestMatrixLoadTime() throws IncompatibleTestMatrixException, IOException {
        final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
        {
            //testing recognition of test constants
            final ConsumableTestDefinition testDefValConstants = constructDefinition(buckets,
                    fromCompactAllocationFormat("${proctor:now()>time}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));
            final Map<String, Object> providedConstantsVal = new HashMap<String, Object>();
            providedConstantsVal.put("time", "1");
            testDefValConstants.setConstants(providedConstantsVal);
            final Map<String, String> providedContext = Collections.emptyMap();
            ProctorUtils.verifyInternallyConsistentDefinition("testELevalwithcontext", "test context recognition", testDefValConstants, RuleEvaluator.FUNCTION_MAPPER,
                    ProctorUtils.convertContextToTestableMap(providedContext));
        }
        {//test if the providedContext is read in correctly
            final ConsumableTestDefinition testDefValConstants2 = constructDefinition(buckets,
                    fromCompactAllocationFormat("${proctor:now()>time}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));
            final ObjectMapper objectMapper = new ObjectMapper();
            final ProctorSpecification spec = objectMapper.readValue(getClass().getResourceAsStream("no-context-specification.json"), ProctorSpecification.class);
            final Map<String, String> providedContext2 = spec.getProvidedContext(); //needs to read in empty provided context as Collections.emptyMap() and not null
            try {
                ProctorUtils.verifyInternallyConsistentDefinition("testELevalwithcontext", "test context recognition", testDefValConstants2, RuleEvaluator.FUNCTION_MAPPER,
                        ProctorUtils.convertContextToTestableMap(providedContext2));
                fail("expected IncompatibleTestMatrixException");
            } catch (IncompatibleTestMatrixException e) {
                //expected
            }
        }
        {//test that an error is thrown with missing providedContext
            final ConsumableTestDefinition testDef = constructDefinition(buckets,
                    fromCompactAllocationFormat("${time eq ''}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));
            try {
                ProctorUtils.verifyInternallyConsistentDefinition("testProvidedContextMissing", "test Provided Context Missing", testDef, RuleEvaluator.FUNCTION_MAPPER, new ProvidedContext(ProvidedContext.EMPTY_CONTEXT,true));
                //checking to make sure it can evaluate with converted provided context
                fail("expected IncompatibleTestMatrixException due to missing provided Context");
            } catch (IncompatibleTestMatrixException e) {
                //expected
            }
        }
        {//testing recognition of providedContext in testRule
            final Map<String, String> providedContextVal = new HashMap<String, String>();
            providedContextVal.put("time", "Integer");
            ConsumableTestDefinition testDefValContextTestRule = constructDefinition(buckets,
                    fromCompactAllocationFormat("${proctor:now()>-1}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));
            testDefValContextTestRule.setRule("${proctor:now()>time}");
            ProctorUtils.verifyInternallyConsistentDefinition("testELevalwithcontext", "test context recognition in test rule", testDefValContextTestRule, RuleEvaluator.FUNCTION_MAPPER,
                    ProctorUtils.convertContextToTestableMap(providedContextVal));
        }
        { //testing that invalid properties are recognized
            final ConsumableTestDefinition testDef = constructDefinition(buckets,
                    fromCompactAllocationFormat("${ua.iPad}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));
            final Map<String, String> providedContextClass = new HashMap<String, String>();
            providedContextClass.put("ua", "com.indeed.proctor.common.TestRulesClass");
            final ProvidedContext providedContext = ProctorUtils.convertContextToTestableMap(providedContextClass);
            try {
                ProctorUtils.verifyInternallyConsistentDefinition("testProvidedContextConversion", "test Provided Context Conversion Class", testDef, RuleEvaluator.FUNCTION_MAPPER, providedContext);
                fail("expected IncompatibleTestMatrixException due to missing attribute");
            } catch (IncompatibleTestMatrixException e) {
                // expected due to incorrect spelling
            }
        }
        { //testing that valid properties are recognized
            final ConsumableTestDefinition testDef = constructDefinition(buckets,
                    fromCompactAllocationFormat("${ua.IPad}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));
            final Map<String, String> providedContextClass = new HashMap<String, String>();
            providedContextClass.put("ua", "com.indeed.proctor.common.TestRulesClass");
            final ProvidedContext providedContext = ProctorUtils.convertContextToTestableMap(providedContextClass);
            ProctorUtils.verifyInternallyConsistentDefinition("testProvidedContextConversion", "test Provided Context Conversion Class", testDef, RuleEvaluator.FUNCTION_MAPPER, providedContext);
        }
        { //testing that invalid functions are recognized
            final ConsumableTestDefinition testDef = constructDefinition(buckets,
                    fromCompactAllocationFormat("${proctor:notafunction(5)}|-1:0.5,0:0.5,1:0.0", "-1:0.25,0:0.5,1:0.25"));

            try {
                ProctorUtils.verifyInternallyConsistentDefinition("testProvidedContextConversion", "test Provided Context Conversion Class", testDef, RuleEvaluator.FUNCTION_MAPPER, new ProvidedContext(ProvidedContext.EMPTY_CONTEXT,true));
                fail("expected IncompatibleTestMatrixException due to missing function");
            } catch (IncompatibleTestMatrixException e) {
                // expected due to incorrect spelling
            }
        }
    }


    @Test
    public void verifyAndConsolidateShouldFailIfMissingDefaultAllocation() throws IncompatibleTestMatrixException {
        final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
        Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets));
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets,
                                                      // Allocations all have rules
                                                      fromCompactAllocationFormat("ruleA|-1:0.0,0:0.0,1:1.0", "ruleB|-1:0.5,0:0.5,1:0.0")));

            assertValid("test missing empty rule is not required", constructArtifact(tests), Collections.<String, TestSpecification>emptyMap());
            assertMissing("test missing empty rule is required", constructArtifact(Collections.<String, ConsumableTestDefinition>emptyMap()), requiredTests);
        }
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets,
                                                      // non-final allocation lacks a non-empty rule
                                                      fromCompactAllocationFormat("|-1:0.0,0:0.0,1:1.0", "-1:0.5,0:0.5,1:0.0")));

            assertInvalid("non-final rule lacks non-empty rule", constructArtifact(tests), requiredTests);
            assertValid("non-final rule lacks non-empty rule is allowed when not required", constructArtifact(tests), Collections.<String, TestSpecification>emptyMap());
        }
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets,
                                                      fromCompactAllocationFormat("ruleA|-1:0.0,0:0.0,1:1.0", "|-1:0.5,0:0.5,1:0.0")));

            assertValid("allocation with '' rule is valid for final last allocation", constructArtifact(tests), requiredTests);
        }
        // NOTE: the two test below illustrate current behavior.
        // The "${}" rule is treated as non-empty for validation.
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets,
                                                      // non-final allocation lacks a non-empty rule
                                                      fromCompactAllocationFormat("${}|-1:0.0,0:0.0,1:1.0", "-1:0.5,0:0.5,1:0.0")));

            assertInvalid("non-final rule of '${}' should be treated as empty rule", constructArtifact(tests), requiredTests);
        }
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets,
                                                      fromCompactAllocationFormat("${}|-1:0.5,0:0.5,1:0.0")));

            assertValid("allocation with '${}' rule is valid for final last allocation", constructArtifact(tests), requiredTests);
        }

    }

    @Test
    public void verifyAndConsolidateShouldFailIfNoAllocations() throws IncompatibleTestMatrixException {
        List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");

        Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets));
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets, Collections.<Allocation>emptyList()));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertValid("test missing allocations is not required", matrix, Collections.<String, TestSpecification>emptyMap());
        }
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets, Collections.<Allocation>emptyList()));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertInvalid("test missing allocations is required", matrix, requiredTests);
        }
    }

    @Test
    public void unknownBucketWithAllocationGreaterThanZero() throws IncompatibleTestMatrixException {
        // The test-matrix has 3 buckets
        List<TestBucket> buckets = fromCompactBucketFormat("zero:0,one:1,two:2");
        // The proctor-specification only knows about two of the buckets
        final TestSpecification testSpecification = transformTestBuckets(fromCompactBucketFormat("zero:0,one:1"));
        Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, testSpecification);

        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            // Allocation of bucketValue=2 is > 0
            final ConsumableTestDefinition testDefinition = constructDefinition(buckets, fromCompactAllocationFormat("0:0,1:0,2:1.0"));
            tests.put(TEST_A, testDefinition);

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // externally inconsistent matrix
            assertInvalid("allocation for externally unknown bucket (two) > 0", matrix, requiredTests);
            assertEquals("trivially expected only one test in the matrix", 1, matrix.getTests().size());
            final List<Allocation> allocations = matrix.getTests().values().iterator().next().getAllocations();
            assertEquals("trivially expected only one allocation in the test (same as source)", 1, allocations.size());
            final List<Range> ranges = allocations.iterator().next().getRanges();
            assertEquals("Expected the ranges to be reduced from 3 to 1, since only the fallback value is now present", 1, ranges.size());
            final Range onlyRange = ranges.iterator().next();
            assertEquals("Should have adopted the fallback value from the test spec", onlyRange.getBucketValue(), testSpecification.getFallbackValue());
            assertEquals("Trivially should have been set to 100% fallback", 1.0, onlyRange.getLength(), 0.005);
        }
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            // Allocation of bucketValue=2 is == 0
            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("0:0.5,1:0.5,2:0")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertValid("allocation for externally unknown bucket (two) == 0", matrix, requiredTests);
        }
    }

    @Test
    public void internallyUnknownBucketWithAllocationGreaterThanZero() throws IncompatibleTestMatrixException {
         // The test-matrix has 3 buckets
        List<TestBucket> buckets = fromCompactBucketFormat("zero:0,one:1,two:2");
        Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets));

        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            // Allocation has 4 buckets, bucket with non-zero allocation in an unknown bucket
            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("0:0,1:0,2:0.5,3:0.5")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally inconsistent matrix
            assertInvalid("allocation for internally unknown bucket (three) > 0", matrix, requiredTests);
        }
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            // There is an unknown bucket with non-zero allocation
            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("0:0,1:0,2:0.5,3:0.5")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally inconsistent matrix
            assertInvalid("allocation for internally unknown bucket (three) == 0", matrix, requiredTests);
        }
    }

    @Test
    public void requiredTestBucketsMissing() throws IncompatibleTestMatrixException {
         // The test-matrix has fewer buckets than the required tests
        List<TestBucket> buckets_matrix = fromCompactBucketFormat("zero:0,one:1");
        List<TestBucket> buckets_required = fromCompactBucketFormat("zero:0,one:1,two:2,three:3");
        Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets_required));

        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets_matrix, fromCompactAllocationFormat("0:0,1:1.0")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally inconsistent matrix
            assertValid("test-matrix has a subset of required buckets", matrix, requiredTests);
        }
    }

    @Test
    public void bucketsNameAndValuesShouldBeConsistent() throws IncompatibleTestMatrixException {
        {
            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(fromCompactBucketFormat("zero:0,one:1")));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            // The Bucket Names and Values intentionally do not match
            tests.put(TEST_A, constructDefinition(fromCompactBucketFormat("zero:1,one:0"),
                                                  fromCompactAllocationFormat("0:0,1:1.0")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertValid("test-matrix has a different {bucketValue} -> {bucketName} mapping than required Tests", matrix, requiredTests);
        }
        {
            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(fromCompactBucketFormat("zero:0,one:1,two:2")));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            // The Bucket Names and Values intentionally do not match
            tests.put(TEST_A, constructDefinition(fromCompactBucketFormat("zero:0,one:2"),
                                                  fromCompactAllocationFormat("0:0,2:1.0")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertValid("test-matrix has a different {bucketValue} -> {bucketName} mapping than required Tests", matrix, requiredTests);
        }
    }

    @Test
    public void requiredTestIsMissing() throws IncompatibleTestMatrixException {
         // The test-matrix has 3 buckets
        List<TestBucket> buckets_A = fromCompactBucketFormat("zero:0,one:1,two:2");
        final TestSpecification testSpecA = transformTestBuckets(buckets_A);

        List<TestBucket> buckets_B = fromCompactBucketFormat("foo:0,bar:1");
        final TestSpecification testSpecB = transformTestBuckets(buckets_B);
        testSpecB.setFallbackValue(-2); // unusual value;

        Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, testSpecA, TEST_B, testSpecB);

        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets_A, fromCompactAllocationFormat("0:0,1:0.5,2:0.5")));

            // Artifact only has 1 of the 2 required tests
            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally inconsistent matrix
            assertNull("missing testB should not be present prior to consolidation", matrix.getTests().get(TEST_B));
            assertMissing("required testB is missing from the test matrix", matrix, requiredTests);

            final ConsumableTestDefinition consolidatedTestB = matrix.getTests().get(TEST_B);
            assertNotNull("autogenerated testB definition missing from consolidated matrix", consolidatedTestB);
            assertEquals(
                    "autogenerated testB definition should have used custom fallback value",
                    testSpecB.getFallbackValue(),
                    consolidatedTestB.getAllocations().get(0).getRanges().get(0).getBucketValue());

        }
        {
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets_A, fromCompactAllocationFormat("0:0,1:0.5,2:0.5")));
            tests.put(TEST_B, constructDefinition(buckets_B, fromCompactAllocationFormat("0:0.5,1:0.5")));

            // Artifact both of the required tests
            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid("both required tests are present in the matrix", matrix, requiredTests);
        }
        {
            Map<String, TestSpecification> only_TestA_Required = ImmutableMap.of(TEST_A, transformTestBuckets(buckets_A));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();
            tests.put(TEST_A, constructDefinition(buckets_A, fromCompactAllocationFormat("0:0,1:0.5,2:0.5")));
            // Intentionally making the non-required test B allocation sum to 0.5
            tests.put(TEST_B, constructDefinition(buckets_B, fromCompactAllocationFormat("0:0,1:0.5")));

            // Artifact both of the required tests
            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertEquals(2, matrix.getTests().size());
            assertValid("required test A is present in the matrix", matrix, only_TestA_Required);

            assertEquals("Only required test A should remain in the matrix", 1, matrix.getTests().size());
            assertTrue("Only required test A should remain in the matrix", matrix.getTests().containsKey(TEST_A));
            assertFalse("Only required test A should remain in the matrix", matrix.getTests().containsKey(TEST_B));
        }
    }

    @Test
    public void verifyBucketPayloads() throws IncompatibleTestMatrixException {
        {
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setLongValue(-1L);
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setLongValue(1L);
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setLongValue(1L);
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "longValue", null));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid("all payloads of the same type", matrix, requiredTests);
        }
        {
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setLongValue(-1L);
            buckets.get(0).setPayload(p);
            // bucket 1 is missing a payload here.
            p = new Payload();
            p.setLongValue(1L);
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "longValue", null));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid("not all payloads of the test defined", matrix, requiredTests);
        }
        {
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setStringValue("inact");
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setStringValue("foo");
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setStringValue("bar");
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "longValue", null));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid("all payloads of the wrong type", matrix, requiredTests);
        }
        {
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setLongValue(-1L);
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setLongValue(0L);
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setStringValue("foo");
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "longValue", null));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally inconsistent matrix: different payload types in test
            assertInvalid("all payloads not of the same type", matrix, requiredTests);
        }
        {
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setStringArray(new String[]{});  // empty arrays are allowed.
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setStringArray(new String[]{"foo", "bar"});
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setStringArray(new String[]{"baz", "quux", "xyzzy"});
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "stringArray", null));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid("vector payloads can be different lengths", matrix, requiredTests);
        }
        {
            final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",1.0,"val2","one","val3",new ArrayList<String>()));
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",2.0,"val2","tw","val3",new ArrayList<String>(){{add("a");add("c");}}));
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",2.0,"val2","th","val3",new ArrayList<String>(){{add("foo");add("bar");}}));
            buckets.get(2).setPayload(p);

            final Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets,"map",
                    ImmutableMap.of("val1","doubleValue","val2","stringValue","val3","stringArray"), null));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertValid("correct allocation and object, map with double values", matrix, requiredTests);
        }
        {
            final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",1.0,"val2",3.0,"val3",1.0));
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",2.0,"val3",1.0,"val4",3.0));
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",2.0,"val2",2.0,"val3",2.0));
            buckets.get(2).setPayload(p);

            final Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets,"map",
                    ImmutableMap.of("val1","doubleValue","val2","doubleValue","val3","doubleValue"), null));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid("map payloads can't have different variable names", matrix, requiredTests);
        }
        {
            final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",1.0,"val2","yea1","val3",1.0));
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",2.0,"val2","yea2","val3",3.0));
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",2.0,"val2","yea3","val3",2.0));
            buckets.get(2).setPayload(p);

            final Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets,"map",
                    ImmutableMap.of("val1","doubleValue","val2","doubleValue","val3","doubleValue"), null));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid("map payloads can't have different variable types than specified", matrix, requiredTests);
        }
        {
            final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",1.0,"val2",3.0,"val3",1.0));
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",2.0,"val2",1.0,"val3",3.0));
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",2.0,"val2",2.0,"val3",2.0));
            buckets.get(2).setPayload(p);

            final Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets,"map",
                    ImmutableMap.of("val1","doubleValue","val2","doubleValue","val3","doubleValue","val4","doubleArray"), null));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid("map payloads can't have less variable types than specified", matrix, requiredTests);
        }
        {
            final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",1.0,"val2",3.0,"val3",1.0));
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",2.0,"val2",new ArrayList<Double>(){{add(1.0D);}},"val3",3.0));
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",2.0,"val2",2.0,"val3",2.0));
            buckets.get(2).setPayload(p);

            final Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets,"map",
                    ImmutableMap.of("val1","doubleValue","val2","doubleValue","val3","doubleValue"), null));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid("map payloads can't have different variable types than specified -- an array instead of a single value", matrix, requiredTests);
        }
        {
            final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",1.0,"val2",ImmutableMap.<String,Object>of("a",1,"b",2),"val3",1.0));
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",2.0,"val2",ImmutableMap.<String,Object>of("c",3,"d",4),"val3",3.0));
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",2.0,"val2",ImmutableMap.<String,Object>of("e",5,"f",6),"val3",2.0));
            buckets.get(2).setPayload(p);

            final Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets,"map",
                    ImmutableMap.of("val1","doubleValue","val2","map","val3","doubleValue"), null));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid("map payloads can't nested map payloads", matrix, requiredTests);
        }
        {
            final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",1.0,"val2","one","val3",new ArrayList<String>()));
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",2.0,"val2","tw","val3",new ArrayList<String>(){{add("a");add("c");}}));
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",2.0,"val2","th","val3",new ArrayList(){{add(2.1D);add("bar");}}));
            buckets.get(2).setPayload(p);

            final Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets,"map",
                    ImmutableMap.of("val1","doubleValue","val2","stringValue","val3","stringArray"), null));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            assertInvalid("map payload nested arrays can't have multiple types", matrix, requiredTests);
        }
    }

    @Test
    public void verifyBucketPayloadValueValidators() throws IncompatibleTestMatrixException {
        {
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setDoubleValue(0D);
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setDoubleValue(10D);
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setDoubleValue(20D);
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "doubleValue", "${value >= 0}"));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid("doubleValue: all payload values pass validation", matrix, requiredTests);
        }
        {
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setDoubleValue(0D);
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setDoubleValue(10D);
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setDoubleValue(-1D);
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "doubleValue", "${value >= 0}"));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid("doubleValue: a payload value doesn't pass validation", matrix, requiredTests);
        }
        {
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setLongValue(0L);
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setLongValue(10L);
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setLongValue(20L);
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "longValue", "${value >= 0}"));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid("longValue: all payload values pass validation", matrix, requiredTests);
        }
        {
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setLongValue(0L);
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setLongValue(10L);
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setLongValue(-1L);
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "longValue", "${value >= 0}"));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid("longValue: a payload value doesn't pass validation", matrix, requiredTests);
        }
        {
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setStringValue("inactive");
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setStringValue("foo");
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setStringValue("bar");
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "stringValue", "${value >= \"b\"}"));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid("stringValue: all payload values pass validation", matrix, requiredTests);
        }
        {
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setStringValue("inactive");
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setStringValue("foo");
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setStringValue("abba");
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "stringValue", "${value >= \"b\"}"));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid("stringValue: a payload value doesn't pass validation", matrix, requiredTests);
        }
        {
            final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",1.0,"val2",3.0,"val3",1.0));
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",2.0,"val2",4.0,"val3",1.0));
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",2.0,"val2",2.0,"val3",2.0));
            buckets.get(2).setPayload(p);

            final Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "map", ImmutableMap.of("val1","doubleValue","val2","doubleValue","val3","doubleValue"), "${val1 + val2 + val3 < 10}"));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid("map: a payload value that should pass validation", matrix, requiredTests);
        }
        {
            final List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",1.0,"val2",3.0,"val3",1.0));
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",2.0,"val2",4.0,"val3",1.0));
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setMap(ImmutableMap.<String,Object>of("val1",2.0,"val2",6.0,"val3",2.0));
            buckets.get(2).setPayload(p);

            final Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "map", ImmutableMap.of("val1","doubleValue","val2","doubleValue","val3","doubleValue"), "${val1 + val2 + val3 < 10}"));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertInvalid("map: a payload value doesn't pass validation", matrix, requiredTests);
        }
    }

    public void verifyBucketPayloadArrayValidators() throws IncompatibleTestMatrixException {
        // TODO(pwp): add
    }

    @Test
    public void verifyPayloadDeploymentScenerios() throws IncompatibleTestMatrixException {
        {
            // Proctor should not break if it consumes a test matrix
            // that has a payload even if it's not expecting one.
            // (So long as all the payloads are of the same type.)
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setLongValue(-1L);
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setLongValue(0L);
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setLongValue(1L);
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets)); // no payload type specified
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid("no payload expected; payloads supplied of the same type", matrix, requiredTests);

            // Should have gotten back a test.
            assertEquals("required tests should not be removed from the matrix", 1, matrix.getTests().size());

            // Make sure we don't have any payloads in the resulting tests.
            for (Entry<String, ConsumableTestDefinition> next : matrix.getTests().entrySet()) {
                final ConsumableTestDefinition testDefinition = next.getValue();

                for (final TestBucket bucket : testDefinition.getBuckets()) {
                    assertNull(bucket.getPayload());
                }
            }
        }
        {
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
            Payload p = new Payload();
            p.setLongValue(-1L);
            buckets.get(0).setPayload(p);
            p = new Payload();
            p.setLongValue(0L);
            buckets.get(1).setPayload(p);
            p = new Payload();
            p.setStringValue("foo");
            buckets.get(2).setPayload(p);

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets)); // no payload type specified
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally inconsistent matrix: different payload types in test
            assertInvalid("no payload expected; payloads supplied of different types", matrix, requiredTests);
        }
        {
            // Proctor should not break if it consumes a test matrix
            // with no payloads when it is expecting one.
            List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");

            Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets, "longValue", null));
            final Map<String, ConsumableTestDefinition> tests = Maps.newHashMap();

            tests.put(TEST_A, constructDefinition(buckets, fromCompactAllocationFormat("-1:0.2,0:0.4,1:0.4")));

            final TestMatrixArtifact matrix = constructArtifact(tests);

            // internally consistent matrix
            assertValid("payload expected; no payloads supplied", matrix, requiredTests);
            // Should have gotten back a test.
            assertEquals("required tests should not be removed from the matrix", 1, matrix.getTests().size());
        }
    }

    @Test
    public void testCompactAllocationFormat() {
//        List<Allocation> allocations_empty = fromCompactAllocationFormat("");
//        assertEquals(0, allocations_empty.size());
        double DELTA = 0;

        List<Allocation> allocations = fromCompactAllocationFormat("ruleA|10:0,11:0.5,12:0.5", "0:0,1:0.5,2:0.5");
        assertEquals(2, allocations.size());
        {
            Allocation allocationA = allocations.get(0);
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
            Allocation allocationB = allocations.get(1);
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
        List<TestBucket> buckets = fromCompactBucketFormat("zero:0,one:1,two:2");

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
        final InputStream input = Preconditions.checkNotNull(getClass().getResourceAsStream(PATH_UNKNOWN_TEST_TYPE), "Missing test definition");
        final ConsumableTestDefinition test = Serializers.lenient().readValue(input, ConsumableTestDefinition.class);

        List<TestBucket> buckets = fromCompactBucketFormat("inactive:-1,control:0,test:1");
        final Map<String, TestSpecification> requiredTests = ImmutableMap.of(TEST_A, transformTestBuckets(buckets));
        final Map<String, ConsumableTestDefinition> tests = ImmutableMap.of(TEST_A, test);

        {
            final TestMatrixArtifact matrix = constructArtifact(tests);

            // verifyAndConsolidate should not throw an error because the 'invalidbuckets' test is not required.
            assertEquals(1, matrix.getTests().size());
            assertInvalid("Test not recognized, replaced with 'invalid' marker test", matrix, requiredTests);
            assertEquals(1, matrix.getTests().size());
            final ConsumableTestDefinition replacement = matrix.getTests().values().iterator().next();
            assertEquals(TestType.RANDOM, replacement.getTestType());
            assertEquals("Expected buckets to expand to include all buckets from the specification",
                         buckets.size(),
                         replacement.getBuckets().size());
            assertEquals(-1, replacement.getBuckets().iterator().next().getValue());
        }

        final TestType unrecognizedTestType = TestType.register("UNRECOGNIZED");
        {
            final TestMatrixArtifact matrix = constructArtifact(tests);

            // verifyAndConsolidate should not throw an error because the 'invalidbuckets' test is not required.
            assertEquals(1, matrix.getTests().size());
            final ConsumableTestDefinition original = matrix.getTests().values().iterator().next();
            assertEquals("Expected only the control and test buckets", 2, original.getBuckets().size());

            assertValid("Test now valid", matrix, requiredTests);
            assertEquals(1, matrix.getTests().size());
            final ConsumableTestDefinition stillOriginal = matrix.getTests().values().iterator().next();
            assertEquals(unrecognizedTestType, stillOriginal.getTestType());
            assertEquals("Expected buckets to expand to include all buckets from the specification",
                         buckets.size(),
                         stillOriginal.getBuckets().size());
        }
    }

    @Test
    public void testGenerateSpecificationFromEmptyDefinition() {
        final String description = "this is an empty test with no buckets";
        final TestDefinition empty = new TestDefinition(
            "empty",
            "",
            TestType.ANONYMOUS_USER,
            "salty",
            Collections.<TestBucket>emptyList(),
            Collections.<Allocation>emptyList(),
            Collections.<String, Object>emptyMap(),
            Collections.<String, Object>emptyMap(),
            description
        );
        final TestSpecification specification = ProctorUtils.generateSpecification(empty);
        assertEquals(description, specification.getDescription());
        assertEquals(0, specification.getBuckets().size());
        assertEquals(-1, specification.getFallbackValue());
        assertNull(specification.getPayload());
    }

    @Test
    public void testGenerateSpecificationWithBuckets() {
        final String description = "this test has 3 buckets";
        final TestBucket control = new TestBucket("control", 0, "control bucket");
        final TestBucket inactiveBucket = new TestBucket("inactive", -3, "status quo");
        final TestBucket test = new TestBucket("test", 1, "test bucket");
        final TestDefinition empty = new TestDefinition(
            "buckets",
            "",
            TestType.ANONYMOUS_USER,
            "salty",
            Lists.newArrayList(control, inactiveBucket, test),
            Collections.<Allocation>emptyList(),
            Collections.<String, Object>emptyMap(),
            Collections.<String, Object>emptyMap(),
            description
        );
        final TestSpecification specification = ProctorUtils.generateSpecification(empty);
        assertEquals(description, specification.getDescription());
        assertEquals(3, specification.getBuckets().size());
        assertEquals(inactiveBucket.getValue(), specification.getFallbackValue());
        assertNull(specification.getPayload());
        final Map<String, Integer> buckets = specification.getBuckets();
        assertEquals(inactiveBucket.getValue(), (int) buckets.get(inactiveBucket.getName()));
        assertEquals(control.getValue(), (int) buckets.get(control.getName()));
        assertEquals(test.getValue(), (int) buckets.get(test.getName()));
        // buckets should be ordered by value ascending
        final List<Integer> values = Lists.newArrayList(buckets.values());
        assertEquals(inactiveBucket.getValue(), values.get(0).intValue());
        assertEquals(control.getValue(), values.get(1).intValue());
        assertEquals(test.getValue(), values.get(2).intValue());


    }

    @Test
    public void testGenerateSpecificationPayload() {
        final String description = "this test has a payload buckets";
        final TestBucket inactiveBucket = new TestBucket("inactive", 0, "status quo");
        final Payload inactivePayload = new Payload();
        inactivePayload.setDoubleArray(new Double[] { 1.4d, 4.5d });
        inactiveBucket.setPayload(inactivePayload);
        final TestBucket control = new TestBucket("control", 0, "control bucket");
        final Payload controlPayload = new Payload();
        controlPayload.setDoubleArray(new Double[]{0.0, 2.4d});
        control.setPayload(controlPayload);
        final TestBucket test = new TestBucket("test", 1, "test bucket");
        final Payload testPayload = new Payload();
        testPayload.setDoubleArray(new Double[]{22.22, 33.33});
        test.setPayload(controlPayload);
        final TestDefinition empty = new TestDefinition(
            "buckets",
            "",
            TestType.ANONYMOUS_USER,
            "salty",
            Lists.newArrayList(inactiveBucket, control, test),
            Collections.<Allocation>emptyList(),
            Collections.<String, Object>emptyMap(),
            Collections.<String, Object>emptyMap(),
            description
        );
        final TestSpecification specification = ProctorUtils.generateSpecification(empty);
        assertEquals(description, specification.getDescription());
        assertEquals(3, specification.getBuckets().size());
        assertEquals(inactiveBucket.getValue(), specification.getFallbackValue());
        final PayloadSpecification payload = specification.getPayload();
        assertNotNull(payload);
        assertEquals(PayloadType.DOUBLE_ARRAY.payloadTypeName, payload.getType());
        assertNull(payload.getSchema());
        assertNull(payload.getValidator());
        final Map<String, Integer> buckets = specification.getBuckets();
        assertEquals(inactiveBucket.getValue(), (int) buckets.get(inactiveBucket.getName()));
        assertEquals(control.getValue(), (int) buckets.get(control.getName()));
        assertEquals(test.getValue(), (int) buckets.get(test.getName()));
    }

    @Test
    public void testGenerateSpecificationPayloadMapSchema() {
        final String description = "this test has a payload buckets";
        final TestBucket bucket = new TestBucket("inactive", -3, "status quo");
        final Payload inactivePayload = new Payload();
        inactivePayload.setMap(ImmutableMap.<String, Object>of(
            "da", new Double[] { 1.4d, 4.5d },
            "lv", 5L,
            "sa", new String[] { "foo", "bar" }
        ));
        bucket.setPayload(inactivePayload);
        final TestDefinition empty = new TestDefinition(
            "buckets",
            "",
            TestType.ANONYMOUS_USER,
            "salty",
            Collections.singletonList(bucket),
            Collections.<Allocation>emptyList(),
            Collections.<String, Object>emptyMap(),
            Collections.<String, Object>emptyMap(),
            description
        );
        final TestSpecification specification = ProctorUtils.generateSpecification(empty);
        assertEquals(description, specification.getDescription());
        assertEquals(1, specification.getBuckets().size());
        assertEquals(bucket.getValue(), specification.getFallbackValue());
        final PayloadSpecification payload = specification.getPayload();
        assertNotNull(payload);
        assertEquals(PayloadType.MAP.payloadTypeName, payload.getType());
        final Map<String, String> schema = payload.getSchema();
        assertNotNull(schema);
        assertEquals(3, schema.size());
        assertEquals(PayloadType.DOUBLE_ARRAY.payloadTypeName, schema.get("da"));
        assertEquals(PayloadType.STRING_ARRAY.payloadTypeName, schema.get("sa"));
        assertEquals(PayloadType.LONG_VALUE.payloadTypeName, schema.get("lv"));
        assertNull(payload.getValidator());

        final Map<String, Integer> buckets = specification.getBuckets();
        assertEquals(bucket.getValue(), (int) buckets.get(bucket.getName()));
    }


    /* Test Helper Methods Below */

    private void assertInvalid(String msg, TestMatrixArtifact matrix, Map<String, TestSpecification> requiredTests) throws IncompatibleTestMatrixException {
        assertErrorCreated(false, true, msg, matrix, requiredTests);
    }
    private void assertMissing(String msg, TestMatrixArtifact matrix, Map<String, TestSpecification> requiredTests) throws IncompatibleTestMatrixException {
        assertErrorCreated(true, false, msg, matrix, requiredTests);
    }
    private void assertValid(String msg, TestMatrixArtifact matrix, Map<String, TestSpecification> requiredTests) throws IncompatibleTestMatrixException {
        assertErrorCreated(false, false, msg, matrix, requiredTests);
    }
    private void assertErrorCreated(boolean hasMissing, boolean hasInvalid, String msg, TestMatrixArtifact matrix, Map<String, TestSpecification> requiredTests) throws IncompatibleTestMatrixException {
        final ProctorLoadResult proctorLoadResult = ProctorUtils.verifyAndConsolidate(matrix, "[ testcase: " + msg + " ]", requiredTests, RuleEvaluator.FUNCTION_MAPPER);

        final Set<String> missingTests = proctorLoadResult.getMissingTests();
        assertEquals(msg + " missing tests is not empty", hasMissing, !missingTests.isEmpty());

        final Set<String> testsWithErrors = proctorLoadResult.getTestsWithErrors();
        assertEquals(msg+ " invalid tests is not empty", hasInvalid, !testsWithErrors.isEmpty());
    }


    private TestMatrixArtifact constructArtifact(Map<String, ConsumableTestDefinition> tests) {
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

    private ConsumableTestDefinition constructDefinition(List<TestBucket> buckets,
                                                         List<Allocation> allocations) {

        final ConsumableTestDefinition test = new ConsumableTestDefinition();
        test.setVersion(""); // don't care about version for this test
        test.setSalt(null); // don't care about salt for this test
        test.setRule(null); // don't care about rule for this test
        test.setTestType(TestType.ANONYMOUS_USER);    // don't really care, but need a valid value
        test.setConstants(Collections.<String, Object>emptyMap()); // don't care about constants for this test

        test.setBuckets(buckets);
        test.setAllocations(allocations);
        return test;
    }

    /*  *********************************************************************
        The compact format is used because it's easier to quickly list bucket
        allocations and bucket values than to use string JSON
     *  ********************************************************************* */

    private List<Allocation> fromCompactAllocationFormat(String ... allocations) {
        final List<String> allocationList = Lists.newArrayListWithExpectedSize(allocations.length);
        for(String s : allocations) {
            allocationList.add(s);
        }
        return fromCompactAllocationFormat(allocationList);
    }
    private List<Allocation> fromCompactAllocationFormat(List<String> allocations) {
        final List<Allocation> allocationList = Lists.newArrayListWithExpectedSize(allocations.size());
        // rule|0:0,0:.0.1,0:.2
        for(String allocation : allocations) {
            final String[] parts = allocation.split("\\|");
            final String rule;
            final String sRanges;
            if(parts.length == 1) {
                rule = null;
                sRanges = parts[0];
            } else if (parts.length == 2) {
                rule = parts[0];
                sRanges = parts[1];
            } else {
                System.out.println("parts : " + parts.length);
                throw new IllegalArgumentException("Invalid compact allocation format [" + allocation + "], expected: rule|<bucketValue>:<length>, ...<bucketValue-N>:<length-N>.");
            }
            String[] allRanges = sRanges.split(",");
            final List<Range> ranges = Lists.newArrayListWithCapacity(allRanges.length);
            for(String sRange : allRanges) {
                // Could handle index-out of bounds + number formatting exception better.
                String[] rangeParts = sRange.split(":");
                ranges.add(new Range(Integer.parseInt(rangeParts[0], 10), Double.parseDouble(rangeParts[1])));
            }
            allocationList.add(new Allocation(rule, ranges));
        }
        return allocationList;
    }

    private List<TestBucket> fromCompactBucketFormat(String sBuckets){
        String[] bucketParts = sBuckets.split(",");
        List<TestBucket> buckets = Lists.newArrayListWithCapacity(bucketParts.length);
        for(int i = 0; i < bucketParts.length; i++) {
            // Could handle index-out of bounds + number formatting exception better.
            final String[] nameAndValue = bucketParts[i].split(":");
            buckets.add(new TestBucket(nameAndValue[0], Integer.parseInt(nameAndValue[1]), "bucket " + i, null));
        }
        return buckets;
    }

    private TestSpecification transformTestBuckets(List<TestBucket> testBuckets) {
        TestSpecification testSpec = new TestSpecification();
        Map<String, Integer> buckets = Maps.newLinkedHashMap();
        for(TestBucket b : testBuckets) {
            buckets.put(b.getName(), b.getValue());
        }
        testSpec.setBuckets(buckets);
        return testSpec;
    }
    private TestSpecification transformTestBuckets(final List<TestBucket> testBuckets, final String payloadType, final Map<String,String> schema, final String validator) {
        TestSpecification testSpec = transformTestBuckets(testBuckets);
        PayloadSpecification payloadSpec = new PayloadSpecification();
        payloadSpec.setType(payloadType);
        payloadSpec.setValidator(validator);
        payloadSpec.setSchema(schema);
        testSpec.setPayload(payloadSpec);
        return testSpec;
    }
    private TestSpecification transformTestBuckets(List<TestBucket> testBuckets, String payloadType, String validator) {
        TestSpecification testSpec = transformTestBuckets(testBuckets);
        PayloadSpecification payloadSpec = new PayloadSpecification();
        payloadSpec.setType(payloadType);
        payloadSpec.setValidator(validator);
        testSpec.setPayload(payloadSpec);
        return testSpec;
    }


    //
}
