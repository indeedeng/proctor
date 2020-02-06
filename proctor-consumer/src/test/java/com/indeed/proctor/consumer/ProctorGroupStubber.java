package com.indeed.proctor.consumer;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

/**
 * Test stub subclasses of AbstractGroups
 */
public class ProctorGroupStubber {

    static final String HOLDOUT_TESTNAME = "holdout_tst";
    static final String CONTROL_TESTNAME = "bgtst";
    static final String ACTIVE_TESTNAME = "abtst";
    static final String GROUP_WITH_FALLBACK_TESTNAME = "groupwithfallbacktst";
    static final String INACTIVE_TESTNAME = "btntst";

    // proctor-test to test situation where bucket and allocation is available but definition is not.
    // This is expected to be happen only in artificial case such as in testing.
    static final String NO_DEFINITION_TESTNAME = "no_definition_tst";

    static final String NO_BUCKETS_WITH_FALLBACK_TESTNAME = "nobucketfallbacktst";
    static final Bucket FALLBACK_BUCKET = createModelBucket(42);
    static final Bucket FALLBACK_NOPAYLOAD_BUCKET = createModelBucket(66);

    static ProctorResult buildProctorResult() {
        final TestBucket inactiveBucket = new TestBucket("inactive", -1, "inactive");
        final TestBucket controlBucketWithPayload = new TestBucket("control", 0, "control", new Payload("controlPayload"));
        final TestBucket activeBucketWithPayload = new TestBucket("active", 1, "active", new Payload("activePayload"));
        final TestBucket activeBucket = new TestBucket("active", 2, "active");
        return new ProctorResult(
                "0",
                // buckets
                ImmutableMap.<String, TestBucket>builder()
                        .put(HOLDOUT_TESTNAME, activeBucket)
                        .put(CONTROL_TESTNAME, controlBucketWithPayload)
                        .put(ACTIVE_TESTNAME, activeBucketWithPayload)
                        .put(GROUP_WITH_FALLBACK_TESTNAME, activeBucket)
                        .put(INACTIVE_TESTNAME, inactiveBucket)
                        .put(NO_DEFINITION_TESTNAME, activeBucket)
                        .build(),
                // allocations
                ImmutableMap.<String, Allocation>builder()
                        .put(HOLDOUT_TESTNAME, new Allocation(null, Arrays.asList(new Range(activeBucket.getValue(), 1.0)), "#A1"))
                        .put(CONTROL_TESTNAME, new Allocation(null, Arrays.asList(new Range(controlBucketWithPayload.getValue(), 1.0)), "#A1"))
                        .put(ACTIVE_TESTNAME, new Allocation(null, Arrays.asList(new Range(activeBucketWithPayload.getValue(), 1.0)), "#B2"))
                        .put(GROUP_WITH_FALLBACK_TESTNAME, new Allocation(null, Arrays.asList(new Range(activeBucket.getValue(), 1.0)), "#B2"))
                        .put(INACTIVE_TESTNAME, new Allocation(null, Arrays.asList(new Range(inactiveBucket.getValue(), 1.0)), "#C3"))
                        .put(NO_DEFINITION_TESTNAME, new Allocation(null, Arrays.asList(new Range(activeBucket.getValue(), 1.0)), "#A5"))
                        .build(),
                // definitions
                ImmutableMap.<String, ConsumableTestDefinition>builder()
                        .put(HOLDOUT_TESTNAME, stubDefinitionWithVersion("vInactive", inactiveBucket, activeBucket))
                        .put(CONTROL_TESTNAME, stubDefinitionWithVersion("vControl", inactiveBucket, controlBucketWithPayload, activeBucketWithPayload))
                        .put(ACTIVE_TESTNAME, stubDefinitionWithVersion("vActive", inactiveBucket, controlBucketWithPayload, activeBucketWithPayload))
                        .put(INACTIVE_TESTNAME, stubDefinitionWithVersion("vInactive", inactiveBucket, activeBucket))
                        .put(GROUP_WITH_FALLBACK_TESTNAME, stubDefinitionWithVersion(
                                "vGroupWithFallback",
                                new TestBucket(
                                        "fallbackBucket",
                                        FALLBACK_BUCKET.getValue(),
                                        "fallbackDesc",
                                        new Payload("fallback")),
                                inactiveBucket, activeBucket))
                        // has no buckets in result, but in definition
                        .put(NO_BUCKETS_WITH_FALLBACK_TESTNAME, stubDefinitionWithVersion(
                                "vNoBuckets",
                                new TestBucket(
                                        "fallbackBucket",
                                        FALLBACK_BUCKET.getValue(),
                                        "fallbackDesc",
                                        new Payload("fallback")),
                                inactiveBucket, activeBucket))
                        .build()
        );
    }

