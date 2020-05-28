package com.indeed.proctor.consumer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.TestBucket;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

import static com.indeed.proctor.consumer.ProctorGroupStubber.FALLBACK_BUCKET;
import static com.indeed.proctor.consumer.ProctorGroupStubber.FALLBACK_NOPAYLOAD_BUCKET;
import static com.indeed.proctor.consumer.ProctorGroupStubber.StubTest.CONTROL_SELECTED_TEST;
import static com.indeed.proctor.consumer.ProctorGroupStubber.StubTest.GROUP1_SELECTED_TEST;
import static com.indeed.proctor.consumer.ProctorGroupStubber.StubTest.GROUP_WITH_FALLBACK_TEST;
import static com.indeed.proctor.consumer.ProctorGroupStubber.StubTest.HOLDOUT_MASTER_TEST;
import static com.indeed.proctor.consumer.ProctorGroupStubber.StubTest.INACTIVE_SELECTED_TEST;
import static com.indeed.proctor.consumer.ProctorGroupStubber.StubTest.MISSING_DEFINITION_TEST;
import static com.indeed.proctor.consumer.ProctorGroupStubber.StubTest.NO_BUCKETS_WITH_FALLBACK_TEST;
import static com.indeed.proctor.consumer.ProctorGroupStubber.buildSampleProctorResult;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestAbstractGroupsWithHoldout {

    /**
     * This is one simple example of a holdout-groupsWithCustom implementation that uses a hardcoded hold-out experiment,
     * applies hold-out to all other experiments, uses the bucket with the smallest value in hold-out case,
     * and uses the fallback value for most error cases.
     * <p>
     * Better implementations might use meta-tags or other properties to identify hold-out experiment, and
     * also to identify experiments subject to hold-out groupsWithCustom, and have better strategies for selecting
     * the hold-out bucket to use.
     */
    static class ProctorGroupsWithHoldout extends AbstractGroups {
        final ProctorGroupStubber.StubTest holdoutMaster;

        ProctorGroupsWithHoldout(final ProctorResult proctorResult, final ProctorGroupStubber.StubTest holdoutMaster) {
            super(proctorResult);
            this.holdoutMaster = holdoutMaster;
        }

        @Override
        protected int overrideDeterminedBucketValue(final String testName, @Nonnull final TestBucket determinedBucket) {
            // for other experiments, if hold-out experiment is active, use bucket with value -1 if available.
            if (!holdoutMaster.getName().equals(testName) && isBucketActive(holdoutMaster.getName(), 2, -1)) {
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

    private ProctorResult proctorResult;
    private AbstractGroups groupsWithHoldOut;

    @Before
    public void setUp() {

        proctorResult = buildSampleProctorResult();

        // using ProctorGroupsWithHoldout to make all tests except HOLDOUT_MASTER_TEST become inactive
        groupsWithHoldOut = new ProctorGroupsWithHoldout(proctorResult, HOLDOUT_MASTER_TEST);
    }

    @Test
    public void testIsBucketActive() {
        assertTrue(groupsWithHoldOut.isBucketActive(CONTROL_SELECTED_TEST.getName(), -1)); // because of holdout
        assertFalse(groupsWithHoldOut.isBucketActive(CONTROL_SELECTED_TEST.getName(), 0));
        assertFalse(groupsWithHoldOut.isBucketActive(CONTROL_SELECTED_TEST.getName(), 1));

        assertTrue(groupsWithHoldOut.isBucketActive(GROUP1_SELECTED_TEST.getName(), -1)); // because of holdout
        assertFalse(groupsWithHoldOut.isBucketActive(GROUP1_SELECTED_TEST.getName(), 0));
        assertFalse(groupsWithHoldOut.isBucketActive(GROUP1_SELECTED_TEST.getName(), 1));

        assertTrue(groupsWithHoldOut.isBucketActive(CONTROL_SELECTED_TEST.getName(), -1, 42)); // because of holdout
        assertFalse(groupsWithHoldOut.isBucketActive(CONTROL_SELECTED_TEST.getName(), 0, 42));

        assertFalse(groupsWithHoldOut.isBucketActive("notexist", -1));
        assertTrue(groupsWithHoldOut.isBucketActive("notexist", 1, 1)); // using default
        assertFalse(groupsWithHoldOut.isBucketActive("notexist", 1, 2));
    }

    @Test
    public void testGetValue() {
        assertThat(groupsWithHoldOut.getValue(CONTROL_SELECTED_TEST.getName(), 42)).isEqualTo(-1);
        assertThat(groupsWithHoldOut.getValue(GROUP1_SELECTED_TEST.getName(), 42)).isEqualTo(-1);
        assertThat(groupsWithHoldOut.getValue("notexist", 42)).isEqualTo(42); // no fallback bucket

    }

    @Test
    public void testGetPayload() {
        assertThat(groupsWithHoldOut.getPayload(INACTIVE_SELECTED_TEST.getName())).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groupsWithHoldOut.getPayload(GROUP1_SELECTED_TEST.getName())).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groupsWithHoldOut.getPayload(CONTROL_SELECTED_TEST.getName())).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groupsWithHoldOut.getPayload(GROUP1_SELECTED_TEST.getName(), FALLBACK_BUCKET)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groupsWithHoldOut.getPayload(CONTROL_SELECTED_TEST.getName(), FALLBACK_BUCKET)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groupsWithHoldOut.getPayload(GROUP_WITH_FALLBACK_TEST.getName(), FALLBACK_BUCKET)).isEqualTo(new Payload("fallback"));
        assertThat(groupsWithHoldOut.getPayload(NO_BUCKETS_WITH_FALLBACK_TEST.getName(), FALLBACK_BUCKET)).isEqualTo(new Payload("fallback"));
        assertThat(groupsWithHoldOut.getPayload(NO_BUCKETS_WITH_FALLBACK_TEST.getName(), FALLBACK_NOPAYLOAD_BUCKET)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groupsWithHoldOut.getPayload("notexist")).isEqualTo(Payload.EMPTY_PAYLOAD);
    }

    @Test
    public void testIsEmpty() {
        assertThat(groupsWithHoldOut.isEmpty()).isFalse();
    }

    @Test
    public void testToLongString() {
        assertThat(groupsWithHoldOut.toLongString()).isEqualTo("abtst-inactive,bgtst-inactive,btntst-inactive,groupwithfallbacktst-inactive,holdout_tst-group1,no_definition_tst-group1");
    }

    @Test
    public void testToLoggingString() {
        assertThat((new AbstractGroups(new ProctorResult("0", emptyMap(), emptyMap(), emptyMap())) {}).toLoggingString()).isEmpty();
        assertThat(groupsWithHoldOut.toLoggingString()).isEqualTo("holdout_tst2,no_definition_tst2,#A1:holdout_tst2,#A1:no_definition_tst2");
    }

    @Test
    public void testGetLoggingTestNames() {
        assertThat(Sets.newHashSet(groupsWithHoldOut.getLoggingTestNames()))
                .containsExactlyInAnyOrder(HOLDOUT_MASTER_TEST.getName(), MISSING_DEFINITION_TEST.getName());
    }

    @Test
    public void testAppendTestGroupsWithoutAllocations() {
        StringBuilder builder = new StringBuilder();
        groupsWithHoldOut.appendTestGroupsWithoutAllocations(builder, ',', Lists.newArrayList(CONTROL_SELECTED_TEST.getName(), GROUP1_SELECTED_TEST.getName()));
        assertThat(builder.toString().split(",")).containsExactly("bgtst-1", "abtst-1");
    }

    @Test
    public void testAppendTestGroupsWithAllocations() {
        StringBuilder builder = new StringBuilder();
        groupsWithHoldOut.appendTestGroupsWithAllocations(builder, ',', Lists.newArrayList(CONTROL_SELECTED_TEST.getName(), GROUP1_SELECTED_TEST.getName()));
        assertThat(builder.toString().split(",")).containsExactly("#A1:bgtst-1", "#A1:abtst-1");
    }

    @Test
    public void testGetJavaScriptConfig() {

        assertThat(groupsWithHoldOut.getJavaScriptConfig())
                .hasSize(2)
                .containsEntry(HOLDOUT_MASTER_TEST.getName(), 2)
                .containsEntry(MISSING_DEFINITION_TEST.getName(), 2) // continue to return due to absent definition
                ;
    }

    @Test
    public void testGetJavaScriptConfigLists() {
        assertThat(groupsWithHoldOut.getJavaScriptConfig(new FakeTest[] {
                new FakeTest("notexist", 42),
                new FakeTest(CONTROL_SELECTED_TEST.getName(), 43),
                new FakeTest(GROUP1_SELECTED_TEST.getName(), 44)}))
                .containsExactly(
                        Arrays.asList(42, null), // no fallback
                        Arrays.asList(-1, null),
                        Arrays.asList(-1, null)
                );
    }

    @Test
    public void testProctorResults() {
        // different data for bucket, else same, but not same instance
        assertThat(groupsWithHoldOut.getAsProctorResult()).isNotSameAs(proctorResult);
        assertThat(groupsWithHoldOut.getAsProctorResult().getMatrixVersion()).isEqualTo(proctorResult.getMatrixVersion());
        assertThat(groupsWithHoldOut.getAsProctorResult().getBuckets()).isNotEqualTo(proctorResult.getBuckets()); // not equal
        assertThat(groupsWithHoldOut.getAsProctorResult().getAllocations()).isEqualTo(proctorResult.getAllocations());
        assertThat(groupsWithHoldOut.getAsProctorResult().getTestDefinitions()).isEqualTo(proctorResult.getTestDefinitions());
    }


    private static class FakeTest implements com.indeed.proctor.consumer.Test {

        private final String name;
        private final int value;

        private FakeTest(final String name, final int value) {
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
