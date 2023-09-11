package com.indeed.proctor.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestDependency;
import com.indeed.proctor.common.model.TestType;
import org.apache.el.ExpressionFactoryImpl;
import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Test;

import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ValueExpression;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/** @author rboyer */
public class TestStandardTestChooser {

    private ExpressionFactory expressionFactory;
    private FunctionMapper functionMapper;
    private String testName;
    private ConsumableTestDefinition testDefinition;
    private int[] counts;
    private int[] hashes;

    private static final ImmutableList<Range> RANGES_50_50 =
            ImmutableList.of(new Range(-1, 0.0), new Range(0, 0.5), new Range(1, 0.5));
    private static final ImmutableList<Range> RANGES_100_0 =
            ImmutableList.of(new Range(-1, 0.0), new Range(0, 0.0), new Range(1, 1.0));

    private static final List<TestBucket> INACTIVE_CONTROL_TEST_BUCKETS =
            ImmutableList.of(
                    new TestBucket("inactive", -1, "zoot", null),
                    new TestBucket("control", 0, "zoot", null),
                    new TestBucket("test", 1, "zoot", null));

    @Before
    public void setupMocks() throws Exception {
        expressionFactory = new ExpressionFactoryImpl();
        functionMapper = RuleEvaluator.FUNCTION_MAPPER;
        testName = "testName";
        testDefinition = new ConsumableTestDefinition();
        testDefinition.setConstants(Collections.<String, Object>emptyMap());
        testDefinition.setTestType(TestType.AUTHENTICATED_USER);
        // most tests just set the salt to be the same as the test name
        testDefinition.setSalt(testName);
        testDefinition.setBuckets(INACTIVE_CONTROL_TEST_BUCKETS);

        updateAllocations(RANGES_50_50);

        final int effBuckets = INACTIVE_CONTROL_TEST_BUCKETS.size() - 1;
        counts = new int[effBuckets];
        hashes = new int[effBuckets];
    }

    @Test
    public void testSimple_100_0() {
        updateAllocations(RANGES_100_0);
        final StandardTestChooser rtc = newChooser();

        final Map<String, ValueExpression> localContext = Collections.emptyMap();
        for (int i = 0; i < 100; i++) {
            final TestChooser.Result chosen =
                    rtc.chooseInternal(String.valueOf(i), localContext, Collections.emptyMap());
            assertNotNull(chosen);
            assertNotNull(chosen.getTestBucket());
            assertNotNull(chosen.getAllocation());
            assertEquals(1, chosen.getTestBucket().getValue());
            assertEquals("#A1", chosen.getAllocation().getId());
        }
    }

    @Test
    public void testSimple_50_50() {
        testDefinition.setSalt(testName);

        final StandardTestChooser chooser = newChooser();
        exerciseChooser(chooser);

        // uncomment this if you need to recompute these values
        //        for (int i = 0; i < counts.length; i++) System.err.println(i + ": " + counts[i] +
        // " / " + hashes[i]);

        // if this ever fails, it means that something is broken about how tests are split
        // and you should investigate why!

        assertEquals("bucket0 counts", 4999412, counts[0]);
        assertEquals("bucket1 counts", 5000587, counts[1]);

        assertEquals("bucket0 hash", 1863060514, hashes[0]);
        assertEquals("bucket1 hash", 765061458, hashes[1]);
    }

    // constants shared between the next two tests
    private static final int COUNTS_BUCKET0_SALT_AMP_TESTNAME = 4999049;
    private static final int COUNTS_BUCKET1_SALT_AMP_TESTNAME = 5000950;
    private static final int HASH_BUCKET0_SALT_AMP_TESTNAME = 1209398320;
    private static final int HASH_BUCKET1_SALT_AMP_TESTNAME = 494965600;

