package com.indeed.proctor.consumer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.consumer.ProctorGroupStubber.FakeTest;
import com.indeed.proctor.consumer.ProctorGroupStubber.ProctorGroupsWithHoldout;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static com.indeed.proctor.consumer.ProctorGroupStubber.FALLBACK_BUCKET;
import static com.indeed.proctor.consumer.ProctorGroupStubber.FALLBACK_NOPAYLOAD_BUCKET;
import static com.indeed.proctor.consumer.ProctorGroupStubber.ProctorGroupsWithForced;
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

public class TestAbstractGroups {

    private ProctorResult proctorResult;
    private AbstractGroups emptyGroup;
    private AbstractGroups sampleGroups;
    private AbstractGroups sampleGroupsWithForced;
    private AbstractGroups groupsWithHoldOut;

    @Before
    public void setUp() {
        emptyGroup = new AbstractGroups(new ProctorResult("0", emptyMap(), emptyMap(), emptyMap())) {};

        proctorResult = buildSampleProctorResult();
        sampleGroups = new AbstractGroups(proctorResult) {};

        // Using ProctorGroupsWithForced to make GROUP1_SELECTED_TEST select control instead of group1
        sampleGroupsWithForced = new ProctorGroupsWithForced(proctorResult, GROUP1_SELECTED_TEST);

        // using ProctorGroupsWithHoldout to make all tests except HOLDOUT_MASTER_TEST become inactive
        groupsWithHoldOut = new ProctorGroupsWithHoldout(proctorResult, HOLDOUT_MASTER_TEST);
    }

    @Test
    public void testIsBucketActive() {
        assertFalse(sampleGroups.isBucketActive(CONTROL_SELECTED_TEST.getName(), -1));
        assertTrue(sampleGroups.isBucketActive(CONTROL_SELECTED_TEST.getName(), 0));
        assertFalse(sampleGroups.isBucketActive(CONTROL_SELECTED_TEST.getName(), 1));

        assertFalse(sampleGroups.isBucketActive(GROUP1_SELECTED_TEST.getName(), -1));
        assertFalse(sampleGroups.isBucketActive(GROUP1_SELECTED_TEST.getName(), 0));
        assertTrue(sampleGroups.isBucketActive(GROUP1_SELECTED_TEST.getName(), 1));

        // forced control group
        assertFalse(sampleGroupsWithForced.isBucketActive(GROUP1_SELECTED_TEST.getName(), -1));
        assertTrue(sampleGroupsWithForced.isBucketActive(GROUP1_SELECTED_TEST.getName(), 0));
        assertFalse(sampleGroupsWithForced.isBucketActive(GROUP1_SELECTED_TEST.getName(), 1));

        assertFalse(sampleGroups.isBucketActive(CONTROL_SELECTED_TEST.getName(), -1, 42));
        assertTrue(sampleGroups.isBucketActive(CONTROL_SELECTED_TEST.getName(), 0, 42));

        assertFalse(sampleGroups.isBucketActive("notexist", -1));
        assertTrue(sampleGroups.isBucketActive("notexist", 1, 1));
        assertFalse(sampleGroups.isBucketActive("notexist", 1, 2));

        assertFalse(emptyGroup.isBucketActive("notexist", -1));
        assertTrue(emptyGroup.isBucketActive("notexist", 1, 1));
        assertFalse(emptyGroup.isBucketActive("notexist", 1, 2));

        assertTrue(groupsWithHoldOut.isBucketActive(CONTROL_SELECTED_TEST.getName(), -1));
        assertFalse(groupsWithHoldOut.isBucketActive(CONTROL_SELECTED_TEST.getName(), 0));
        assertFalse(groupsWithHoldOut.isBucketActive(CONTROL_SELECTED_TEST.getName(), 1));

        assertTrue(groupsWithHoldOut.isBucketActive(GROUP1_SELECTED_TEST.getName(), -1));
        assertFalse(groupsWithHoldOut.isBucketActive(GROUP1_SELECTED_TEST.getName(), 0));
        assertFalse(groupsWithHoldOut.isBucketActive(GROUP1_SELECTED_TEST.getName(), 1));

        assertTrue(groupsWithHoldOut.isBucketActive(CONTROL_SELECTED_TEST.getName(), -1, 42));
        assertFalse(groupsWithHoldOut.isBucketActive(CONTROL_SELECTED_TEST.getName(), 0, 42));

        assertFalse(groupsWithHoldOut.isBucketActive("notexist", -1));
        assertTrue(groupsWithHoldOut.isBucketActive("notexist", 1, 1)); // using default
        assertFalse(groupsWithHoldOut.isBucketActive("notexist", 1, 2));
    }

