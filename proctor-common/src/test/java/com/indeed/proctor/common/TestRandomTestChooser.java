package com.indeed.proctor.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;

import org.apache.el.ExpressionFactoryImpl;
import org.junit.Test;

import com.google.common.collect.Lists;

public class TestRandomTestChooser {
    @Test
    public void test100Percent() {
        final List<Range> ranges = Lists.newArrayList(new Range(-1, 0.0), new Range(0, 0.0), new Range(1, 1.0));
        final List<TestBucket> buckets = Lists.newArrayList(new TestBucket("inactive", -1, "zoot", null), new TestBucket("control", 0, "zoot", null), new TestBucket("test", 1, "zoot", null));

        final RandomTestChooser rtc = initializeRandomTestChooser(ranges, buckets);

        final Map<String, Object> values = Collections.emptyMap();
        for (int i = 0; i < 100; i++) {
            final TestChooser.Result chosen = rtc.choose(null, values);
            assertNotNull(chosen);
            assertNotNull(chosen.getTestBucket());
            assertNotNull(chosen.getAllocation());
            assertEquals("#A1", chosen.getAllocation().getId());
            assertEquals(1, chosen.getTestBucket().getValue());
        }
    }

    @Test
    public void test5050Percent() {
        final List<Range> ranges = Lists.newArrayList(new Range(0, 0.5), new Range(1, 1.0));
        final List<TestBucket> buckets = Lists.newArrayList(new TestBucket("control", 0, "zoot", null), new TestBucket("test", 1, "zoot", null));

        final RandomTestChooser rtc = initializeRandomTestChooser(ranges, buckets);

        int[] found = { 0, 0 };
        final Map<String, Object> values = Collections.emptyMap();
        for (int i = 0; i < 1000; i++) {
            final TestChooser.Result chosen = rtc.choose(null, values);
            assertNotNull(chosen);
            assertNotNull(chosen.getTestBucket());
            assertNotNull(chosen.getAllocation());
            assertEquals("#A1", chosen.getAllocation().getId());
            found[chosen.getTestBucket().getValue()]++;
        }

        assertTrue(found[0] > 400);
        assertTrue(found[0] < 600);
        assertTrue(found[1] > 400);
        assertTrue(found[1] < 600);
    }

    @Test
    public void test333333Percent() {
        final List<Range> ranges = Lists.newArrayList(new Range(0, 0.3333333333333333), new Range(1, 0.3333333333333333), new Range(2, 0.3333333333333333));
        final List<TestBucket> buckets = Lists.newArrayList(new TestBucket("inactive", 0, "zoot", null), new TestBucket("control", 1, "zoot", null), new TestBucket("test", 2, "zoot", null));

        final RandomTestChooser rtc = initializeRandomTestChooser(ranges, buckets);

        int[] found = { 0, 0, 0 };
        final Map<String, Object> values = Collections.emptyMap();
        for (int i = 0; i < 1000; i++) {
            final TestChooser.Result chosen = rtc.choose(null, values);
            assertNotNull(chosen);
            assertNotNull(chosen.getTestBucket());
            assertNotNull(chosen.getAllocation());
            assertEquals("#A1", chosen.getAllocation().getId());
            found[chosen.getTestBucket().getValue()]++;
        }

        assertTrue(found[0] > 250);
        assertTrue(found[0] < 400);
        assertTrue(found[1] > 250);
        assertTrue(found[1] < 400);
        assertTrue(found[2] > 250);
        assertTrue(found[2] < 400);
    }

    static RandomTestChooser initializeRandomTestChooser(final List<Range> ranges, final List<TestBucket> buckets) {
        final ExpressionFactory expressionFactory = new ExpressionFactoryImpl();

        final FunctionMapper functionMapper = RuleEvaluator.FUNCTION_MAPPER;

        final ConsumableTestDefinition testDefinition = new ConsumableTestDefinition();
        testDefinition.setConstants(Collections.<String, Object>emptyMap());

        testDefinition.setBuckets(buckets);

        final List<Allocation> allocations = Lists.newArrayList();
        allocations.add(new Allocation("${}", ranges, "#A1"));
        testDefinition.setAllocations(allocations);

        final RandomTestChooser rtc = new RandomTestChooser(expressionFactory, functionMapper, "testName", testDefinition);
        return rtc;
    }
}