    @Test
    public void test_50_50_withMagicTestSalt() {
        // Now change the spec version and reevaluate
        testDefinition.setSalt("&" + testName);

        final StandardTestChooser chooser = newChooser();
        exerciseChooser(chooser);

        // uncomment this if you need to recompute these values
        //        for (int i = 0; i < counts.length; i++) System.err.println(i + ": " + counts[i] +
        // " / " + hashes[i]);

        // if this ever fails, it means that something is broken about how tests are split
        // and you should investigate why!

        assertEquals("bucket0 counts", COUNTS_BUCKET0_SALT_AMP_TESTNAME, counts[0]);
        assertEquals("bucket1 counts", COUNTS_BUCKET1_SALT_AMP_TESTNAME, counts[1]);

        assertEquals("bucket0 hash", HASH_BUCKET0_SALT_AMP_TESTNAME, hashes[0]);
        assertEquals("bucket1 hash", HASH_BUCKET1_SALT_AMP_TESTNAME, hashes[1]);
    }

    @Test
    public void test50_50_withMagicTestSalt_and_unrelatedTestName() {
        final String originalTestName = testName;
        testName = "someOtherTestName";
        testDefinition.setSalt("&" + originalTestName);

        final StandardTestChooser chooser = newChooser();
        exerciseChooser(chooser);

        // uncomment this if you need to recompute these values
        //        for (int i = 0; i < counts.length; i++) System.err.println(i + ": " + counts[i] +
        // " / " + hashes[i]);

        // if this ever fails, it means that something is broken about how tests are split
        // and you should investigate why!

        // These values should be the same as in the preceding test
        assertEquals("bucket0 counts", COUNTS_BUCKET0_SALT_AMP_TESTNAME, counts[0]);
        assertEquals("bucket1 counts", COUNTS_BUCKET1_SALT_AMP_TESTNAME, counts[1]);

        assertEquals("bucket0 hash", HASH_BUCKET0_SALT_AMP_TESTNAME, hashes[0]);
        assertEquals("bucket1 hash", HASH_BUCKET1_SALT_AMP_TESTNAME, hashes[1]);
    }

    @Test
    public void testExceptionsDealtWith() {
        final String testName = "test";

        final ConsumableTestDefinition testDefinition = new ConsumableTestDefinition();
        testDefinition.setConstants(Collections.<String, Object>emptyMap());
        testDefinition.setRule("${lang == 'en'}");

        testDefinition.setTestType(TestType.ANONYMOUS_USER);

        // most tests just set the salt to be the same as the test name
        testDefinition.setSalt(testName);
        testDefinition.setBuckets(Collections.<TestBucket>emptyList());

        final RuleEvaluator ruleEvaluator = EasyMock.createMock(RuleEvaluator.class);
        EasyMock.expect(
                        ruleEvaluator.evaluateBooleanRuleWithValueExpr(
                                EasyMock.<String>anyObject(),
                                EasyMock.<Map<String, ValueExpression>>anyObject()))
                // throw an unexpected type of runtime exception
                .andThrow(new RuntimeException() {})
                // Must be evaluated, or this was not a valid test
                .once();
        EasyMock.replay(ruleEvaluator);

        final TestRangeSelector selector =
                new TestRangeSelector(
                        ruleEvaluator, testName, testDefinition, new IdentifierValidator.Noop());

        // Ensure no exceptions thrown.
        final TestChooser.Result chooseResult =
                new StandardTestChooser(selector)
                        .chooseInternal(
                                "identifier",
                                Collections.<String, ValueExpression>emptyMap(),
                                Collections.emptyMap());

        assertNotNull(chooseResult);
        assertNull("Expected no bucket to be found ", chooseResult.getTestBucket());
        assertNull("Expected no allocation to be found ", chooseResult.getAllocation());

        EasyMock.verify(ruleEvaluator);
    }