    private static ConsumableTestDefinition stubDefinitionWithVersion(final String version, final TestBucket... buckets) {
        final ConsumableTestDefinition testDefinition = new ConsumableTestDefinition();
        testDefinition.setVersion(version);
        testDefinition.setBuckets(Arrays.asList(buckets));
        return testDefinition;
    }

    static class ProctorGroupsForTest extends AbstractGroups {
        ProctorGroupsForTest(final ProctorResult proctorResult) {
            super(proctorResult);
        }
    }

    /**
     * This is one simple example of a holdout-groupsWithCustom implementation that uses a hardcoded hold-out experiment,
     * applies hold-out to all other experiments, uses the bucket with the smallest value in hold-out case,
     * and uses the fallback value for most error cases.
     * <p>
     * Better implementations might use meta-tags or other properties to identify hold-out experiment, and
     * also to identify experiments subject to hold-out groupsWithCustom, and have better strategies for selecting
     * the hold-out bucket to use.
     */
    static class ProctorGroupsWithHoldout extends ProctorGroupsForTest {
        ProctorGroupsWithHoldout(final ProctorResult proctorResult) {
            super(proctorResult);
        }

        @Override
        protected int overrideDeterminedBucketValue(final String testName, @Nonnull final TestBucket determinedBucket) {
            // for other experiments, if hold-out experiment is active, use bucket with value -1 if available.
            if (!HOLDOUT_TESTNAME.equals(testName) && isBucketActive(HOLDOUT_TESTNAME, 2, -1)) {
                // return bucket with smallest value
                return Optional.ofNullable(getProctorResult().getTestDefinitions().get(testName))
                        .map(ConsumableTestDefinition::getBuckets)
                        .flatMap(buckets -> buckets.stream().min(Comparator.comparing(TestBucket::getValue)))
                        .map(TestBucket::getValue)
                        .orElse(determinedBucket.getValue());
            }
            return determinedBucket.getValue();
        }
    }

    /**
     * This is one simple example modifying a testbucket for whatever purpose.
     * Some purposes could be to implement sub-experiments, or have special environments with forced groups.
     */
    static class ProctorGroupsWithForced extends ProctorGroupsForTest {
        ProctorGroupsWithForced(final ProctorResult proctorResult) {
            super(proctorResult);
        }

        @Override
        protected int overrideDeterminedBucketValue(final String testName, @Nonnull final TestBucket determinedBucket) {
            // for other experiments, if hold-out experiment is active, use bucket with value -1 if available.
            if (ACTIVE_TESTNAME.equals(testName)) {
                // return bucket with control value
                return Optional.ofNullable(getProctorResult().getTestDefinitions().get(testName))
                        .map(ConsumableTestDefinition::getBuckets)
                        // use control bucket instead of active
                        .flatMap(buckets -> buckets.stream().filter(b -> b.getValue() == 0).findFirst())
                        .map(TestBucket::getValue)
                        .orElse(determinedBucket.getValue());
            }
            return determinedBucket.getValue();
        }
    }

    private static Bucket createModelBucket(final int value) {
        return new Bucket() {
            @Override
            public Enum getTest() {
                return null;
            }

            @Override
            public int getValue() {
                return value;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public String getFullName() {
                return null;
            }
        };
    }

    static class StubTest implements Test {

        private final String name;
        private final int value;

        StubTest(final String name, final int value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getFallbackValue() {
            return value;
        }
    }
}