    @Test
    public void testGetValue() {
        assertThat(sampleGroups.getValue(CONTROL_SELECTED_TEST.getName(), 42)).isEqualTo(0);
        assertThat(sampleGroups.getValue(GROUP1_SELECTED_TEST.getName(), 42)).isEqualTo(1);
        assertThat(sampleGroups.getValue("notexist", 42)).isEqualTo(42);

        assertThat(sampleGroupsWithForced.getValue(CONTROL_SELECTED_TEST.getName(), 42)).isEqualTo(0);
        assertThat(sampleGroupsWithForced.getValue(GROUP1_SELECTED_TEST.getName(), 42)).isEqualTo(0); // forced
        assertThat(sampleGroupsWithForced.getValue("notexist", 42)).isEqualTo(42);

        assertThat(groupsWithHoldOut.getValue(CONTROL_SELECTED_TEST.getName(), 42)).isEqualTo(-1);
        assertThat(groupsWithHoldOut.getValue(GROUP1_SELECTED_TEST.getName(), 42)).isEqualTo(-1);
        assertThat(groupsWithHoldOut.getValue("notexist", 42)).isEqualTo(42); // no fallback bucket

        assertThat(emptyGroup.getValue("notexist", 42)).isEqualTo(42); // no fallback bucket
    }

