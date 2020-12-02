package com.indeed.proctor.consumer.logging;

import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.consumer.ProctorGroupStubber;
import org.junit.Test;

import java.util.Collections;

import static com.indeed.proctor.consumer.ProctorGroupStubber.CONTROL_BUCKET_WITH_PAYLOAD;
import static com.indeed.proctor.consumer.ProctorGroupStubber.GROUP_1_BUCKET_WITH_PAYLOAD;
import static com.indeed.proctor.consumer.ProctorGroupStubber.INACTIVE_BUCKET;
import static org.assertj.core.api.Assertions.assertThat;

public class TestUsageMarkerTest {

    @Test
    public void isMarkedEmptyResult() {
        final TestUsageMarker marker = new TestUsageMarker(ProctorResult.EMPTY);
        assertThat(marker.isMarked("notexist")).isFalse();

        // assert no error
        marker.markTests(Collections.emptyList());


        final String unknownTest = "foo";
        marker.markTests(Collections.singletonList(unknownTest));
        assertThat(marker.isMarked(unknownTest)).isFalse();
    }

    @Test
    public void isMarked() {
        final ProctorResult proctorResult = new ProctorGroupStubber.ProctorResultStubBuilder()
                .withStubTest(ProctorGroupStubber.StubTest.CONTROL_SELECTED_TEST, CONTROL_BUCKET_WITH_PAYLOAD,
                        INACTIVE_BUCKET, CONTROL_BUCKET_WITH_PAYLOAD, GROUP_1_BUCKET_WITH_PAYLOAD)
                .withDynamicallyResolvedStubTest(ProctorGroupStubber.StubTest.GROUP1_SELECTED_TEST, GROUP_1_BUCKET_WITH_PAYLOAD,
                        INACTIVE_BUCKET, CONTROL_BUCKET_WITH_PAYLOAD, GROUP_1_BUCKET_WITH_PAYLOAD)
                .build();
        final TestUsageMarker marker = new TestUsageMarker(proctorResult);
        assertThat(marker.isMarked("notexist")).isFalse();
        assertThat(marker.isMarked(ProctorGroupStubber.StubTest.CONTROL_SELECTED_TEST.getName())).isFalse();
        // dynamically resolved tests premarked
        assertThat(marker.isMarked(ProctorGroupStubber.StubTest.GROUP1_SELECTED_TEST.getName())).isTrue();

        // assert no error
        marker.markTests(Collections.emptyList());

        final String unknownTest = "foo";
        marker.markTests(Collections.singletonList(unknownTest));
        assertThat(marker.isMarked(unknownTest)).isFalse();

        marker.markTests(Collections.singletonList(ProctorGroupStubber.StubTest.CONTROL_SELECTED_TEST.getName()));
        assertThat(marker.isMarked(ProctorGroupStubber.StubTest.CONTROL_SELECTED_TEST.getName())).isTrue();
        // dynamically resolved tests premarked
        assertThat(marker.isMarked(ProctorGroupStubber.StubTest.GROUP1_SELECTED_TEST.getName())).isTrue();
    }
}
