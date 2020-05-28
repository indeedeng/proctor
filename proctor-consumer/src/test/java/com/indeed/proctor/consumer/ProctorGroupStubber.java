package com.indeed.proctor.consumer;

import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Test stub subclasses of AbstractGroups
 */
public class ProctorGroupStubber {
    static final Bucket<StubTest> FALLBACK_BUCKET = createModelBucket(StubTest.GROUP_WITH_FALLBACK_TEST, 42);
    static final Bucket<StubTest> FALLBACK_NOPAYLOAD_BUCKET = createModelBucket(StubTest.NO_BUCKETS_WITH_FALLBACK_TEST, 66);

    // Same Buckets reused for multiple tests, just for simplicity (would not happen in proctor)
    public static final TestBucket INACTIVE_BUCKET = new TestBucket("inactive", -1, "inactive");
    public static final TestBucket CONTROL_BUCKET_WITH_PAYLOAD = new TestBucket("control", 0, "control", new Payload("controlPayload"));
    public static final TestBucket GROUP_1_BUCKET_WITH_PAYLOAD = new TestBucket("group1", 1, "group1", new Payload("activePayload"));
    public static final TestBucket GROUP_1_BUCKET = new TestBucket("group1", 2, "group1");
    public static final TestBucket FALLBACK_TEST_BUCKET = new TestBucket(
            "fallbackBucket",
            FALLBACK_BUCKET.getValue(),
            "fallbackDesc",
            new Payload("fallback"));

    /**
     * Builds up a Proctor Result with given test definitions and selected buckets
     */
    static class ProctorResultStubBuilder {

        private Map<StubTest, TestBucket[]> definedBucketSets = new TreeMap<>();
        private Map<StubTest, TestBucket> resolvedBuckets = new TreeMap<>();

        public ProctorResultStubBuilder withStubTest(
                final StubTest stubTest,
                @Nullable final TestBucket resolved,
                final TestBucket... definedBuckets) {
            if (resolved != null) {
                resolvedBuckets.put(stubTest, resolved);
            }
            if (definedBuckets.length > 0) {
                definedBucketSets.put(stubTest, definedBuckets);
            }
            return this;
        }

        ProctorResult build() {
            return new ProctorResult(
                    "0",
                    resolvedBuckets.entrySet().stream()
                            .collect(Collectors.toMap(
                                    e -> e.getKey().getName(),
                                    Map.Entry::getValue)),
                    resolvedBuckets.entrySet().stream()
                            .collect(Collectors.toMap(
                                    e -> e.getKey().getName(),
                                    e -> new Allocation(null, Collections.singletonList(new Range(e.getValue().getValue(), 1.0)), "#A1"))),
                    definedBucketSets.entrySet().stream()
                            .collect(Collectors.toMap(
                                    e -> e.getKey().getName(),
                                    e -> stubDefinitionWithVersion("v1", e.getValue())))
            );
        }
    }

    static ProctorResult buildSampleProctorResult() {

        return new ProctorResultStubBuilder()
                .withStubTest(StubTest.HOLDOUT_MASTER_TEST, GROUP_1_BUCKET,
                        INACTIVE_BUCKET, GROUP_1_BUCKET)
                .withStubTest(StubTest.CONTROL_SELECTED_TEST, CONTROL_BUCKET_WITH_PAYLOAD,
                        INACTIVE_BUCKET, CONTROL_BUCKET_WITH_PAYLOAD, GROUP_1_BUCKET_WITH_PAYLOAD)
                .withStubTest(StubTest.GROUP1_SELECTED_TEST, GROUP_1_BUCKET_WITH_PAYLOAD,
                        INACTIVE_BUCKET, CONTROL_BUCKET_WITH_PAYLOAD, GROUP_1_BUCKET_WITH_PAYLOAD)
                .withStubTest(StubTest.INACTIVE_SELECTED_TEST, INACTIVE_BUCKET,
                        INACTIVE_BUCKET, GROUP_1_BUCKET)
                // provides reference to FALLBACK_BUCKET that can be used in tests
                .withStubTest(StubTest.GROUP_WITH_FALLBACK_TEST, GROUP_1_BUCKET,
                        INACTIVE_BUCKET, GROUP_1_BUCKET, FALLBACK_TEST_BUCKET)
                // provides reference to FALLBACK_BUCKET that can be used in tests, no resolved test
                .withStubTest(StubTest.NO_BUCKETS_WITH_FALLBACK_TEST, null,
                        INACTIVE_BUCKET, GROUP_1_BUCKET, FALLBACK_TEST_BUCKET)
                .withStubTest(StubTest.MISSING_DEFINITION_TEST, GROUP_1_BUCKET)
                .build();
    }

    static ConsumableTestDefinition stubDefinitionWithVersion(final String version, final TestBucket... buckets) {
        final ConsumableTestDefinition testDefinition = new ConsumableTestDefinition();
        testDefinition.setVersion(version);
        testDefinition.setBuckets(Arrays.asList(buckets));
        return testDefinition;
    }

    /**
     * simulate generated subclass from json, would be Enum normally
     */
    private static Bucket<StubTest> createModelBucket(final StubTest test, final int value) {
        return new Bucket<StubTest>() {
            @Override
            public StubTest getTest() {
                return test;
            }

            @Override
            public int getValue() {
                return value;
            }

            @Override
            public String getName() {
                return test.getName();
            }

            @Override
            public String getFullName() {
                return test.getName();
            }
        };
    }

    /**
     * simulate generated enum from json
     */
    enum StubTest implements com.indeed.proctor.consumer.Test {
        HOLDOUT_MASTER_TEST("holdout_tst", -1),

        CONTROL_SELECTED_TEST("bgtst", -1),
        GROUP1_SELECTED_TEST("abtst", -1),
        GROUP_WITH_FALLBACK_TEST("groupwithfallbacktst", -1),
        INACTIVE_SELECTED_TEST("btntst", -1),

        // proctor-test to test situation where bucket and allocation is available but definition is not.
        // This is expected to be happen only in artificial case such as in testing.
        MISSING_DEFINITION_TEST("no_definition_tst", -1),

        NO_BUCKETS_WITH_FALLBACK_TEST("nobucketfallbacktst", -1);

        private final String name;
        private final int fallbackValue;

        StubTest(final String name, final int fallbackValue) {
            this.name = name;
            this.fallbackValue = fallbackValue;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getFallbackValue() {
            return fallbackValue;
        }
    }

    /**
     * simulate generated subclass from json, would be enum normally
     */
    static class FakeTest implements com.indeed.proctor.consumer.Test {

        private final String name;
        private final int fallbackValue;

        FakeTest(final String name, final int fallbackValue) {
            this.name = name;
            this.fallbackValue = fallbackValue;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getFallbackValue() {
            return fallbackValue;
        }
    }
}
