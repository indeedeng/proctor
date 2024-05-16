package com.indeed.proctor.consumer;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.consumer.ProctorGroupStubber.FakeTest;
import com.indeed.proctor.consumer.logging.TestGroupFormatter;
import com.indeed.proctor.consumer.logging.TestMarkingObserver;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static com.indeed.proctor.consumer.AbstractGroups.loggableAllocation;
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
import static com.indeed.proctor.consumer.ProctorGroupStubber.StubTest.SUPPRESS_LOGGING_TST;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestAbstractGroups {
    private TestMarkingObserver observer;
    private ProctorResult proctorResult;
    private AbstractGroups emptyGroup;
    private AbstractGroups sampleGroups;

    @Before
    public void setUp() {
        emptyGroup =
                new AbstractGroups(new ProctorResult("0", emptyMap(), emptyMap(), emptyMap())) {};

        proctorResult =
                new ProctorGroupStubber.ProctorResultStubBuilder()
                        .withStubTest(
                                ProctorGroupStubber.StubTest.CONTROL_SELECTED_TEST,
                                CONTROL_BUCKET_WITH_PAYLOAD,
                                INACTIVE_BUCKET,
                                CONTROL_BUCKET_WITH_PAYLOAD,
                                GROUP_1_BUCKET_WITH_PAYLOAD)
                        .withStubTest(
                                false,
                                ProctorGroupStubber.StubTest.SUPPRESS_LOGGING_TST,
                                CONTROL_BUCKET_WITH_PAYLOAD,
                                INACTIVE_BUCKET,
                                CONTROL_BUCKET_WITH_PAYLOAD,
                                GROUP_1_BUCKET_WITH_PAYLOAD)
                        .withStubTest(
                                ProctorGroupStubber.StubTest.GROUP1_SELECTED_TEST,
                                GROUP_1_BUCKET_WITH_PAYLOAD,
                                INACTIVE_BUCKET,
                                CONTROL_BUCKET_WITH_PAYLOAD,
                                GROUP_1_BUCKET_WITH_PAYLOAD)
                        .withStubTest(
                                ProctorGroupStubber.StubTest.INACTIVE_SELECTED_TEST,
                                INACTIVE_BUCKET,
                                INACTIVE_BUCKET,
                                GROUP_1_BUCKET)
                        // provides reference to FALLBACK_BUCKET that can be used in tests
                        .withStubTest(
                                ProctorGroupStubber.StubTest.GROUP_WITH_FALLBACK_TEST,
                                GROUP_1_BUCKET,
                                INACTIVE_BUCKET,
                                GROUP_1_BUCKET,
                                FALLBACK_TEST_BUCKET)
                        // provides reference to FALLBACK_BUCKET that can be used in tests, no
                        // resolved test
                        .withStubTest(
                                ProctorGroupStubber.StubTest.NO_BUCKETS_WITH_FALLBACK_TEST,
                                null,
                                INACTIVE_BUCKET,
                                GROUP_1_BUCKET,
                                FALLBACK_TEST_BUCKET)
                        .withStubTest(
                                ProctorGroupStubber.StubTest.MISSING_DEFINITION_TEST,
                                GROUP_1_BUCKET,
                                (ConsumableTestDefinition) null)
                        .build();
        observer = new TestMarkingObserver(proctorResult);
        sampleGroups = new AbstractGroups(proctorResult, observer) {};
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
        assertThat(sampleGroups.getPayload(INACTIVE_SELECTED_TEST.getName()))
                .isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(sampleGroups.getPayload(GROUP1_SELECTED_TEST.getName()))
                .isEqualTo(GROUP_1_BUCKET_WITH_PAYLOAD.getPayload());
        assertThat(sampleGroups.getPayload(CONTROL_SELECTED_TEST.getName()))
                .isEqualTo(CONTROL_BUCKET_WITH_PAYLOAD.getPayload());

        // Do not pick Fallback bucket for resolved buckets
        assertThat(sampleGroups.getPayload(GROUP1_SELECTED_TEST.getName(), FALLBACK_BUCKET))
                .isEqualTo(GROUP_1_BUCKET_WITH_PAYLOAD.getPayload());
        assertThat(sampleGroups.getPayload(CONTROL_SELECTED_TEST.getName(), FALLBACK_BUCKET))
                .isEqualTo(CONTROL_BUCKET_WITH_PAYLOAD.getPayload());
        assertThat(sampleGroups.getPayload(INACTIVE_SELECTED_TEST.getName(), FALLBACK_BUCKET))
                .isEqualTo(Payload.EMPTY_PAYLOAD);

        // Because group1 bucket here has no payload, return empty
        assertThat(sampleGroups.getPayload(GROUP_WITH_FALLBACK_TEST.getName(), FALLBACK_BUCKET))
                .isEqualTo(Payload.EMPTY_PAYLOAD);
        // because no bucket resolved, use fallback
        assertThat(
                        sampleGroups.getPayload(
                                NO_BUCKETS_WITH_FALLBACK_TEST.getName(), FALLBACK_BUCKET))
                .isEqualTo(FALLBACK_TEST_BUCKET.getPayload());
        assertThat(
                        sampleGroups.getPayload(
                                NO_BUCKETS_WITH_FALLBACK_TEST.getName(), FALLBACK_NOPAYLOAD_BUCKET))
                .isEqualTo(Payload.EMPTY_PAYLOAD);
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
        assertThat(sampleGroups.toLongString())
                .isEqualTo(
                        "abtst-group1,bgtst-control,btntst-inactive,groupwithfallbacktst-group1,no_definition_tst-group1,suppress_logging_example_tst-control");
    }

    @Test
    public void testToLoggingString() {
        assertThat(
                        (new AbstractGroups(
                                        new ProctorResult(
                                                "0", emptyMap(), emptyMap(), emptyMap())) {})
                                .toLoggingString())
                .isEmpty();
        assertThat(sampleGroups.toLoggingString())
                .isEqualTo(
                        "#A1:abtst1,#A1:bgtst0,#A1:groupwithfallbacktst2,#A1:no_definition_tst2");
    }

    @Test
    public void testCheckRolledOutAllocation() {
        final ConsumableTestDefinition td =
                ConsumableTestDefinition.fromTestDefinition(
                        TestDefinition.builder()
                                .setTestType(TestType.RANDOM)
                                .setSalt("foo")
                                .build());
        final ConsumableTestDefinition tdWithForceLogging =
                ConsumableTestDefinition.fromTestDefinition(
                        TestDefinition.builder()
                                .setTestType(TestType.RANDOM)
                                .setSalt("foo")
                                .setForceLogging(true)
                                .build());
        final ProctorResult result =
                new ProctorGroupStubber.ProctorResultStubBuilder()
                        .withStubTest(
                                false,
                                ProctorGroupStubber.StubTest.SUPPRESS_LOGGING_TST,
                                CONTROL_BUCKET_WITH_PAYLOAD,
                                INACTIVE_BUCKET,
                                CONTROL_BUCKET_WITH_PAYLOAD,
                                GROUP_1_BUCKET_WITH_PAYLOAD)
                        .build();

        assertThat(loggableAllocation("suppress_logging_example_tst", tdWithForceLogging, result))
                .isTrue();

        assertThat(loggableAllocation("suppress_logging_example_tst", td, result)).isFalse();
    }

    @Test
    public void testToLoggingStringWithExposureAndObserver() {

        // same logging as current AbstractGroups
        final ProctorGroupsWriter writer =
                new ProctorGroupsWriter.Builder(TestGroupFormatter.WITH_ALLOC_ID).build();

        // no test usage observed yet
        assertThat(writer.writeGroupsAsString(observer.asProctorResult())).isEmpty();

        // getGroupsString and getAsProctorResult should not mark tests as used
        final String fullLoggingString =
                "#A1:abtst1,#A1:bgtst0,#A1:groupwithfallbacktst2,#A1:no_definition_tst2";
        assertThat(sampleGroups.getAsProctorResult()).isNotNull();
        assertThat(sampleGroups.toLoggingString()).isEqualTo(fullLoggingString);
        assertThat(sampleGroups.toLongString()).isNotBlank();
        assertThat(sampleGroups.toString()).isEqualTo(fullLoggingString);

        // getActiveBucket is observed
        assertThat(sampleGroups.getActiveBucket(GROUP1_SELECTED_TEST.getName())).isNotEmpty();
        assertThat(writer.writeGroupsAsString(observer.asProctorResult())).isEqualTo("#A1:abtst1");

        // explicitly marked tests (e.g. from dynamic resolution)
        sampleGroups.markTestsUsed(singleton(CONTROL_SELECTED_TEST.getName()));
        assertThat(writer.writeGroupsAsString(observer.asProctorResult()))
                .isEqualTo("#A1:abtst1,#A1:bgtst0");

        // using JavascriptConfig means given tests might be exposed, so each test is marked as used
        assertThat(
                        sampleGroups.getJavaScriptConfig(
                                ImmutableSet.of(GROUP_WITH_FALLBACK_TEST.getName())))
                .isNotEmpty();
        assertThat(writer.writeGroupsAsString(observer.asProctorResult()))
                .isEqualTo("#A1:abtst1,#A1:bgtst0,#A1:groupwithfallbacktst2");

        // using JavascriptConfig without testnames means all tests might be exposed, so all tests
        // are marked as used
        assertThat(sampleGroups.getJavaScriptConfig()).isNotEmpty();
        assertThat(writer.writeGroupsAsString(observer.asProctorResult()))
                .isEqualTo(fullLoggingString);
    }

    @Test
    public void testGetLoggingTestNames() {
        assertThat(Sets.newHashSet(sampleGroups.getLoggingTestNames()))
                .containsExactlyInAnyOrder(
                        CONTROL_SELECTED_TEST.getName(),
                        GROUP1_SELECTED_TEST.getName(),
                        GROUP_WITH_FALLBACK_TEST.getName(),
                        MISSING_DEFINITION_TEST.getName());
    }

    @Test
    public void testAppendTestGroupsWithAllocations() {
        StringBuilder builder = new StringBuilder();
        sampleGroups.appendTestGroupsWithAllocations(
                builder,
                ',',
                Lists.newArrayList(
                        CONTROL_SELECTED_TEST.getName(), GROUP1_SELECTED_TEST.getName()));
        assertThat(builder.toString().split(",")).containsExactly("#A1:bgtst0", "#A1:abtst1");
        builder = new StringBuilder();
        emptyGroup.appendTestGroupsWithAllocations(
                builder,
                ',',
                Lists.newArrayList(
                        CONTROL_SELECTED_TEST.getName(), GROUP1_SELECTED_TEST.getName()));
        assertThat(builder.toString().split(",")).containsExactly("");
    }

    @Test
    public void testAppendTestGroups() {
        StringBuilder builder = new StringBuilder();
        sampleGroups.appendTestGroups(builder, ',');
        assertThat(builder.toString().split(","))
                .containsExactlyInAnyOrder(
                        "#A1:bgtst0",
                        "#A1:abtst1",
                        "#A1:groupwithfallbacktst2",
                        "#A1:no_definition_tst2");
    }

    @Test
    public void testGetJavaScriptConfig() {

        assertThat(emptyGroup.getJavaScriptConfig()).hasSize(0);

        assertThat(sampleGroups.getJavaScriptConfig())
                .hasSize(5)
                .containsEntry(GROUP1_SELECTED_TEST.getName(), 1)
                .containsEntry(CONTROL_SELECTED_TEST.getName(), 0)
                .containsEntry(GROUP_WITH_FALLBACK_TEST.getName(), 2)
                .containsEntry(MISSING_DEFINITION_TEST.getName(), 2)
                .containsEntry(SUPPRESS_LOGGING_TST.getName(), 0);
    }

    @Test
    public void testGetJavaScriptConfigLists() {
        assertThat(
                        sampleGroups.getJavaScriptConfig(
                                new FakeTest[] {
                                    new FakeTest("notexist", 42),
                                    new FakeTest(CONTROL_SELECTED_TEST.getName(), 43),
                                    new FakeTest(GROUP1_SELECTED_TEST.getName(), 44)
                                }))
                .containsExactly(
                        Arrays.asList(42, null),
                        Arrays.asList(0, CONTROL_BUCKET_WITH_PAYLOAD.getPayload().getStringValue()),
                        Arrays.asList(
                                1, GROUP_1_BUCKET_WITH_PAYLOAD.getPayload().getStringValue()));
    }

    @SuppressWarnings("deprecation") // intentionally calling deprecated
    @Test
    public void testGetLegacyProctorResults() {
        // same instance, which was historically exposed
        assertThat(sampleGroups.getProctorResult()).isSameAs(proctorResult);
    }

    @Test
    public void testProctorResults() {
        final ProctorResult rawProctorResult = sampleGroups.getRawProctorResult();
        assertThat(rawProctorResult).isNotSameAs(proctorResult);
        assertThat(rawProctorResult.getMatrixVersion()).isEqualTo(proctorResult.getMatrixVersion());
        assertThat(rawProctorResult.getBuckets()).isEqualTo(proctorResult.getBuckets());
        assertThat(rawProctorResult.getAllocations()).isEqualTo(proctorResult.getAllocations());
        assertThat(rawProctorResult.getTestDefinitions())
                .isEqualTo(proctorResult.getTestDefinitions());

        // ensure getRawProctorResult is unmodifiable
        final ProctorResult rawProctorResult2 = sampleGroups.getRawProctorResult();
        assertThatThrownBy(() -> rawProctorResult2.getBuckets().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> rawProctorResult2.getAllocations().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> rawProctorResult2.getTestDefinitions().clear())
                .isInstanceOf(UnsupportedOperationException.class);

        // same data, but not same instance
        final ProctorResult convertedProctorResult = sampleGroups.getAsProctorResult();
        assertThat(convertedProctorResult).isNotSameAs(proctorResult);
        assertThat(convertedProctorResult.getMatrixVersion())
                .isEqualTo(proctorResult.getMatrixVersion());
        assertThat(convertedProctorResult.getBuckets()).isEqualTo(proctorResult.getBuckets());
        assertThat(convertedProctorResult.getAllocations())
                .isEqualTo(proctorResult.getAllocations());
        assertThat(convertedProctorResult.getTestDefinitions())
                .isEqualTo(proctorResult.getTestDefinitions());

        // ensure getAsProctorResult is unmodifiable
        assertThatThrownBy(() -> convertedProctorResult.getBuckets().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> convertedProctorResult.getAllocations().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> convertedProctorResult.getTestDefinitions().clear())
                .isInstanceOf(UnsupportedOperationException.class);

        // check legacy behavior still works (changing the input ProctorResult changes behavior of
        // AbstractGroups)
        // probably this is a bug rather than an undocumented feature, but for now prefer not to
        // break clients
        proctorResult.getBuckets().clear();
        proctorResult.getAllocations().clear();
        proctorResult.getTestDefinitions().clear();

        assertThat(convertedProctorResult.getBuckets()).isEmpty();
        assertThat(convertedProctorResult.getAllocations()).isEmpty();
        assertThat(convertedProctorResult.getTestDefinitions()).isEmpty();

        assertThat(rawProctorResult.getBuckets()).isEmpty();
        assertThat(rawProctorResult.getAllocations()).isEmpty();
        assertThat(rawProctorResult.getTestDefinitions()).isEmpty();
    }
}
