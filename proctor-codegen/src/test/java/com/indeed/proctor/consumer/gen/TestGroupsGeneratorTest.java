package com.indeed.proctor.consumer.gen;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.TestSpecification;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestGroupsGeneratorTest {
    @Test
    public void testValidateTestSpecification_NoError() throws CodeGenException {
        TestGroupsGenerator.validateTestSpecification(
                "example_tst",
                stubTestSpecification(ImmutableMap.of("control", 0))); // should throw no exception
    }

    @Test
    public void testValidateTestSpecification_DuplicatedValue() {
        assertThatThrownBy(
                        () ->
                                TestGroupsGenerator.validateTestSpecification(
                                        "example_tst",
                                        stubTestSpecification(
                                                ImmutableMap.of(
                                                        "control", 0, "another_control", 0))))
                .isInstanceOf(CodeGenException.class);
    }

    @Test
    public void testValidateTestSpecification_NullValue() throws CodeGenException {
        final Map<String, Integer> buckets = new HashMap<>(); // no ImmutableMap to allow null
        buckets.put("control", null);
        assertThatThrownBy(
                        () ->
                                TestGroupsGenerator.validateTestSpecification(
                                        "example_tst", stubTestSpecification(buckets)))
                .isInstanceOf(CodeGenException.class);
    }

    private static TestSpecification stubTestSpecification(final Map<String, Integer> buckets) {
        final TestSpecification testSpecification = new TestSpecification();
        testSpecification.setBuckets(buckets);
        return testSpecification;
    }
}