    @Test
    public void testEvaluateBooleanRuleWithValueExpr() {
        final BiConsumer<String, Boolean> assertEvaluatesTo =
                (rule, value) -> {
                    final RuleEvaluator evaluator =
                            new RuleEvaluator(
                                    ExpressionFactory.newInstance(),
                                    RuleEvaluator.FUNCTION_MAPPER,
                                    Collections.emptyMap());
                    final boolean result =
                            evaluator.evaluateBooleanRuleWithValueExpr(
                                    rule, Collections.emptyMap());
                    assertEquals(value, result);
                };

        // fast-track
        assertEvaluatesTo.accept("", true);
        assertEvaluatesTo.accept("${}", true);
        assertEvaluatesTo.accept("${    }", true);
        assertEvaluatesTo.accept("${true}", true);
        assertEvaluatesTo.accept("${TrUe}", true);
        assertEvaluatesTo.accept("${false}", false);
        assertEvaluatesTo.accept("${faLsE}", false);

        // slow-track
        assertEvaluatesTo.accept("${1 == 0}", false);
        assertEvaluatesTo.accept("${1 == 1}", true);
    }

    @Test
    public void testDefaultAllocationWithNonEmptyRule_fallback() {
        final String testName = "test";

        final ConsumableTestDefinition testDefinition = new ConsumableTestDefinition();
        testDefinition.setConstants(Collections.<String, Object>emptyMap());
        testDefinition.setTestType(TestType.ANONYMOUS_USER);
        testDefinition.setSalt(testName);

        testDefinition.setBuckets(INACTIVE_CONTROL_TEST_BUCKETS);

        final List<Allocation> allocations = Lists.newArrayList();
        allocations.add(new Allocation("${country == 'US'}", RANGES_100_0, "#B1"));
        allocations.add(new Allocation("${country == 'GB'}", RANGES_100_0, "#C1"));
        testDefinition.setAllocations(allocations);

        final RuleEvaluator ruleEvaluator = newRuleEvaluator(false);
        final TestRangeSelector selector =
                new TestRangeSelector(
                        ruleEvaluator, testName, testDefinition, new IdentifierValidator.Noop());

        final TestChooser.Result chooseResult =
                new StandardTestChooser(selector)
                        .chooseInternal(
                                "identifier",
                                Collections.<String, ValueExpression>emptyMap(),
                                Collections.emptyMap());

        assertNotNull(chooseResult);
        assertNull("Expected no bucket to be found", chooseResult.getTestBucket());
        assertNull("Expected no allocation to be found", chooseResult.getAllocation());

        EasyMock.verify(ruleEvaluator);
    }

    @Test
    public void testDefaultAllocationWithNonEmptyRule_match() {
        final String testName = "test";

        final ConsumableTestDefinition testDefinition = new ConsumableTestDefinition();
        testDefinition.setConstants(Collections.<String, Object>emptyMap());
        testDefinition.setRule("${lang == 'en'}");

        testDefinition.setTestType(TestType.ANONYMOUS_USER);
        testDefinition.setSalt(testName);

        testDefinition.setBuckets(INACTIVE_CONTROL_TEST_BUCKETS);

        final List<Allocation> allocations = Lists.newArrayList();
        allocations.add(new Allocation("${country == 'GB'}", RANGES_100_0, "#B1"));
        testDefinition.setAllocations(allocations);

        final RuleEvaluator ruleEvaluator = newRuleEvaluator(true);
        final TestRangeSelector selector =
                new TestRangeSelector(
                        ruleEvaluator, testName, testDefinition, new IdentifierValidator.Noop());

        final TestChooser.Result chooseResult =
                new StandardTestChooser(selector)
                        .chooseInternal(
                                "identifier",
                                Collections.<String, ValueExpression>emptyMap(),
                                Collections.emptyMap());

        assertEquals(
                "Test bucket with value 1 expected", 1, chooseResult.getTestBucket().getValue());
        assertEquals(
                "Test allocation with id #B1 expected",
                "#B1",
                chooseResult.getAllocation().getId());

        EasyMock.verify(ruleEvaluator);
    }

