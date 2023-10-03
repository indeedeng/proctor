package com.indeed.proctor.common;

import com.google.common.annotations.VisibleForTesting;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ValueExpression;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Embodies the logic for a single purely random test, including applicability rule and
 * distribution. {@link #choose(Void, java.util.Map)} is the only useful entry point.
 *
 * @author ketan
 */
@VisibleForTesting
class RandomTestChooser implements TestChooser<Void> {
    @Nonnull private final Random random;
    @Nonnull private final TestRangeSelector testRangeSelector;
    @Nonnull private final List<Allocation> allocations;

    public RandomTestChooser(
            final ExpressionFactory expressionFactory,
            final FunctionMapper functionMapper,
            final String testName,
            @Nonnull final ConsumableTestDefinition testDefinition) {
        this(System.nanoTime(), expressionFactory, functionMapper, testName, testDefinition);
    }

    public RandomTestChooser(
            final long seed,
            final ExpressionFactory expressionFactory,
            final FunctionMapper functionMapper,
            final String testName,
            @Nonnull final ConsumableTestDefinition testDefinition) {
        testRangeSelector =
                new TestRangeSelector(expressionFactory, functionMapper, testName, testDefinition);
        allocations = testDefinition.getAllocations();
        random = new Random(seed);
    }

    @Nonnull
    private Map<String, String> getDescriptorParameters() {
        return Collections.singletonMap(
                "type", testRangeSelector.getTestDefinition().getTestType().name());
    }

    @Nonnull
    @Override
    public TestChooser.Result choose(
            @Nullable final String identifier,
            @Nonnull final Map<String, ValueExpression> localContext,
            @Nonnull final Map<String, TestBucket> testGroups,
            @Nonnull final ForceGroupsOptions forceGroupsOptions,
            @Nonnull final Set<TestType> testTypesWithInvalidIdentifier,
            final boolean isRandomEnabled) {
        if (!isRandomEnabled) {
            // skipping here to make it use the fallback bucket.
            return Result.EMPTY;
        }
        return TestChooser.super.choose(null, localContext, testGroups, forceGroupsOptions);
    }

    @Override
    public String toString() {
        final Map<String, String> parameters = getDescriptorParameters();

        final Writer sw = new StringWriter();
        final PrintWriter writer = new PrintWriter(sw);
        testRangeSelector.printTestBuckets(writer, parameters);
        return sw.toString();
    }

    @Override
    public void printTestBuckets(@Nonnull final PrintWriter writer) {
        final Map<String, String> parameters = getDescriptorParameters();
        testRangeSelector.printTestBuckets(writer, parameters);
    }

    @Nullable
    @Override
    public TestBucket getTestBucket(final int value) {
        return testRangeSelector.getTestBucket(value);
    }

    @Override
    @Nonnull
    public String[] getRules() {
        return testRangeSelector.getRules();
    }

    @Override
    @Nonnull
    public ConsumableTestDefinition getTestDefinition() {
        return testRangeSelector.getTestDefinition();
    }

    @Override
    @Nonnull
    public String getTestName() {
        return testRangeSelector.getTestName();
    }

    @Nonnull
    @Override
    public TestChooser.Result chooseInternal(
            @Nullable final Void identifier,
            @Nonnull final Map<String, ValueExpression> localContext,
            @Nonnull final Map<String, TestBucket> testGroups) {
        final int matchingRuleIndex =
                testRangeSelector.findMatchingRuleWithValueExpr(localContext, testGroups);
        if (matchingRuleIndex < 0) {
            return TestChooser.Result.EMPTY;
        }
        // TODO Reimplement
        //noinspection deprecation
        return allocateRandomGroup(matchingRuleIndex);
    }

    /**
     * @deprecated Temporary implementation; this should be more like {@link StandardTestChooser},
     *     with the cutoffs etc. set in the constructor.
     */
    TestChooser.Result allocateRandomGroup(final int matchingRuleIndex) {
        final TestBucket[] matchingBucketRange =
                testRangeSelector.getBucketRange(matchingRuleIndex);
        final Allocation allocation = allocations.get(matchingRuleIndex);
        final List<Range> ranges = allocation.getRanges();

        final double nextDouble = random.nextDouble();
        double current = 0;

        for (final Range range : ranges) {
            final double max = current + range.getLength();
            if (nextDouble < max) {
                final int matchingBucketValue = range.getBucketValue();
                return new Result(
                        getBucketForValue(matchingBucketValue, matchingBucketRange), allocation);
            }
            current = max;
        }
        //  fallback because I don't trust double math to always do the right thing
        return new Result(
                getBucketForValue(
                        ranges.get(ranges.size() - 1).getBucketValue(), matchingBucketRange),
                allocation);
    }

    static TestBucket getBucketForValue(
            final int matchingBucketValue, @Nonnull final TestBucket[] matchingBucketRange) {
        for (final TestBucket bucket : matchingBucketRange) {
            if (matchingBucketValue == bucket.getValue()) {
                return bucket;
            }
        }
        throw new IllegalStateException(
                "Unable to find a bucket with value " + matchingBucketValue);
    }
}
