package com.indeed.proctor.common;

import com.google.common.collect.Maps;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDependency;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ValueExpression;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

/**
 * This is perhaps not the greatest abstraction the world has seen; is meant to consolidate common
 * functionality needed for different types of choosers WITHOUT using inheritance
 *
 * @author ketan
 */
public class TestRangeSelector {
    private static final Logger LOGGER = LogManager.getLogger(TestRangeSelector.class);

    @Nonnull private final String testName;
    @Nonnull private final ConsumableTestDefinition testDefinition;
    @Nonnull private final String[] rules;
    @Nonnull private final TestBucket[][] rangeToBucket;
    private final RuleEvaluator ruleEvaluator;

    TestRangeSelector(
            @Nonnull final ExpressionFactory expressionFactory,
            @Nonnull final FunctionMapper functionMapper,
            final String testName,
            @Nonnull final ConsumableTestDefinition testDefinition) {
        this(
                new RuleEvaluator(expressionFactory, functionMapper, testDefinition.getConstants()),
                testName,
                testDefinition);
    }

    TestRangeSelector(
            @Nonnull final RuleEvaluator ruleEvaluator,
            @Nonnull final String testName,
            @Nonnull final ConsumableTestDefinition testDefinition) {
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

    /** @deprecated Use findMatchingRuleWithValueExpr(Map, Map) instead, which is more efficient. */
    @Deprecated
    public int findMatchingRule(
            @Nonnull final Map<String, Object> values,
            @Nonnull final Map<String, TestBucket> testGroups,
            @Nullable final String identifier) {
        return findMatchingRuleInternal(
                rule -> ruleEvaluator.evaluateBooleanRule(rule, values), testGroups, identifier);
    }

    public int findMatchingRuleWithValueExpr(
            @Nonnull final Map<String, ValueExpression> localContext,
            @Nonnull final Map<String, TestBucket> testGroups,
            @Nullable final String identifier) {
        return findMatchingRuleInternal(
                rule -> ruleEvaluator.evaluateBooleanRuleWithValueExpr(rule, localContext),
                testGroups,
                identifier);
    }

    private int findMatchingRuleInternal(
            final Function<String, Boolean> evaluator,
            @Nonnull final Map<String, TestBucket> testGroups,
            @Nullable final String identifier) {
        final TestDependency dependsOn = testDefinition.getDependsOn();
        if (dependsOn != null) {
            final TestBucket testBucket = testGroups.get(dependsOn.getTestName());
            if ((testBucket == null) || (testBucket.getValue() != dependsOn.getBucketValue())) {
                return -1;
            }
        }

        @Nullable final String rule = testDefinition.getRule();
        try {
            if (rule != null) {
                if (!evaluator.apply(rule)) {
                    return -1;
                }
            }

            return getMatchingAllocation(evaluator, identifier);
        } catch (final RuntimeException e) {
            LOGGER.error(
                    "Failed to evaluate test rule; ",
                    new InvalidRuleException(
                            e,
                            String.format(
                                    "Error evaluating rule '%s' for test '%s': '%s'. Failing evaluation and continuing.",
                                    rule, testName, e.getMessage())));
        }

        return -1;
    }

    protected int getMatchingAllocation(
            final Function<String, Boolean> evaluator, @Nullable final String identifier) {
        int i = 0;
        try {
            for (i = 0; i < rules.length; i++) {
                if (evaluator.apply(rules[i])) {
                    return i;
                }
            }
        } catch (final RuntimeException e) {
            LOGGER.error(
                    "Failed to evaluate test allocation rules; ",
                    new InvalidRuleException(
                            e,
                            String.format(
                                    "Error evaluating rule '%s' for test '%s': '%s'. Failing evaluation and continuing.",
                                    rules[i], testName, e.getMessage())));
        }
        return -1;
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
     * Do not evaluate the rule, do not use the pseudo-random allocation algorithm, do not collect
     * $200. This should ONLY be used by privileged code for debugging.
     *
     * @param value bucket number
     * @return a {@link TestBucket} with the specified value or null if none exists
     */
    @CheckForNull
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

    /** appends testbuckets in a notation a bit similar to Json */
    protected void printTestBuckets(
            @Nonnull final PrintWriter writer, @Nonnull final Map<String, String> parameters) {
        final NumberFormat fmt = NumberFormat.getPercentInstance(Locale.US);
        fmt.setMaximumFractionDigits(2);

        writer.printf("{ ");
        for (final Iterator<Entry<String, String>> iterator = parameters.entrySet().iterator();
                iterator.hasNext(); ) {
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
                writer.printf(
                        "%s%d='%s'",
                        testName, range.getBucketValue(), fmt.format(range.getLength()));
            }
        }
        writer.print(" },");
    }
}
