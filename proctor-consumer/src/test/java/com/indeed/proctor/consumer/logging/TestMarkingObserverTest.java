package com.indeed.proctor.consumer.logging;

import com.google.common.collect.ImmutableSet;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.consumer.ProctorGroupStubber;
import org.junit.Test;

import static com.indeed.proctor.consumer.ProctorGroupStubber.CONTROL_BUCKET_WITH_PAYLOAD;
import static com.indeed.proctor.consumer.ProctorGroupStubber.GROUP_1_BUCKET_WITH_PAYLOAD;
import static com.indeed.proctor.consumer.ProctorGroupStubber.INACTIVE_BUCKET;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

public class TestMarkingObserverTest {

    @Test
    public void testAsProctorResultEmpty() {
        final TestMarkingObserver observer = new TestMarkingObserver(ProctorResult.EMPTY);
        assertThat(observer.asProctorResult().getBuckets())
                .isEqualTo(ProctorResult.EMPTY.getBuckets());
    }

    @Test
    public void testAsProctorResult() {
        final ProctorResult proctorResult =
                new ProctorGroupStubber.ProctorResultStubBuilder()
                        .withStubTest(
                                ProctorGroupStubber.StubTest.CONTROL_SELECTED_TEST,
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
                        .build();
        final TestMarkingObserver observer = new TestMarkingObserver(proctorResult);
        // empty
        assertThat(observer.asProctorResult().getBuckets()).isEmpty();
        assertThat(observer.asProctorResult().getAllocations()).isEmpty();
        assertThat(observer.asProctorResult().getMatrixVersion())
                .isEqualTo(proctorResult.getMatrixVersion());
        assertThat(observer.asProctorResult().getTestDefinitions()).isEmpty();

        observer.markUsedForToggling(ProctorGroupStubber.StubTest.GROUP1_SELECTED_TEST.getName());
        assertThat(observer.asProctorResult().getBuckets())
                .containsOnlyKeys(ProctorGroupStubber.StubTest.GROUP1_SELECTED_TEST.getName())
                .containsValue(GROUP_1_BUCKET_WITH_PAYLOAD);
        assertThat(observer.asProctorResult().getAllocations())
                .containsOnlyKeys(ProctorGroupStubber.StubTest.GROUP1_SELECTED_TEST.getName());
        assertThat(observer.asProctorResult().getMatrixVersion())
                .isEqualTo(proctorResult.getMatrixVersion());
        assertThat(observer.asProctorResult().getTestDefinitions())
                .containsOnlyKeys(ProctorGroupStubber.StubTest.GROUP1_SELECTED_TEST.getName());

        // add valid testname, now result should be equal to original
        observer.markTestsUsedForLogging(
                ProctorGroupStubber.StubTest.CONTROL_SELECTED_TEST.getName());
        assertThat(observer.asProctorResult().getBuckets()).isEqualTo(proctorResult.getBuckets());
        assertThat(observer.asProctorResult().getAllocations())
                .isEqualTo(proctorResult.getAllocations());
        assertThat(observer.asProctorResult().getMatrixVersion())
                .isEqualTo(proctorResult.getMatrixVersion());
        assertThat(observer.asProctorResult().getTestDefinitions())
                .isEqualTo(proctorResult.getTestDefinitions());

        // add invalid testname (check no exception)
        observer.markUsedForToggling(singleton("notexist"));
        assertThat(observer.asProctorResult().getBuckets())
                .containsOnlyKeys(
                        ProctorGroupStubber.StubTest.GROUP1_SELECTED_TEST.getName(),
                        ProctorGroupStubber.StubTest.CONTROL_SELECTED_TEST.getName())
                .containsValue(GROUP_1_BUCKET_WITH_PAYLOAD);
    }

    @Test
    public void testAsProctorResultSetsForToggling() {
        final ProctorResult proctorResult =
                new ProctorGroupStubber.ProctorResultStubBuilder()
                        .withStubTest(
                                ProctorGroupStubber.StubTest.CONTROL_SELECTED_TEST,
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
                        .build();
        final TestMarkingObserver observer = new TestMarkingObserver(proctorResult);
        // empty
        assertThat(observer.asProctorResult().getBuckets()).isEmpty();
        assertThat(observer.asProctorResult().getAllocations()).isEmpty();
        assertThat(observer.asProctorResult().getMatrixVersion())
                .isEqualTo(proctorResult.getMatrixVersion());
        assertThat(observer.asProctorResult().getTestDefinitions()).isEmpty();

        // add valid+invalid testnames, now result should be equal to original
        observer.markUsedForToggling(
                ImmutableSet.of(
                        ProctorGroupStubber.StubTest.GROUP1_SELECTED_TEST.getName(),
                        ProctorGroupStubber.StubTest.CONTROL_SELECTED_TEST.getName(),
                        "notexist"));

        assertThat(observer.asProctorResult().getBuckets()).isEqualTo(proctorResult.getBuckets());
        assertThat(observer.asProctorResult().getAllocations())
                .isEqualTo(proctorResult.getAllocations());
        assertThat(observer.asProctorResult().getMatrixVersion())
                .isEqualTo(proctorResult.getMatrixVersion());
        assertThat(observer.asProctorResult().getTestDefinitions())
                .isEqualTo(proctorResult.getTestDefinitions());
    }

    @Test
    public void testAsProctorResultSetsForLogging() {
        final ProctorResult proctorResult =
                new ProctorGroupStubber.ProctorResultStubBuilder()
                        .withStubTest(
                                ProctorGroupStubber.StubTest.CONTROL_SELECTED_TEST,
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
                        .build();
        final TestMarkingObserver observer = new TestMarkingObserver(proctorResult);
        // empty
        assertThat(observer.asProctorResult().getBuckets()).isEmpty();
        assertThat(observer.asProctorResult().getAllocations()).isEmpty();
        assertThat(observer.asProctorResult().getMatrixVersion())
                .isEqualTo(proctorResult.getMatrixVersion());
        assertThat(observer.asProctorResult().getTestDefinitions()).isEmpty();

        // add valid+invalid testnames, now result should be equal to original
        observer.markTestsUsedForLogging(
                ImmutableSet.of(
                        ProctorGroupStubber.StubTest.GROUP1_SELECTED_TEST.getName(),
                        ProctorGroupStubber.StubTest.CONTROL_SELECTED_TEST.getName(),
                        "notexist"));

        assertThat(observer.asProctorResult().getBuckets()).isEqualTo(proctorResult.getBuckets());
        assertThat(observer.asProctorResult().getAllocations())
                .isEqualTo(proctorResult.getAllocations());
        assertThat(observer.asProctorResult().getMatrixVersion())
                .isEqualTo(proctorResult.getMatrixVersion());
        assertThat(observer.asProctorResult().getTestDefinitions())
                .isEqualTo(proctorResult.getTestDefinitions());
    }
}