    @Test
    public void testGetPayload() {
        assertThat(sampleGroups.getPayload(INACTIVE_SELECTED_TEST.getName())).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(sampleGroups.getPayload(GROUP1_SELECTED_TEST.getName())).isEqualTo(new Payload("activePayload"));
        assertThat(sampleGroups.getPayload(CONTROL_SELECTED_TEST.getName())).isEqualTo(new Payload("controlPayload"));

        assertThat(sampleGroups.getPayload(GROUP1_SELECTED_TEST.getName(), FALLBACK_BUCKET)).isEqualTo(new Payload("activePayload"));
        assertThat(sampleGroups.getPayload(CONTROL_SELECTED_TEST.getName(), FALLBACK_BUCKET)).isEqualTo(new Payload("controlPayload"));
        assertThat(sampleGroups.getPayload(INACTIVE_SELECTED_TEST.getName(), FALLBACK_BUCKET)).isEqualTo(Payload.EMPTY_PAYLOAD);

        assertThat(sampleGroups.getPayload(GROUP_WITH_FALLBACK_TEST.getName(), FALLBACK_BUCKET)).isEqualTo(new Payload("fallback"));
        assertThat(sampleGroups.getPayload(NO_BUCKETS_WITH_FALLBACK_TEST.getName(), FALLBACK_BUCKET)).isEqualTo(new Payload("fallback"));
        assertThat(sampleGroups.getPayload(NO_BUCKETS_WITH_FALLBACK_TEST.getName(), FALLBACK_NOPAYLOAD_BUCKET)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(sampleGroups.getPayload("notexist")).isEqualTo(Payload.EMPTY_PAYLOAD);

        assertThat(sampleGroupsWithForced.getPayload(INACTIVE_SELECTED_TEST.getName())).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(sampleGroupsWithForced.getPayload(GROUP1_SELECTED_TEST.getName())).isEqualTo(new Payload("controlPayload")); // forced
        assertThat(sampleGroupsWithForced.getPayload(CONTROL_SELECTED_TEST.getName())).isEqualTo(new Payload("controlPayload"));

        assertThat(groupsWithHoldOut.getPayload(INACTIVE_SELECTED_TEST.getName())).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groupsWithHoldOut.getPayload(GROUP1_SELECTED_TEST.getName())).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groupsWithHoldOut.getPayload(CONTROL_SELECTED_TEST.getName())).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groupsWithHoldOut.getPayload(GROUP1_SELECTED_TEST.getName(), FALLBACK_BUCKET)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groupsWithHoldOut.getPayload(CONTROL_SELECTED_TEST.getName(), FALLBACK_BUCKET)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groupsWithHoldOut.getPayload(GROUP_WITH_FALLBACK_TEST.getName(), FALLBACK_BUCKET)).isEqualTo(new Payload("fallback"));
        assertThat(groupsWithHoldOut.getPayload(NO_BUCKETS_WITH_FALLBACK_TEST.getName(), FALLBACK_BUCKET)).isEqualTo(new Payload("fallback"));
        assertThat(groupsWithHoldOut.getPayload(NO_BUCKETS_WITH_FALLBACK_TEST.getName(), FALLBACK_NOPAYLOAD_BUCKET)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groupsWithHoldOut.getPayload("notexist")).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(emptyGroup.getPayload("notexist")).isEqualTo(Payload.EMPTY_PAYLOAD);
    }

    @Test
    public void testIsEmpty() {
        assertThat(emptyGroup.isEmpty()).isTrue();
        assertThat(sampleGroups.isEmpty()).isFalse();
        assertThat(groupsWithHoldOut.isEmpty()).isFalse();
    }

    @Test
    public void testToLongString() {
        assertThat(emptyGroup.toLongString()).isEmpty();
        assertThat(sampleGroups.toLongString()).isEqualTo("abtst-group1,bgtst-control,btntst-inactive,groupwithfallbacktst-group1,holdout_tst-group1,no_definition_tst-group1");
        assertThat(sampleGroupsWithForced.toLongString()).isEqualTo("abtst-control,bgtst-control,btntst-inactive,groupwithfallbacktst-group1,holdout_tst-group1,no_definition_tst-group1");
        assertThat(groupsWithHoldOut.toLongString()).isEqualTo("abtst-inactive,bgtst-inactive,btntst-inactive,groupwithfallbacktst-inactive,holdout_tst-group1,no_definition_tst-group1");
    }

    @Test
    public void testToLoggingString() {
        assertThat((new AbstractGroups(new ProctorResult("0", emptyMap(), emptyMap(), emptyMap())) {}).toLoggingString()).isEmpty();
        assertThat(sampleGroups.toLoggingString()).isEqualTo("abtst1,bgtst0,groupwithfallbacktst2,holdout_tst2,no_definition_tst2,#A1:abtst1,#A1:bgtst0,#A1:groupwithfallbacktst2,#A1:holdout_tst2,#A1:no_definition_tst2");
        assertThat(sampleGroupsWithForced.toLoggingString()).isEqualTo("abtst0,bgtst0,groupwithfallbacktst2,holdout_tst2,no_definition_tst2,#A1:abtst0,#A1:bgtst0,#A1:groupwithfallbacktst2,#A1:holdout_tst2,#A1:no_definition_tst2");
        assertThat(groupsWithHoldOut.toLoggingString()).isEqualTo("holdout_tst2,no_definition_tst2,#A1:holdout_tst2,#A1:no_definition_tst2");
    }

    @Test
    public void testGetLoggingTestNames() {
        assertThat(Sets.newHashSet(sampleGroups.getLoggingTestNames()))
                .containsExactlyInAnyOrder(CONTROL_SELECTED_TEST.getName(), GROUP1_SELECTED_TEST.getName(), GROUP_WITH_FALLBACK_TEST.getName(), HOLDOUT_MASTER_TEST.getName(), MISSING_DEFINITION_TEST.getName());
    }

    @Test
    public void testAppendTestGroupsWithoutAllocations() {
        StringBuilder builder = new StringBuilder();
        sampleGroups.appendTestGroupsWithoutAllocations(builder, ',', Lists.newArrayList(CONTROL_SELECTED_TEST.getName(), GROUP1_SELECTED_TEST.getName()));
        assertThat(builder.toString().split(",")).containsExactly("bgtst0", "abtst1");
        builder = new StringBuilder();
        sampleGroupsWithForced.appendTestGroupsWithoutAllocations(builder, ',', Lists.newArrayList(CONTROL_SELECTED_TEST.getName(), GROUP1_SELECTED_TEST.getName()));
        assertThat(builder.toString().split(",")).containsExactly("bgtst0", "abtst0");
        builder = new StringBuilder();
        groupsWithHoldOut.appendTestGroupsWithoutAllocations(builder, ',', Lists.newArrayList(CONTROL_SELECTED_TEST.getName(), GROUP1_SELECTED_TEST.getName()));
        assertThat(builder.toString().split(",")).containsExactly("bgtst-1", "abtst-1");
        emptyGroup.appendTestGroupsWithoutAllocations(builder, ',', Lists.newArrayList(CONTROL_SELECTED_TEST.getName(), GROUP1_SELECTED_TEST.getName()));
        assertThat(builder.toString().split(",")).containsExactly("bgtst-1", "abtst-1");
    }

    @Test
    public void testAppendTestGroupsWithAllocations() {
        StringBuilder builder = new StringBuilder();
        sampleGroups.appendTestGroupsWithAllocations(builder, ',', Lists.newArrayList(CONTROL_SELECTED_TEST.getName(), GROUP1_SELECTED_TEST.getName()));
        assertThat(builder.toString().split(",")).containsExactly("#A1:bgtst0", "#A1:abtst1");
        builder = new StringBuilder();
        sampleGroupsWithForced.appendTestGroupsWithAllocations(builder, ',', Lists.newArrayList(CONTROL_SELECTED_TEST.getName(), GROUP1_SELECTED_TEST.getName()));
        assertThat(builder.toString().split(",")).containsExactly("#A1:bgtst0", "#A1:abtst0");
        builder = new StringBuilder();
        groupsWithHoldOut.appendTestGroupsWithAllocations(builder, ',', Lists.newArrayList(CONTROL_SELECTED_TEST.getName(), GROUP1_SELECTED_TEST.getName()));
        assertThat(builder.toString().split(",")).containsExactly("#A1:bgtst-1", "#A1:abtst-1");
        builder = new StringBuilder();
        emptyGroup.appendTestGroupsWithAllocations(builder, ',', Lists.newArrayList(CONTROL_SELECTED_TEST.getName(), GROUP1_SELECTED_TEST.getName()));
        assertThat(builder.toString().split(",")).containsExactly("");
    }

    @Test
    public void testAppendTestGroups() {
        StringBuilder builder = new StringBuilder();
        sampleGroups.appendTestGroups(builder, ',');
        assertThat(builder.toString().split(","))
                .containsExactlyInAnyOrder(
                        "groupwithfallbacktst2", "bgtst0", "abtst1", "holdout_tst2",
                        "#A1:bgtst0", "#A1:abtst1", "#A1:groupwithfallbacktst2", "#A1:holdout_tst2",
                        "#A1:no_definition_tst2", "no_definition_tst2");
        builder = new StringBuilder();
        sampleGroupsWithForced.appendTestGroups(builder, ',');
        assertThat(builder.toString().split(","))
                .containsExactlyInAnyOrder(
                        "groupwithfallbacktst2", "bgtst0", "abtst0", "holdout_tst2",
                        "#A1:bgtst0", "#A1:abtst0", "#A1:groupwithfallbacktst2", "#A1:holdout_tst2",
                        "#A1:no_definition_tst2", "no_definition_tst2");
    }

    @Test
    public void testGetJavaScriptConfig() {

        assertThat(emptyGroup.getJavaScriptConfig())
                .hasSize(0);

        assertThat(sampleGroups.getJavaScriptConfig())
                .hasSize(5)
                .containsEntry(GROUP1_SELECTED_TEST.getName(), 1)
                .containsEntry(CONTROL_SELECTED_TEST.getName(), 0)
                .containsEntry(GROUP_WITH_FALLBACK_TEST.getName(), 2)
                .containsEntry(HOLDOUT_MASTER_TEST.getName(), 2)
                .containsEntry(MISSING_DEFINITION_TEST.getName(), 2);

        assertThat(sampleGroupsWithForced.getJavaScriptConfig())
                .hasSize(5)
                .containsEntry(GROUP1_SELECTED_TEST.getName(), 0) // forced
                .containsEntry(CONTROL_SELECTED_TEST.getName(), 0)
                .containsEntry(GROUP_WITH_FALLBACK_TEST.getName(), 2)
                .containsEntry(HOLDOUT_MASTER_TEST.getName(), 2)
                .containsEntry(MISSING_DEFINITION_TEST.getName(), 2);

        assertThat(groupsWithHoldOut.getJavaScriptConfig())
                .hasSize(2)
                .containsEntry(HOLDOUT_MASTER_TEST.getName(), 2)
                .containsEntry(MISSING_DEFINITION_TEST.getName(), 2) // continue to return due to absent definition
                ;
    }

    @Test
    public void testGetJavaScriptConfigLists() {
        assertThat(sampleGroups.getJavaScriptConfig(new FakeTest[] {
                new FakeTest("notexist", 42),
                new FakeTest(CONTROL_SELECTED_TEST.getName(), 43),
                new FakeTest(GROUP1_SELECTED_TEST.getName(), 44)}))
                .containsExactly(
                        Arrays.asList(42, null),
                        Arrays.asList(0, "controlPayload"),
                        Arrays.asList(1, "activePayload")
                );
        assertThat(sampleGroupsWithForced.getJavaScriptConfig(new FakeTest[] {
                new FakeTest("notexist", 42),
                new FakeTest(CONTROL_SELECTED_TEST.getName(), 43),
                new FakeTest(GROUP1_SELECTED_TEST.getName(), 44)}))
                .containsExactly(
                        Arrays.asList(42, null),
                        Arrays.asList(0, "controlPayload"),
                        Arrays.asList(0, "controlPayload") // forced
                );
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
        // same instance, which was historically exposed
        assertThat(sampleGroups.getProctorResult()).isSameAs(proctorResult);

        // same data, but not same instance
        assertThat(sampleGroups.getRawProctorResult()).isNotSameAs(proctorResult);
        assertThat(sampleGroups.getRawProctorResult().getMatrixVersion()).isEqualTo(proctorResult.getMatrixVersion());
        assertThat(sampleGroups.getRawProctorResult().getBuckets()).isEqualTo(proctorResult.getBuckets());
        assertThat(sampleGroups.getRawProctorResult().getAllocations()).isEqualTo(proctorResult.getAllocations());
        assertThat(sampleGroups.getRawProctorResult().getTestDefinitions()).isEqualTo(proctorResult.getTestDefinitions());

        // same data, but not same instance
        assertThat(sampleGroups.getAsProctorResult()).isNotSameAs(proctorResult);
        assertThat(sampleGroups.getAsProctorResult().getMatrixVersion()).isEqualTo(proctorResult.getMatrixVersion());
        assertThat(sampleGroups.getAsProctorResult().getBuckets()).isEqualTo(proctorResult.getBuckets());
        assertThat(sampleGroups.getAsProctorResult().getAllocations()).isEqualTo(proctorResult.getAllocations());
        assertThat(sampleGroups.getAsProctorResult().getTestDefinitions()).isEqualTo(proctorResult.getTestDefinitions());

        // different data for bucket, else same, but not same instance
        assertThat(groupsWithHoldOut.getAsProctorResult()).isNotSameAs(proctorResult);
        assertThat(groupsWithHoldOut.getAsProctorResult().getMatrixVersion()).isEqualTo(proctorResult.getMatrixVersion());
        assertThat(groupsWithHoldOut.getAsProctorResult().getBuckets()).isNotEqualTo(proctorResult.getBuckets()); // not equal
        assertThat(groupsWithHoldOut.getAsProctorResult().getAllocations()).isEqualTo(proctorResult.getAllocations());
        assertThat(groupsWithHoldOut.getAsProctorResult().getTestDefinitions()).isEqualTo(proctorResult.getTestDefinitions());
    }

}
