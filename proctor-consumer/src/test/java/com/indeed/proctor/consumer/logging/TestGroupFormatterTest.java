package com.indeed.proctor.consumer.logging;

import com.indeed.proctor.common.model.TestBucket;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestGroupFormatterTest {

    @Test
    public void testWithAllocId() {
        assertThat(buildLogString(
                TestGroupFormatter.WITH_ALLOC_ID,
                "fooTest",
                "#A1",
                new TestBucket("inactive", -1, "")))
        .isEqualTo("#A1:fooTest-1");
        assertThat(buildLogString(
                TestGroupFormatter.WITH_ALLOC_ID,
                "fooTest",
                "",
                new TestBucket("inactive", -1, "")))
                .isEqualTo("");
    }

    @Test
    public void testWithoutAllocId() {
        assertThat(buildLogString(
                TestGroupFormatter.WITHOUT_ALLOC_ID,
                "fooTest",
                "#A1",
                new TestBucket("inactive", -1, "")))
                .isEqualTo("fooTest-1");
        assertThat(buildLogString(
                TestGroupFormatter.WITHOUT_ALLOC_ID,
                "fooTest",
                "",
                new TestBucket("inactive", -1, "")))
                .isEqualTo("fooTest-1");
    }

    private static String buildLogString(
            final TestGroupFormatter testGroupFormatter,
            final String testName,
            final String allocId,
            final TestBucket bucket
    ) {
        final StringBuilder sb = new StringBuilder();
        testGroupFormatter
                .appendProctorTestGroup(sb, testName, allocId, bucket);
        return sb.toString();
    }

}
