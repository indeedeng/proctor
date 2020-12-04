package com.indeed.proctor.consumer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.consumer.ProctorGroupStubber.FakeTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static com.indeed.proctor.consumer.ProctorGroupStubber.CONTROL_BUCKET_WITH_PAYLOAD;
import static com.indeed.proctor.consumer.ProctorGroupStubber.FALLBACK_BUCKET;
import static com.indeed.proctor.consumer.ProctorGroupStubber.FALLBACK_NOPAYLOAD_BUCKET;
import static com.indeed.proctor.consumer.ProctorGroupStubber.FALLBACK_TEST_BUCKET;
import static com.indeed.proctor.consumer.ProctorGroupStubber.GROUP_1_BUCKET;
import static com.indeed.proctor.consumer.ProctorGroupStubber.GROUP_1_BUCKET_WITH_PAYLOAD;
import static com.indeed.proctor.consumer.ProctorGroupStubber.INACTIVE_BUCKET;
import static com.indeed.proctor.consumer.ProctorGroupStubber.StubTest.CONTROL_SELECTED_TEST;
import static com.indeed.proctor.consumer.ProctorGroupStubber.StubTest.GROUP1_SELECTED_TEST;
import static com.indeed.proctor.consumer.ProctorGroupStubber.StubTest.GROUP_WITH_FALLBACK_TEST;
import static com.indeed.proctor.consumer.ProctorGroupStubber.StubTest.INACTIVE_SELECTED_TEST;
import static com.indeed.proctor.consumer.ProctorGroupStubber.StubTest.MISSING_DEFINITION_TEST;
import static com.indeed.proctor.consumer.ProctorGroupStubber.StubTest.NO_BUCKETS_WITH_FALLBACK_TEST;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestAbstractGroups {

    private ProctorResult proctorResult;
    private AbstractGroups emptyGroup;
    private AbstractGroups sampleGroups;

    @Before
    public void setUp() {
        emptyGroup = new AbstractGroups(new ProctorResult("0", emptyMap(), emptyMap(), emptyMap())) {};

        proctorResult = new ProctorGroupStubber.ProctorResultStubBuilder()
                .withStubTest(ProctorGroupStubber.StubTest.CONTROL_SELECTED_TEST, CONTROL_BUCKET_WITH_PAYLOAD,
                        INACTIVE_BUCKET, CONTROL_BUCKET_WITH_PAYLOAD, GROUP_1_BUCKET_WITH_PAYLOAD)
                .withStubTest(ProctorGroupStubber.StubTest.GROUP1_SELECTED_TEST, GROUP_1_BUCKET_WITH_PAYLOAD,
                        INACTIVE_BUCKET, CONTROL_BUCKET_WITH_PAYLOAD, GROUP_1_BUCKET_WITH_PAYLOAD)
                .withStubTest(ProctorGroupStubber.StubTest.INACTIVE_SELECTED_TEST, INACTIVE_BUCKET,
                        INACTIVE_BUCKET, GROUP_1_BUCKET)
                // provides reference to FALLBACK_BUCKET that can be used in tests
                .withStubTest(ProctorGroupStubber.StubTest.GROUP_WITH_FALLBACK_TEST, GROUP_1_BUCKET,
                        INACTIVE_BUCKET, GROUP_1_BUCKET, FALLBACK_TEST_BUCKET)
                // provides reference to FALLBACK_BUCKET that can be used in tests, no resolved test
                .withStubTest(ProctorGroupStubber.StubTest.NO_BUCKETS_WITH_FALLBACK_TEST, null,
                        INACTIVE_BUCKET, GROUP_1_BUCKET, FALLBACK_TEST_BUCKET)
                .withStubTest(ProctorGroupStubber.StubTest.MISSING_DEFINITION_TEST, GROUP_1_BUCKET, (ConsumableTestDefinition) null)
                .build();
        sampleGroups = new AbstractGroups(proctorResult) {};
    }

    @Test
    public void testIsBucketActive() {
        assertFalse(sampleGroups.isBucketActive(CONTROL_SELECTED_TEST.getName(), -1));
        assertTrue(sampleGroups.isBucketActive(CONTROL_SELECTED_TEST.getName(), 0)); // selected
        assertFalse(sampleGroups.isBucketActive(CONTROL_SELECTED_TEST.getName(), 1));

        assertFalse(sampleGroups.isBucketActive(GROUP1_SELECTED_TEST.getName(), -1));
        assertFalse(sampleGroups.isBucketActive(GROUP1_SELECTED_TEST.getName(), 0));
        assertTrue(sampleGroups.isBucketActive(GROUP1_SELECTED_TEST.getName(), 1)); // selected

        assertFalse(sampleGroups.isBucketActive(CONTROL_SELECTED_TEST.getName(), -1, 42));
        assertTrue(sampleGroups.isBucketActive(CONTROL_SELECTED_TEST.getName(), 0, 42)); // selected

        assertFalse(sampleGroups.isBucketActive("notexist", -1));
        assertTrue(sampleGroups.isBucketActive("notexist", 1, 1)); // using default value
        assertFalse(sampleGroups.isBucketActive("notexist", 1, 2));

        assertFalse(emptyGroup.isBucketActive("notexist", -1));
        assertTrue(emptyGroup.isBucketActive("notexist", 1, 1)); // using default value
        assertFalse(emptyGroup.isBucketActive("notexist", 1, 2));

    }

    @Test
    public void testGetValue() {
        assertThat(sampleGroups.getValue(CONTROL_SELECTED_TEST.getName(), 42)).isEqualTo(0);
        assertThat(sampleGroups.getValue(GROUP1_SELECTED_TEST.getName(), 42)).isEqualTo(1);
        assertThat(sampleGroups.getValue("notexist", 42)).isEqualTo(42); // using default

        assertThat(emptyGroup.getValue("notexist", 42)).isEqualTo(42); // no fallback bucket
    }

    @Test
    public void testGetPayload() {
        assertThat(sampleGroups.getPayload(INACTIVE_SELECTED_TEST.getName())).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(sampleGroups.getPayload(GROUP1_SELECTED_TEST.getName())).isEqualTo(GROUP_1_BUCKET_WITH_PAYLOAD.getPayload());
        assertThat(sampleGroups.getPayload(CONTROL_SELECTED_TEST.getName())).isEqualTo(CONTROL_BUCKET_WITH_PAYLOAD.getPayload());

        // Do not pick Fallback bucket for resolved buckets
        assertThat(sampleGroups.getPayload(GROUP1_SELECTED_TEST.getName(), FALLBACK_BUCKET)).isEqualTo(GROUP_1_BUCKET_WITH_PAYLOAD.getPayload());
        assertThat(sampleGroups.getPayload(CONTROL_SELECTED_TEST.getName(), FALLBACK_BUCKET)).isEqualTo(CONTROL_BUCKET_WITH_PAYLOAD.getPayload());
        assertThat(sampleGroups.getPayload(INACTIVE_SELECTED_TEST.getName(), FALLBACK_BUCKET)).isEqualTo(Payload.EMPTY_PAYLOAD);

        // Because group1 bucket here has no payload, return empty
        assertThat(sampleGroups.getPayload(GROUP_WITH_FALLBACK_TEST.getName(), FALLBACK_BUCKET)).isEqualTo(Payload.EMPTY_PAYLOAD);
        // because no bucket resolved, use fallback
        assertThat(sampleGroups.getPayload(NO_BUCKETS_WITH_FALLBACK_TEST.getName(), FALLBACK_BUCKET)).isEqualTo(FALLBACK_TEST_BUCKET.getPayload());
        assertThat(sampleGroups.getPayload(NO_BUCKETS_WITH_FALLBACK_TEST.getName(), FALLBACK_NOPAYLOAD_BUCKET)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(sampleGroups.getPayload("notexist")).isEqualTo(Payload.EMPTY_PAYLOAD);

        assertThat(emptyGroup.getPayload("notexist")).isEqualTo(Payload.EMPTY_PAYLOAD);
    }

    @Test
    public void testIsEmpty() {
        assertThat(emptyGroup.isEmpty()).isTrue();
        assertThat(sampleGroups.isEmpty()).isFalse();
    }

    @Test
    public void testToLongString() {
        assertThat(emptyGroup.toLongString()).isEmpty();
        assertThat(sampleGroups.toLongString()).isEqualTo("abtst-group1,bgtst-control,btntst-inactive,groupwithfallbacktst-group1,no_definition_tst-group1");
    }

    @Test
    public void testToLoggingString() {
        assertThat((new AbstractGroups(new ProctorResult("0", emptyMap(), emptyMap(), emptyMap())) {}).toLoggingString()).isEmpty();
        assertThat(sampleGroups.toLoggingString()).isEqualTo("abtst1,bgtst0,groupwithfallbacktst2,no_definition_tst2,#A1:abtst1,#A1:bgtst0,#A1:groupwithfallbacktst2,#A1:no_definition_tst2");
    }

    @Test
    public void testGetLoggingTestNames() {
        assertThat(Sets.newHashSet(sampleGroups.getLoggingTestNames()))
                .containsExactlyInAnyOrder(CONTROL_SELECTED_TEST.getName(), GROUP1_SELECTED_TEST.getName(), GROUP_WITH_FALLBACK_TEST.getName(), MISSING_DEFINITION_TEST.getName());
    }

    @Test
    public void testAppendTestGroupsWithoutAllocations() {
        StringBuilder builder = new StringBuilder();
        sampleGroups.appendTestGroupsWithoutAllocations(builder, ',', Lists.newArrayList(CONTROL_SELECTED_TEST.getName(), GROUP1_SELECTED_TEST.getName()));
        assertThat(builder.toString().split(",")).containsExactly("bgtst0", "abtst1");

        builder = new StringBuilder();
        emptyGroup.appendTestGroupsWithoutAllocations(builder, ',', Lists.newArrayList(CONTROL_SELECTED_TEST.getName(), GROUP1_SELECTED_TEST.getName()));
        assertThat(builder.toString()).isEmpty();
    }

    @Test
    public void testAppendTestGroupsWithAllocations() {
        StringBuilder builder = new StringBuilder();
        sampleGroups.appendTestGroupsWithAllocations(builder, ',', Lists.newArrayList(CONTROL_SELECTED_TEST.getName(), GROUP1_SELECTED_TEST.getName()));
        assertThat(builder.toString().split(",")).containsExactly("#A1:bgtst0", "#A1:abtst1");
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
                        "groupwithfallbacktst2", "bgtst0", "abtst1",
                        "#A1:bgtst0", "#A1:abtst1", "#A1:groupwithfallbacktst2",
                        "#A1:no_definition_tst2", "no_definition_tst2");
    }

    @Test
    public void testGetJavaScriptConfig() {

        assertThat(emptyGroup.getJavaScriptConfig())
                .hasSize(0);

        assertThat(sampleGroups.getJavaScriptConfig())
                .hasSize(4)
                .containsEntry(GROUP1_SELECTED_TEST.getName(), 1)
                .containsEntry(CONTROL_SELECTED_TEST.getName(), 0)
                .containsEntry(GROUP_WITH_FALLBACK_TEST.getName(), 2)
                .containsEntry(MISSING_DEFINITION_TEST.getName(), 2);
    }

    @Test
    public void testGetJavaScriptConfigLists() {
        assertThat(sampleGroups.getJavaScriptConfig(new FakeTest[] {
                new FakeTest("notexist", 42),
                new FakeTest(CONTROL_SELECTED_TEST.getName(), 43),
                new FakeTest(GROUP1_SELECTED_TEST.getName(), 44)}))
                .containsExactly(
                        Arrays.asList(42, null),
                        Arrays.asList(0, CONTROL_BUCKET_WITH_PAYLOAD.getPayload().getStringValue()),
                        Arrays.asList(1, GROUP_1_BUCKET_WITH_PAYLOAD.getPayload().getStringValue())
                );
    }

    @Test
    public void testProctorResults() {
        // same instance, which was historically exposed
        assertThat(sampleGroups.getProctorResult()).isSameAs(proctorResult);

        // same data, but not same instance
        final ProctorResult rawProctorResult = sampleGroups.getRawProctorResult();
        assertThat(rawProctorResult).isNotSameAs(proctorResult);
        assertThat(rawProctorResult.getMatrixVersion()).isEqualTo(proctorResult.getMatrixVersion());
        assertThat(rawProctorResult.getBuckets()).isEqualTo(proctorResult.getBuckets());
        assertThat(rawProctorResult.getAllocations()).isEqualTo(proctorResult.getAllocations());
        assertThat(rawProctorResult.getTestDefinitions()).isEqualTo(proctorResult.getTestDefinitions());

        // avoid creation of new copies on each call
        final ProctorResult rawProctorResult2 = sampleGroups.getRawProctorResult();
        assertThat(rawProctorResult2).isSameAs(rawProctorResult);

        // same data, but not same instance
        final ProctorResult convertedProctorResult = sampleGroups.getAsProctorResult();
        assertThat(convertedProctorResult).isNotSameAs(proctorResult);
        assertThat(convertedProctorResult.getMatrixVersion()).isEqualTo(proctorResult.getMatrixVersion());
        assertThat(convertedProctorResult.getBuckets()).isEqualTo(proctorResult.getBuckets());
        assertThat(convertedProctorResult.getAllocations()).isEqualTo(proctorResult.getAllocations());
        assertThat(convertedProctorResult.getTestDefinitions()).isEqualTo(proctorResult.getTestDefinitions());

        final ProctorResult convertedProctorResult2 = sampleGroups.getAsProctorResult();
        assertThat(convertedProctorResult2.getAllocations()).isSameAs(convertedProctorResult.getAllocations());
        assertThat(convertedProctorResult2.getTestDefinitions()).isSameAs(convertedProctorResult.getTestDefinitions());
    }

}
