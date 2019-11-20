package com.indeed.proctor.consumer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestAbstractGroups {

    private static final String HOLDOUT_TESTNAME = "holdout_tst";
    private static final String CONTROL_TESTNAME = "bgtst";
    private static final String ACTIVE_TESTNAME = "abtst";
    private static final String GROUP_WITH_FALLBACK_TESTNAME = "groupwithfallbacktst";
    private static final String INACTIVE_TESTNAME = "btntst";

    private static final String NO_BUCKETS_WITH_FALLBACK_TESTNAME = "nobucketfallbacktst";
    private static final Bucket FALLBACK_BUCKET = createModelBucket(42);
    private static final Bucket FALLBACK_NOPAYLOAD_BUCKET = createModelBucket(66);

    static class TestGroups extends AbstractGroups {
        TestGroups(final ProctorResult proctorResult) {
            super(proctorResult);
        }
    }

    /**
     * This is one simple example of a holdout-groupsWithCustom implementation that uses a hardcoded hold-out experiment,
     * applies hold-out to all other experiments, uses the bucket with the smallest value in hold-out case,
     * and uses the fallback value for most error cases.
     *
     * Better implementations might use meta-tags or other properties to identify hold-out experiment, and
     * also to identify experiments subject to hold-out groupsWithCustom, and have better strategies for selecting
     * the hold-out bucket to use.
     */
    static class TestGroupsWithHoldout extends TestGroups {
        TestGroupsWithHoldout(final ProctorResult proctorResult) {
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
    static class TestGroupsWithForced extends TestGroups {
        TestGroupsWithForced(final ProctorResult proctorResult) {
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

    private TestGroups emptyGroup;
    private TestGroups groups;
    private TestGroupsWithForced groupsWithForced;
    private TestGroupsWithHoldout groupsWithHoldOut;

    @Before
    public void setUp() throws Exception {
        final TestBucket inactiveBucket = new TestBucket("inactive", -1, "inactive");
        final TestBucket controlBucketWithPayload = new TestBucket("control", 0, "control", createStringPayload("controlPayload"));
        final TestBucket activeBucketWithPayload = new TestBucket("active", 1, "active", createStringPayload("activePayload"));
        final TestBucket activeBucket = new TestBucket("active", 2, "active");
        final ProctorResult proctorResult = new ProctorResult(
                "0",
                // buckets
                ImmutableMap.of(
                        HOLDOUT_TESTNAME, activeBucket,
                        CONTROL_TESTNAME, controlBucketWithPayload,
                        ACTIVE_TESTNAME, activeBucketWithPayload,
                        GROUP_WITH_FALLBACK_TESTNAME, activeBucket,
                        INACTIVE_TESTNAME, inactiveBucket
                ),
                // allocations
                ImmutableMap.of(
                        HOLDOUT_TESTNAME, new Allocation(null, Arrays.asList(new Range(activeBucket.getValue(), 1.0)), "#A1"),
                        CONTROL_TESTNAME, new Allocation(null, Arrays.asList(new Range(controlBucketWithPayload.getValue(), 1.0)), "#A1"),
                        ACTIVE_TESTNAME, new Allocation(null, Arrays.asList(new Range(activeBucketWithPayload.getValue(), 1.0)), "#B2"),
                        GROUP_WITH_FALLBACK_TESTNAME, new Allocation(null, Arrays.asList(new Range(activeBucket.getValue(), 1.0)), "#B2"),
                        INACTIVE_TESTNAME, new Allocation(null, Arrays.asList(new Range(inactiveBucket.getValue(), 1.0)), "#C3")
                ),
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
                                        createStringPayload("fallback")),
                                inactiveBucket, activeBucket))
                        // has no buckets in result, but in definition
                        .put(NO_BUCKETS_WITH_FALLBACK_TESTNAME, stubDefinitionWithVersion(
                                "vNoBuckets",
                                new TestBucket(
                                        "fallbackBucket",
                                        FALLBACK_BUCKET.getValue(),
                                        "fallbackDesc",
                                        createStringPayload("fallback")),
                                inactiveBucket, activeBucket))
                        .build()
        );

        emptyGroup = new TestGroups(new ProctorResult("0", emptyMap(), emptyMap(), emptyMap()));
        groups = new TestGroups(proctorResult);
        groupsWithForced = new TestGroupsWithForced(proctorResult);
        groupsWithHoldOut = new TestGroupsWithHoldout(proctorResult);
    }

    private static Payload createStringPayload(final String value) {
        final Payload payload = new Payload();
        payload.setStringValue(value);
        return payload;
    }

    private static ConsumableTestDefinition stubDefinitionWithVersion(final String version, final TestBucket... buckets) {
        final ConsumableTestDefinition testDefinition = new ConsumableTestDefinition();
        testDefinition.setVersion(version);
        testDefinition.setBuckets(Arrays.asList(buckets));
        return testDefinition;
    }

    @Test
    public void testIsBucketActive() {
        assertFalse(groups.isBucketActive(CONTROL_TESTNAME, -1));
        assertTrue(groups.isBucketActive(CONTROL_TESTNAME, 0));
        assertFalse(groups.isBucketActive(CONTROL_TESTNAME, 1));

        assertFalse(groups.isBucketActive(ACTIVE_TESTNAME, -1));
        assertFalse(groups.isBucketActive(ACTIVE_TESTNAME, 0));
        assertTrue(groups.isBucketActive(ACTIVE_TESTNAME, 1));

        // forced control group
        assertFalse(groupsWithForced.isBucketActive(ACTIVE_TESTNAME, -1));
        assertTrue(groupsWithForced.isBucketActive(ACTIVE_TESTNAME, 0));
        assertFalse(groupsWithForced.isBucketActive(ACTIVE_TESTNAME, 1));

        assertFalse(groups.isBucketActive(CONTROL_TESTNAME, -1, 42));
        assertTrue(groups.isBucketActive(CONTROL_TESTNAME, 0, 42));

        assertFalse(groups.isBucketActive("notexist", -1));
        assertTrue(groups.isBucketActive("notexist", 1, 1));
        assertFalse(groups.isBucketActive("notexist", 1, 2));

        assertFalse(emptyGroup.isBucketActive("notexist", -1));
        assertTrue(emptyGroup.isBucketActive("notexist", 1, 1));
        assertFalse(emptyGroup.isBucketActive("notexist", 1, 2));

        assertTrue(groupsWithHoldOut.isBucketActive(CONTROL_TESTNAME, -1));
        assertFalse(groupsWithHoldOut.isBucketActive(CONTROL_TESTNAME, 0));
        assertFalse(groupsWithHoldOut.isBucketActive(CONTROL_TESTNAME, 1));

        assertTrue(groupsWithHoldOut.isBucketActive(ACTIVE_TESTNAME, -1));
        assertFalse(groupsWithHoldOut.isBucketActive(ACTIVE_TESTNAME, 0));
        assertFalse(groupsWithHoldOut.isBucketActive(ACTIVE_TESTNAME, 1));

        assertTrue(groupsWithHoldOut.isBucketActive(CONTROL_TESTNAME, -1, 42));
        assertFalse(groupsWithHoldOut.isBucketActive(CONTROL_TESTNAME, 0, 42));

        assertFalse(groupsWithHoldOut.isBucketActive("notexist", -1));
        assertTrue(groupsWithHoldOut.isBucketActive("notexist", 1, 1)); // using default
        assertFalse(groupsWithHoldOut.isBucketActive("notexist", 1, 2));
    }

    @Test
    public void testGetValue() {
        assertThat(groups.getValue(CONTROL_TESTNAME, 42)).isEqualTo(0);
        assertThat(groups.getValue(ACTIVE_TESTNAME, 42)).isEqualTo(1);
        assertThat(groups.getValue("notexist", 42)).isEqualTo(42);

        assertThat(groupsWithForced.getValue(CONTROL_TESTNAME, 42)).isEqualTo(0);
        assertThat(groupsWithForced.getValue(ACTIVE_TESTNAME, 42)).isEqualTo(0); // forced
        assertThat(groupsWithForced.getValue("notexist", 42)).isEqualTo(42);

        assertThat(groupsWithHoldOut.getValue(CONTROL_TESTNAME, 42)).isEqualTo(-1);
        assertThat(groupsWithHoldOut.getValue(ACTIVE_TESTNAME, 42)).isEqualTo(-1);
        assertThat(groupsWithHoldOut.getValue("notexist", 42)).isEqualTo(42); // no fallback bucket

        assertThat(emptyGroup.getValue("notexist", 42)).isEqualTo(42); // no fallback bucket
    }

    @Test
    public void testGetTestVersions() {
        assertThat(groups.getTestVersions()).isEqualTo(ImmutableMap.builder()
                .put(CONTROL_TESTNAME, "vControl")
                .put(ACTIVE_TESTNAME, "vActive")
                .put(INACTIVE_TESTNAME, "vInactive")
                .put(GROUP_WITH_FALLBACK_TESTNAME, "vGroupWithFallback")
                .put(NO_BUCKETS_WITH_FALLBACK_TESTNAME, "vNoBuckets")
                .put(HOLDOUT_TESTNAME, "vInactive")
                .build());

        assertThat(groups.getTestVersions(Collections.emptySet())).isEqualTo(emptyMap());

        assertThat(groups.getTestVersions(ImmutableSet.of(CONTROL_TESTNAME, "notexist"))).isEqualTo(ImmutableMap.builder()
                .put(CONTROL_TESTNAME, "vControl")
                .build());
    }

    @Test
    public void testGetPayload() {
        assertThat(groups.getPayload(INACTIVE_TESTNAME)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groups.getPayload(ACTIVE_TESTNAME)).isEqualTo(createStringPayload("activePayload"));
        assertThat(groups.getPayload(CONTROL_TESTNAME)).isEqualTo(createStringPayload("controlPayload"));
        assertThat(groups.getPayload(ACTIVE_TESTNAME, FALLBACK_BUCKET)).isEqualTo(createStringPayload("activePayload"));
        assertThat(groups.getPayload(CONTROL_TESTNAME, FALLBACK_BUCKET)).isEqualTo(createStringPayload("controlPayload"));
        assertThat(groups.getPayload(GROUP_WITH_FALLBACK_TESTNAME, FALLBACK_BUCKET)).isEqualTo(createStringPayload("fallback"));
        assertThat(groups.getPayload(NO_BUCKETS_WITH_FALLBACK_TESTNAME, FALLBACK_BUCKET)).isEqualTo(createStringPayload("fallback"));
        assertThat(groups.getPayload(NO_BUCKETS_WITH_FALLBACK_TESTNAME, FALLBACK_NOPAYLOAD_BUCKET)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groups.getPayload("notexist")).isEqualTo(Payload.EMPTY_PAYLOAD);

        assertThat(groupsWithForced.getPayload(INACTIVE_TESTNAME)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groupsWithForced.getPayload(ACTIVE_TESTNAME)).isEqualTo(createStringPayload("controlPayload")); // forced
        assertThat(groupsWithForced.getPayload(CONTROL_TESTNAME)).isEqualTo(createStringPayload("controlPayload"));

        assertThat(groupsWithHoldOut.getPayload(INACTIVE_TESTNAME)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groupsWithHoldOut.getPayload(ACTIVE_TESTNAME)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groupsWithHoldOut.getPayload(CONTROL_TESTNAME)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groupsWithHoldOut.getPayload(ACTIVE_TESTNAME, FALLBACK_BUCKET)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groupsWithHoldOut.getPayload(CONTROL_TESTNAME, FALLBACK_BUCKET)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groupsWithHoldOut.getPayload(GROUP_WITH_FALLBACK_TESTNAME, FALLBACK_BUCKET)).isEqualTo(createStringPayload("fallback"));
        assertThat(groupsWithHoldOut.getPayload(NO_BUCKETS_WITH_FALLBACK_TESTNAME, FALLBACK_BUCKET)).isEqualTo(createStringPayload("fallback"));
        assertThat(groupsWithHoldOut.getPayload(NO_BUCKETS_WITH_FALLBACK_TESTNAME, FALLBACK_NOPAYLOAD_BUCKET)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groupsWithHoldOut.getPayload("notexist")).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(emptyGroup.getPayload("notexist")).isEqualTo(Payload.EMPTY_PAYLOAD);
    }

    @Test
    public void testisEmpty() {
        assertThat(emptyGroup.isEmpty()).isTrue();
        assertThat(groups.isEmpty()).isFalse();
        assertThat(groupsWithHoldOut.isEmpty()).isFalse();
    }

    @Test
    public void testToLongString() {
        assertThat(emptyGroup.toLongString()).isEmpty();
        assertThat(groups.toLongString()).isEqualTo("abtst-active,bgtst-control,btntst-inactive,groupwithfallbacktst-active,holdout_tst-active");
        assertThat(groupsWithForced.toLongString()).isEqualTo("abtst-control,bgtst-control,btntst-inactive,groupwithfallbacktst-active,holdout_tst-active");
        assertThat(groupsWithHoldOut.toLongString()).isEqualTo("abtst-inactive,bgtst-inactive,btntst-inactive,groupwithfallbacktst-inactive,holdout_tst-active");
    }

    @Test
    public void testToLoggingString() {
        assertThat(new TestGroups(new ProctorResult("0", emptyMap(), emptyMap(), emptyMap())).toLoggingString()).isEmpty();
        assertThat(groups.toLoggingString()).isEqualTo("abtst1,bgtst0,groupwithfallbacktst2,holdout_tst2,#B2:abtst1,#A1:bgtst0,#B2:groupwithfallbacktst2,#A1:holdout_tst2");
        assertThat(groupsWithForced.toLoggingString()).isEqualTo("abtst0,bgtst0,groupwithfallbacktst2,holdout_tst2,#B2:abtst0,#A1:bgtst0,#B2:groupwithfallbacktst2,#A1:holdout_tst2");
        assertThat(groupsWithHoldOut.toLoggingString()).isEqualTo("holdout_tst2,#A1:holdout_tst2");
    }

    @Test
    public void testGetLoggingTestNames() {
        assertThat(Sets.newHashSet(groups.getLoggingTestNames()))
                .containsExactlyInAnyOrder(CONTROL_TESTNAME, ACTIVE_TESTNAME, GROUP_WITH_FALLBACK_TESTNAME, HOLDOUT_TESTNAME);
    }

    @Test
    public void testAppendTestGroupsWithoutAllocations() {
        StringBuilder builder = new StringBuilder();
        groups.appendTestGroupsWithoutAllocations(builder, ',', Lists.newArrayList(CONTROL_TESTNAME, ACTIVE_TESTNAME));
        assertThat(builder.toString().split(",")).containsExactly("bgtst0", "abtst1");
        builder = new StringBuilder();
        groupsWithForced.appendTestGroupsWithoutAllocations(builder, ',', Lists.newArrayList(CONTROL_TESTNAME, ACTIVE_TESTNAME));
        assertThat(builder.toString().split(",")).containsExactly("bgtst0", "abtst0");
        builder = new StringBuilder();
        groupsWithHoldOut.appendTestGroupsWithoutAllocations(builder, ',', Lists.newArrayList(CONTROL_TESTNAME, ACTIVE_TESTNAME));
        assertThat(builder.toString().split(",")).containsExactly("bgtst-1", "abtst-1");
        emptyGroup.appendTestGroupsWithoutAllocations(builder, ',', Lists.newArrayList(CONTROL_TESTNAME, ACTIVE_TESTNAME));
        assertThat(builder.toString().split(",")).containsExactly("bgtst-1", "abtst-1");
    }

    @Test
    public void testAppendTestGroupsWithAllocations() {
        StringBuilder builder = new StringBuilder();
        groups.appendTestGroupsWithAllocations(builder, ',', Lists.newArrayList(CONTROL_TESTNAME, ACTIVE_TESTNAME));
        assertThat(builder.toString().split(",")).containsExactly("#A1:bgtst0", "#B2:abtst1");
        builder = new StringBuilder();
        groupsWithForced.appendTestGroupsWithAllocations(builder, ',', Lists.newArrayList(CONTROL_TESTNAME, ACTIVE_TESTNAME));
        assertThat(builder.toString().split(",")).containsExactly("#A1:bgtst0", "#B2:abtst0");
        builder = new StringBuilder();
        groupsWithHoldOut.appendTestGroupsWithAllocations(builder, ',', Lists.newArrayList(CONTROL_TESTNAME, ACTIVE_TESTNAME));
        assertThat(builder.toString().split(",")).containsExactly("#A1:bgtst-1", "#B2:abtst-1");
        builder = new StringBuilder();
        emptyGroup.appendTestGroupsWithAllocations(builder, ',', Lists.newArrayList(CONTROL_TESTNAME, ACTIVE_TESTNAME));
        assertThat(builder.toString().split(",")).containsExactly("");
    }

    @Test
    public void testAppendTestGroups() {
        StringBuilder builder = new StringBuilder();
        groups.appendTestGroups(builder, ',');
        assertThat(builder.toString().split(","))
                .containsExactlyInAnyOrder(
                        "groupwithfallbacktst2", "bgtst0", "abtst1", "holdout_tst2",
                        "#A1:bgtst0", "#B2:abtst1", "#B2:groupwithfallbacktst2", "#A1:holdout_tst2");
        builder = new StringBuilder();
        groupsWithForced.appendTestGroups(builder, ',');
        assertThat(builder.toString().split(","))
                .containsExactlyInAnyOrder(
                        "groupwithfallbacktst2", "bgtst0", "abtst0", "holdout_tst2",
                        "#A1:bgtst0", "#B2:abtst0", "#B2:groupwithfallbacktst2", "#A1:holdout_tst2");
    }

    @Test
    public void testGetJavaScriptConfig() {

        assertThat(emptyGroup.getJavaScriptConfig())
                .hasSize(0);

        assertThat(groups.getJavaScriptConfig())
                .hasSize(4)
                .containsEntry(ACTIVE_TESTNAME, 1)
                .containsEntry(CONTROL_TESTNAME, 0)
                .containsEntry(GROUP_WITH_FALLBACK_TESTNAME, 2)
                .containsEntry(HOLDOUT_TESTNAME, 2);

        assertThat(groupsWithForced.getJavaScriptConfig())
                .hasSize(4)
                .containsEntry(ACTIVE_TESTNAME, 0) // forced
                .containsEntry(CONTROL_TESTNAME, 0)
                .containsEntry(GROUP_WITH_FALLBACK_TESTNAME, 2)
                .containsEntry(HOLDOUT_TESTNAME, 2);

        assertThat(groupsWithHoldOut.getJavaScriptConfig())
                .hasSize(1)
                .containsEntry(HOLDOUT_TESTNAME, 2);
    }

    @Test
    public void testGetJavaScriptConfigLists() {
        assertThat(groups.getJavaScriptConfig(new StubTest[] {
                new StubTest("notexist", 42),
                new StubTest(CONTROL_TESTNAME, 43),
                new StubTest(ACTIVE_TESTNAME, 44)}))
                .containsExactly(
                        Arrays.asList(42, null),
                        Arrays.asList(0, "controlPayload"),
                        Arrays.asList(1, "activePayload")
                );
        assertThat(groupsWithForced.getJavaScriptConfig(new StubTest[] {
                new StubTest("notexist", 42),
                new StubTest(CONTROL_TESTNAME, 43),
                new StubTest(ACTIVE_TESTNAME, 44)}))
                .containsExactly(
                        Arrays.asList(42, null),
                        Arrays.asList(0, "controlPayload"),
                        Arrays.asList(0, "controlPayload") // forced
                );
        assertThat(groupsWithHoldOut.getJavaScriptConfig(new StubTest[] {
                new StubTest("notexist", 42),
                new StubTest(CONTROL_TESTNAME, 43),
                new StubTest(ACTIVE_TESTNAME, 44)}))
                .containsExactly(
                        Arrays.asList(42, null), // no fallback
                        Arrays.asList(-1, null),
                        Arrays.asList(-1, null)
                );
    }

    private static class StubTest implements com.indeed.proctor.consumer.Test {

        private final String name;
        private final int value;

        private StubTest(final String name, final int value) {
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
}
