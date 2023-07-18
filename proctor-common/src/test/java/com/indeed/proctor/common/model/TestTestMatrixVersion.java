package com.indeed.proctor.common.model;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class TestTestMatrixVersion {
    @Test
    public void testCopyConstructor() {
        final TestMatrixVersion testMatrixVersion =
                new TestMatrixVersion(
                        new TestMatrixDefinition(ImmutableMap.of("test_a", new TestDefinition())),
                        new Date(0),
                        "v1",
                        "test matrix",
                        "dummy author");
        final TestMatrixVersion copiedMatrixVersion = new TestMatrixVersion(testMatrixVersion);
        assertThat(copiedMatrixVersion).isEqualTo(testMatrixVersion);

        testMatrixVersion.setVersion("another version");
        assertThat(copiedMatrixVersion).isNotEqualTo(testMatrixVersion);
    }
}
