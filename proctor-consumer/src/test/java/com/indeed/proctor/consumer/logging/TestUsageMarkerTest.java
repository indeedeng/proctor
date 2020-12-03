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
        final TestUsageMarker marker = new TestUsageMarker(0);
        assertThat(marker.isMarked("notexist")).isFalse();

        // assert no error
        marker.markTests(Collections.emptyList());

        final String anyName = "foo";
        marker.markTests(Collections.singletonList(anyName));
        assertThat(marker.isMarked(anyName)).isTrue();
    }

    @Test
    public void isMarked() {
        final ProctorResult proctorResult = new ProctorGroupStubber.ProctorResultStubBuilder()
                .withStubTest(ProctorGroupStubber.StubTest.CONTROL_SELECTED_TEST, CONTROL_BUCKET_WITH_PAYLOAD,
                        INACTIVE_BUCKET, CONTROL_BUCKET_WITH_PAYLOAD, GROUP_1_BUCKET_WITH_PAYLOAD)
                .withDynamicallyResolvedStubTest(ProctorGroupStubber.StubTest.GROUP1_SELECTED_TEST, GROUP_1_BUCKET_WITH_PAYLOAD,
                        INACTIVE_BUCKET, CONTROL_BUCKET_WITH_PAYLOAD, GROUP_1_BUCKET_WITH_PAYLOAD)
                .build();
        final TestUsageMarker marker = new TestUsageMarker(proctorResult.getBuckets().size());
        assertThat(marker.isMarked("notexist")).isFalse();
        assertThat(marker.isMarked(ProctorGroupStubber.StubTest.CONTROL_SELECTED_TEST.getName())).isFalse();
        assertThat(marker.isMarked(ProctorGroupStubber.StubTest.GROUP1_SELECTED_TEST.getName())).isFalse();

        // assert no error
        marker.markTests(Collections.emptyList());

        final String anyTestName = "foo";
        marker.markTests(Collections.singletonList(anyTestName));
        assertThat(marker.isMarked(anyTestName)).isTrue();

        marker.markTests(Collections.singletonList(ProctorGroupStubber.StubTest.CONTROL_SELECTED_TEST.getName()));
        assertThat(marker.isMarked(ProctorGroupStubber.StubTest.CONTROL_SELECTED_TEST.getName())).isTrue();
        assertThat(marker.isMarked(ProctorGroupStubber.StubTest.GROUP1_SELECTED_TEST.getName())).isFalse();
    }
}