    @Test
    public void testDependency_match() {
        final String testName = "test";

        final ConsumableTestDefinition testDefinition =
                ConsumableTestDefinition.fromTestDefinition(
                        TestDefinition.builder()
                                .setTestType(TestType.ANONYMOUS_USER)
                                .setSalt(testName)
                                .setBuckets(INACTIVE_CONTROL_TEST_BUCKETS)
                                .addAllocations(new Allocation("", RANGES_100_0, "#B1"))
                                .setDependsOn(new TestDependency("par_test", 10))
                                .build());

        final RuleEvaluator ruleEvaluator = newRuleEvaluator(true);
        final TestRangeSelector selector =
                new TestRangeSelector(
                        ruleEvaluator, testName, testDefinition, new IdentifierValidator.Noop());

        final TestChooser.Result chooseResult =
                new StandardTestChooser(selector)
                        .chooseInternal(
                                "identifier",
                                Collections.emptyMap(),
                                ImmutableMap.of("par_test", new TestBucket("", 10, "")));

        assertThat(chooseResult.getTestBucket().getValue()).isEqualTo(1);
        assertThat(chooseResult.getAllocation().getId()).isEqualTo("#B1");
    }

    @Test
    public void testDependency_fallback() {
        final String testName = "test";

        final ConsumableTestDefinition testDefinition =
                ConsumableTestDefinition.fromTestDefinition(
                        TestDefinition.builder()
                                .setTestType(TestType.ANONYMOUS_USER)
                                .setSalt(testName)
                                .setBuckets(INACTIVE_CONTROL_TEST_BUCKETS)
                                .addAllocations(new Allocation("", RANGES_100_0, "#B1"))
                                .setDependsOn(new TestDependency("par_test", 10))
                                .build());

        final RuleEvaluator ruleEvaluator = newRuleEvaluator(true);
        final TestRangeSelector selector =
                new TestRangeSelector(
                        ruleEvaluator, testName, testDefinition, new IdentifierValidator.Noop());

        final TestChooser.Result chooseResult =
                new StandardTestChooser(selector)
                        .chooseInternal(
                                "identifier",
                                Collections.emptyMap(),
                                ImmutableMap.of("par_test", new TestBucket("", 1, "")));

        assertThat(chooseResult.getTestBucket()).isNull();
        assertThat(chooseResult.getAllocation()).isNull();
    }

    private StandardTestChooser newChooser() {
        return new StandardTestChooser(
                expressionFactory,
                functionMapper,
                testName,
                testDefinition,
                new IdentifierValidator.Noop());
    }

    private RuleEvaluator newRuleEvaluator(final boolean result) {
        final RuleEvaluator ruleEvaluator = EasyMock.createMock(RuleEvaluator.class);
        EasyMock.expect(
                        ruleEvaluator.evaluateBooleanRuleWithValueExpr(
                                EasyMock.<String>anyObject(),
                                EasyMock.<Map<String, ValueExpression>>anyObject()))
                .andReturn(result)
                .anyTimes();
        EasyMock.replay(ruleEvaluator);
        return ruleEvaluator;
    }

    private void exerciseChooser(final StandardTestChooser rtc) {
        final int num = 10000000;

        final Map<String, ValueExpression> localContext = Collections.emptyMap();
        for (int accountId = 1; accountId < num; accountId++) { // deliberately skipping 0
            final TestChooser.Result chosen =
                    rtc.chooseInternal(
                            String.valueOf(accountId), localContext, Collections.emptyMap());
            assertNotNull(chosen);
            assertNotNull(chosen.getTestBucket());
            assertNotNull(chosen.getAllocation());

            counts[chosen.getTestBucket().getValue()]++;
            hashes[chosen.getTestBucket().getValue()] =
                    31 * hashes[chosen.getTestBucket().getValue()] + accountId;
        }
    }

    private void updateAllocations(final ImmutableList<Range> ranges) {
        final List<Allocation> allocations = Lists.newArrayList();
        allocations.add(new Allocation("${}", ranges, "#A1"));
        testDefinition.setAllocations(allocations);
    }
}
