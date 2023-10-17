package com.indeed.proctor.common;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
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
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.joining;

/**
 * Embodies the logic for a single test, including applicability rule and distribution. {@link
 * #choose(String, Map)} is the only useful entry point.
 *
 * @author ketan
 */
@VisibleForTesting
class StandardTestChooser implements TestChooser<String> {
    @Nonnull private final TestRangeSelector testRangeSelector;
    @Nonnull private final Hasher hasher;
    @Nonnull private final int[][] cutoffs;

    public StandardTestChooser(
            @Nonnull final ExpressionFactory expressionFactory,
            @Nonnull final FunctionMapper functionMapper,
            @Nonnull final String testName,
            @Nonnull final ConsumableTestDefinition testDefinition) {
        this(new TestRangeSelector(expressionFactory, functionMapper, testName, testDefinition));
    }

    @Nonnull
    public TestChooser.Result choose(
            @Nullable final String identifier,
            @Nonnull final Map<String, ValueExpression> localContext,
            @Nonnull final Map<String, TestBucket> testGroups,
            @Nonnull final ForceGroupsOptions forceGroupsOptions,
            @Nonnull final Set<TestType> testTypesWithInvalidIdentifier) {
        final TestType testType = getTestDefinition().getTestType();
        if (testTypesWithInvalidIdentifier.contains(testType)) {
            // skipping here to make it use the fallback bucket.
            return Result.EMPTY;
        }

        if (identifier == null) {
            // No identifier for the testType of this chooser, nothing to do
            return Result.EMPTY;
        }
        return TestChooser.super.choose(identifier, localContext, testGroups, forceGroupsOptions);
    }

    @VisibleForTesting
    StandardTestChooser(@Nonnull final TestRangeSelector selector) {
        this.testRangeSelector = selector;
        this.hasher = newHasherFor(selector);

        final ConsumableTestDefinition testDefinition = selector.getTestDefinition();

        final Map<Integer, TestBucket> bucketValueToTest = Maps.newHashMap();
        for (final TestBucket testBucket : testDefinition.getBuckets()) {
            bucketValueToTest.put(testBucket.getValue(), testBucket);
        }

        final List<Allocation> allocations = testDefinition.getAllocations();
        cutoffs = new int[allocations.size()][];
        for (int i = 0; i < allocations.size(); i++) {
            final Allocation allocation = allocations.get(i);
            final List<Range> ranges = allocation.getRanges();
            cutoffs[i] = constructCutoffArray(allocation.getRule(), ranges);
        }
    }

    @Nonnull
    private static int[] constructCutoffArray(
            @Nullable final String rule, @Nonnull final List<Range> ranges) {
        final int[] cutoffs = new int[ranges.size() - 1];

        double bucketTotal = 0;
        for (int i = 0; i < ranges.size(); i++) {
            bucketTotal += ranges.get(i).getLength();
            if (i < cutoffs.length) {
                cutoffs[i] = (int) (Integer.MIN_VALUE + bucketTotal * Proctor.INT_RANGE);
            }
        }

        //  I hate floating points.  TODO: extract a required precision constant/parameter?
        if (bucketTotal < 0.9999
                || bucketTotal
                        > 1.0001) { //  compensate for FP imprecision.  TODO: determine what these
            // bounds really should be by testing stuff
            throw new IllegalArgumentException(
                    "Buckets with rule "
                            + rule
                            + " don't add up to 1: "
                            + ranges.stream()
                                    .map(r -> Double.toString(r.getLength()))
                                    .collect(joining(" + "))
                            + " = "
                            + bucketTotal);
        }
        return cutoffs;
    }

    public static Hasher newHasherFor(@Nonnull final TestRangeSelector selector) {
        final String salt = Strings.nullToEmpty(selector.getTestDefinition().getSalt());

        @Nonnull final Hasher result;
        // The standard naming convention is to let the test salt be == the test name
        //  The '&' salt-prefix character is used (TEMPORARILY!) as a special flag indicated that
        // the test name
        //  should not be a parameter to the hashing function, thus allowing multiple tests to be
        // 'linked'
        //  through sharing the same prefixed test salt.
        //
        // TODO This test should be replaced with a definitionVersion test once all proctor-consumer
        // applications
        //  have been updated to use a lenient parser and we can safely add to the schema.
        //
        if (salt.startsWith("&")) {
            // Newer tests use the salt-only hasher
            result = new TestSaltHasher(selector);
        } else {
            // Older tests consider the name as well
            result = new TestNameAndSaltHasher(selector);
        }

        return result;
    }

