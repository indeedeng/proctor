package com.indeed.proctor.common;

import com.google.common.collect.Maps;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This is perhaps not the greatest abstraction the world has seen; is meant to consolidate common functionality needed for different types of choosers WITHOUT using inheritance
 * @author ketan
 *
 */
public class TestRangeSelector {
    private static final Logger LOGGER = Logger.getLogger(RandomTestChooser.class);

    @Nonnull
    private final String testName;
    @Nonnull
    private final ConsumableTestDefinition testDefinition;
    @Nonnull
    private final String[] rules;
    @Nonnull
    private final TestBucket[][] rangeToBucket;
    private final RuleEvaluator ruleEvaluator;

    TestRangeSelector(@Nonnull final ExpressionFactory expressionFactory, @Nonnull final FunctionMapper functionMapper, final String testName, @Nonnull final ConsumableTestDefinition testDefinition) {
        this(new RuleEvaluator(expressionFactory, functionMapper, testDefinition.getConstants()), testName, testDefinition);
    }

    TestRangeSelector(
            @Nonnull final RuleEvaluator ruleEvaluator,
            @Nonnull final String testName,
            @Nonnull final ConsumableTestDefinition testDefinition
    ) {
        this.ruleEvaluator = ruleEvaluator;

        this.testName = testName;
        this.testDefinition = testDefinition;

        final Map<Integer, TestBucket> bucketValueToTest = Maps.newHashMap();
        for (final TestBucket testBucket : testDefinition.getBuckets()) {
            bucketValueToTest.put(testBucket.getValue(), testBucket);
        }

        final List<Allocation> allocations = testDefinition.getAllocations();
        this.rangeToBucket = new TestBucket[allocations.size()][];
        this.rules = new String[allocations.size()];
        for (int i = 0; i < allocations.size(); i++) {
            final Allocation allocation = allocations.get(i);
            rules[i] = allocation.getRule();
            final List<Range> ranges = allocation.getRanges();
            this.rangeToBucket[i] = new TestBucket[ranges.size()];
            for (int j = 0; j < ranges.size(); j++) {
                this.rangeToBucket[i][j] = bucketValueToTest.get(ranges.get(j).getBucketValue());
            }
        }
    }

    public TestBucket[] getBucketRange(final int index) {
        return rangeToBucket[index];
    }

    public int findMatchingRule(@Nonnull final Map<String, Object> values) {
        try {
            @Nullable final String rule = testDefinition.getRule();
            if (rule != null) {
                if (! evaluateRule(rule, values)) {
                    return -1;
                }
            }

            for (int i = 0; i < rules.length; i++) {
                if (evaluateRule(rules[i], values)) {
                    return i;
                }
            }

        } catch (InvalidRuleException e) {
            LOGGER.error("Failed to evaluate test rules; ", e);
        }

        return -1;
    }

    private boolean evaluateRule(@Nonnull final String rule, @Nonnull final Map<String, Object> values) throws InvalidRuleException {
        try {
            return ruleEvaluator.evaluateBooleanRule(rule, values);

        } catch (@Nonnull final RuntimeException e) {
            throw new InvalidRuleException(e, String.format(
                    "Error evaluating rule '%s' for test '%s': '%s'. Failing evaluation and continuing.",
                    rule, testName, e.getMessage()));
        }
    }

    @Nonnull
    public String[] getRules() {
        return rules;
    }

    @Nonnull
    public ConsumableTestDefinition getTestDefinition() {
        return testDefinition;
    }

    /**
     * Do not evaluate the rule, do not use the pseudo-random allocation algorithm, do not collect $200.
     * This should ONLY be used by privileged code for debugging.
     *
     * @param value bucket number
     * @return a {@link TestBucket} with the specified value or null if none exists
     */
    @Nullable
    public TestBucket getTestBucket(final int value) {
        for (final TestBucket testBucket : testDefinition.getBuckets()) {
            if (testBucket.getValue() == value) {
                return testBucket;
            }
        }
        return null;
    }

    @Nonnull
    public String getTestName() {
        return testName;
    }

    protected void printTestBuckets(@Nonnull final PrintWriter writer, @Nonnull final Map<String, String> parameters) {
        // TODO (parker) 5/4/12 - figure out why jasx RandomChooser uses a ThreadLocal NumberFormatter
        final NumberFormat fmt = NumberFormat.getPercentInstance(Locale.US);
        fmt.setMaximumFractionDigits(2);

        writer.printf("{ ");
        for (final Iterator<Entry<String, String>> iterator = parameters.entrySet().iterator(); iterator.hasNext(); ) {
            final Entry<String, String> entry = iterator.next();
            writer.print(entry.getKey());
            writer.print("'");
            writer.print(entry.getValue());
            writer.print("'");
            if (iterator.hasNext()) {
                writer.print(", ");
            }
        }

        if (testDefinition.getRule() != null) {
            writer.printf(", rule='%s'", testDefinition.getRule());
        }
        final String separator;
        if (testDefinition.getAllocations().size() > 1) {
            separator = ",\n\t";
        } else {
            separator = ", ";
        }
        final List<Allocation> allocations = testDefinition.getAllocations();
        for (final Allocation allocation : allocations) {
            writer.print(separator);
            if (allocation.getRule() != null) {
                writer.printf("rule='%s', ", allocation.getRule());
            }
            final List<Range> ranges = allocation.getRanges();
            for (int j = 0; j < ranges.size(); j++) {
                // ignoring the trailing comma
                if (j > 0) {
                    writer.print(", ");
                }
                final Range range = ranges.get(j);
                writer.printf("%s%d='%s'", testName, range.getBucketValue(), fmt.format(range.getLength()));
            }
        }
        writer.print(" },");
    }
}
