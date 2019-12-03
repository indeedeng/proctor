package com.indeed.proctor.consumer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.consumer.ProctorGroupStubber.ProctorGroupsForTest;
import com.indeed.proctor.consumer.ProctorGroupStubber.ProctorGroupsWithHoldout;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static com.indeed.proctor.consumer.ProctorGroupStubber.*;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestAbstractGroups {

    private ProctorGroupsForTest emptyGroup;
    private ProctorGroupsForTest groups;
    private ProctorGroupsWithForced groupsWithForced;
    private ProctorGroupsWithHoldout groupsWithHoldOut;

    @Before
    public void setUp() {
        final ProctorResult proctorResult = buildProctorResult();
        emptyGroup = new ProctorGroupsForTest(new ProctorResult("0", emptyMap(), emptyMap(), emptyMap()));
        groups = new ProctorGroupsForTest(proctorResult);
        groupsWithForced = new ProctorGroupsWithForced(proctorResult);
        groupsWithHoldOut = new ProctorGroupsWithHoldout(proctorResult);
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
        assertThat(groups.getPayload(ACTIVE_TESTNAME)).isEqualTo(new Payload("activePayload"));
        assertThat(groups.getPayload(CONTROL_TESTNAME)).isEqualTo(new Payload("controlPayload"));
        assertThat(groups.getPayload(ACTIVE_TESTNAME, FALLBACK_BUCKET)).isEqualTo(new Payload("activePayload"));
        assertThat(groups.getPayload(CONTROL_TESTNAME, FALLBACK_BUCKET)).isEqualTo(new Payload("controlPayload"));
        assertThat(groups.getPayload(GROUP_WITH_FALLBACK_TESTNAME, FALLBACK_BUCKET)).isEqualTo(new Payload("fallback"));
        assertThat(groups.getPayload(NO_BUCKETS_WITH_FALLBACK_TESTNAME, FALLBACK_BUCKET)).isEqualTo(new Payload("fallback"));
        assertThat(groups.getPayload(NO_BUCKETS_WITH_FALLBACK_TESTNAME, FALLBACK_NOPAYLOAD_BUCKET)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groups.getPayload("notexist")).isEqualTo(Payload.EMPTY_PAYLOAD);

        assertThat(groupsWithForced.getPayload(INACTIVE_TESTNAME)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groupsWithForced.getPayload(ACTIVE_TESTNAME)).isEqualTo(new Payload("controlPayload")); // forced
        assertThat(groupsWithForced.getPayload(CONTROL_TESTNAME)).isEqualTo(new Payload("controlPayload"));

        assertThat(groupsWithHoldOut.getPayload(INACTIVE_TESTNAME)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groupsWithHoldOut.getPayload(ACTIVE_TESTNAME)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groupsWithHoldOut.getPayload(CONTROL_TESTNAME)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groupsWithHoldOut.getPayload(ACTIVE_TESTNAME, FALLBACK_BUCKET)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groupsWithHoldOut.getPayload(CONTROL_TESTNAME, FALLBACK_BUCKET)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groupsWithHoldOut.getPayload(GROUP_WITH_FALLBACK_TESTNAME, FALLBACK_BUCKET)).isEqualTo(new Payload("fallback"));
        assertThat(groupsWithHoldOut.getPayload(NO_BUCKETS_WITH_FALLBACK_TESTNAME, FALLBACK_BUCKET)).isEqualTo(new Payload("fallback"));
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
        assertThat(groups.toLongString()).isEqualTo("abtst-active,bgtst-control,btntst-inactive,groupwithfallbacktst-active,holdout_tst-active,no_definition_tst-active");
        assertThat(groupsWithForced.toLongString()).isEqualTo("abtst-control,bgtst-control,btntst-inactive,groupwithfallbacktst-active,holdout_tst-active,no_definition_tst-active");
        assertThat(groupsWithHoldOut.toLongString()).isEqualTo("abtst-inactive,bgtst-inactive,btntst-inactive,groupwithfallbacktst-inactive,holdout_tst-active,no_definition_tst-active");
    }

    @Test
    public void testToLoggingString() {
        assertThat(new ProctorGroupsForTest(new ProctorResult("0", emptyMap(), emptyMap(), emptyMap())).toLoggingString()).isEmpty();
        assertThat(groups.toLoggingString()).isEqualTo("abtst1,bgtst0,groupwithfallbacktst2,holdout_tst2,no_definition_tst2,#B2:abtst1,#A1:bgtst0,#B2:groupwithfallbacktst2,#A1:holdout_tst2,#A5:no_definition_tst2");
        assertThat(groupsWithForced.toLoggingString()).isEqualTo("abtst0,bgtst0,groupwithfallbacktst2,holdout_tst2,no_definition_tst2,#B2:abtst0,#A1:bgtst0,#B2:groupwithfallbacktst2,#A1:holdout_tst2,#A5:no_definition_tst2");
        assertThat(groupsWithHoldOut.toLoggingString()).isEqualTo("holdout_tst2,no_definition_tst2,#A1:holdout_tst2,#A5:no_definition_tst2");
    }

    @Test
    public void testGetLoggingTestNames() {
        assertThat(Sets.newHashSet(groups.getLoggingTestNames()))
                .containsExactlyInAnyOrder(CONTROL_TESTNAME, ACTIVE_TESTNAME, GROUP_WITH_FALLBACK_TESTNAME, HOLDOUT_TESTNAME, NO_DEFINITION_TESTNAME);
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
                        "#A1:bgtst0", "#B2:abtst1", "#B2:groupwithfallbacktst2", "#A1:holdout_tst2",
                        "#A5:no_definition_tst2", "no_definition_tst2");
        builder = new StringBuilder();
        groupsWithForced.appendTestGroups(builder, ',');
        assertThat(builder.toString().split(","))
                .containsExactlyInAnyOrder(
                        "groupwithfallbacktst2", "bgtst0", "abtst0", "holdout_tst2",
                        "#A1:bgtst0", "#B2:abtst0", "#B2:groupwithfallbacktst2", "#A1:holdout_tst2",
                        "#A5:no_definition_tst2", "no_definition_tst2");
    }

    @Test
    public void testGetJavaScriptConfig() {

        assertThat(emptyGroup.getJavaScriptConfig())
                .hasSize(0);

        assertThat(groups.getJavaScriptConfig())
                .hasSize(5)
                .containsEntry(ACTIVE_TESTNAME, 1)
                .containsEntry(CONTROL_TESTNAME, 0)
                .containsEntry(GROUP_WITH_FALLBACK_TESTNAME, 2)
                .containsEntry(HOLDOUT_TESTNAME, 2)
                .containsEntry(NO_DEFINITION_TESTNAME, 2);

        assertThat(groupsWithForced.getJavaScriptConfig())
                .hasSize(5)
                .containsEntry(ACTIVE_TESTNAME, 0) // forced
                .containsEntry(CONTROL_TESTNAME, 0)
                .containsEntry(GROUP_WITH_FALLBACK_TESTNAME, 2)
                .containsEntry(HOLDOUT_TESTNAME, 2)
                .containsEntry(NO_DEFINITION_TESTNAME, 2);

        assertThat(groupsWithHoldOut.getJavaScriptConfig())
                .hasSize(2)
                .containsEntry(HOLDOUT_TESTNAME, 2)
                .containsEntry(NO_DEFINITION_TESTNAME, 2) // continue to return due to absent definition
                ;
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
}