    @Nonnull
    @Override
    public TestChooser.Result chooseInternal(
            @Nullable final String identifier,
            @Nonnull final Map<String, ValueExpression> localContext,
            @Nonnull final Map<String, TestBucket> testGroups) {
        final int matchingRuleIndex =
                testRangeSelector.findMatchingRuleWithValueExpr(localContext, testGroups);
        if (matchingRuleIndex < 0) {
            return Result.EMPTY;
        }

        final Allocation matchingAllocation =
                testRangeSelector.getTestDefinition().getAllocations().get(matchingRuleIndex);

        return new Result(
                chooseBucket(
                        cutoffs[matchingRuleIndex],
                        testRangeSelector.getBucketRange(matchingRuleIndex),
                        Preconditions.checkNotNull(identifier, "Missing identifier")),
                matchingAllocation);
    }

    private TestBucket chooseBucket(
            @Nonnull final int[] matchingCutoffs,
            final TestBucket[] matchingBucketRange,
            @Nonnull final String identifier) {
        final int value = hasher.hash(identifier);
        int i;
        for (i = 0; i < matchingCutoffs.length && value > matchingCutoffs[i]; i++) {
            /* intentionally empty */
        }
        return matchingBucketRange[i];
    }

    private Map<String, String> getDescriptorParameters() {
        final Map<String, String> parameters = Maps.newLinkedHashMap();
        parameters.put("type", testRangeSelector.getTestDefinition().getTestType().name());
        parameters.put("salt", testRangeSelector.getTestDefinition().getSalt());
        return parameters;
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

    @Override
    public TestBucket getTestBucket(final int value) {
        return testRangeSelector.getTestBucket(value);
    }

    @Nonnull
    @Override
    public String[] getRules() {
        return testRangeSelector.getRules();
    }

    @Nonnull
    @Override
    public ConsumableTestDefinition getTestDefinition() {
        return testRangeSelector.getTestDefinition();
    }

    @Nonnull
    @Override
    public String getTestName() {
        return testRangeSelector.getTestName();
    }

    /** @author matts */
    private interface Hasher {
        int hash(@Nonnull String identifier);
    }

    /** @author matts */
    private abstract static class AbstractMD5Hasher implements Hasher {
        private final byte[] bytes;

        public AbstractMD5Hasher(final String salt) {
            this.bytes = salt.getBytes(Charsets.UTF_8);
        }

        @Override
        public int hash(@Nonnull final String identifier) {
            final MessageDigest md = ProctorUtils.createMessageDigest();

            md.update(bytes);
            md.update(identifier.getBytes(Charsets.UTF_8));

            final byte[] digest = md.digest();

            return convertToInt(digest);
        }

        private static int convertToInt(final byte[] digest) {
            final int offset =
                    12; //  arbitrary choice; changing this would reshuffle all groups just like
            // changing the salt
            return (0xff & digest[offset + 0]) << 24
                    | (0xff & digest[offset + 1]) << 16
                    | (0xff & digest[offset + 2]) << 8
                    | (0xff & digest[offset + 3]);
        }
    }

    // Legacy salting technique
    private static class TestNameAndSaltHasher extends AbstractMD5Hasher {
        private TestNameAndSaltHasher(@Nonnull final TestRangeSelector selector) {
            super(extractSalt(selector));
        }

        private static String extractSalt(@Nonnull final TestRangeSelector selector) {
            final String testName = selector.getTestName();
            final ConsumableTestDefinition testDefinition = selector.getTestDefinition();

            return testName + "|" + testDefinition.getSalt();
        }
    }

    // Modern salting technique, allowing multiple tests to be 'linked' through use of identical
    // hashes
    private static class TestSaltHasher extends AbstractMD5Hasher {
        private TestSaltHasher(@Nonnull final TestRangeSelector selector) {
            super(extractSalt(selector));
        }

        private static String extractSalt(@Nonnull final TestRangeSelector selector) {
            final ConsumableTestDefinition testDefinition = selector.getTestDefinition();

            return Strings.nullToEmpty(testDefinition.getSalt());
        }
    }
}
