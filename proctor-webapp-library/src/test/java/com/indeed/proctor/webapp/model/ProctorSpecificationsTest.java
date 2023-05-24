package com.indeed.proctor.webapp.model;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.common.TestSpecification;
import com.indeed.proctor.common.dynamic.DynamicFilter;
import com.indeed.proctor.common.dynamic.DynamicFilters;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ProctorSpecificationsTest {
    @Test
    public void testGetRequiredTests() {
        final ProctorSpecification specA = new ProctorSpecification();
        specA.setTests(
                ImmutableMap.of(
                        "a_test", stubTestSpecification(),
                        "b_test", stubTestSpecification()));

        final ProctorSpecification specB = new ProctorSpecification();
        specB.setTests(
                ImmutableMap.of(
                        "b_test", stubTestSpecification(),
                        "c_test", stubTestSpecification()));

        final ProctorSpecifications specifications =
                new ProctorSpecifications(Arrays.asList(specA, specB));

        assertThat(specifications.getRequiredTests())
                .containsOnlyKeys("a_test", "b_test", "c_test")
                .hasEntrySatisfying("a_test", e -> assertThat(e).hasSize(1))
                .hasEntrySatisfying("b_test", e -> assertThat(e).hasSize(2))
                .hasEntrySatisfying("c_test", e -> assertThat(e).hasSize(1));
    }

    @Test
    public void testGetDynamicTests() {
        final ProctorSpecification specA = new ProctorSpecification();
        specA.setDynamicFilters(
                new DynamicFilters(Collections.singleton(exactNameMatching("a_test"))));

        final ProctorSpecification specB = new ProctorSpecification();
        specB.setDynamicFilters(
                new DynamicFilters(Collections.singleton(exactNameMatching("b_test"))));

        final ProctorSpecifications specifications =
                new ProctorSpecifications(Arrays.asList(specA, specB));

        final Map<String, ConsumableTestDefinition> requiredTests =
                ImmutableMap.of(
                        "a_test", stubTestDefinition(),
                        "b_test", stubTestDefinition(),
                        "c_test", stubTestDefinition());

        assertThat(specifications.getDynamicTests(requiredTests)).containsOnly("a_test", "b_test");
    }

    @Test
    public void testGetResolvedTests() {
        final ProctorSpecification specA = new ProctorSpecification();
        specA.setTests(
                ImmutableMap.of(
                        "a_test", stubTestSpecification(),
                        "d_test", stubTestSpecification()));

        final ProctorSpecification specB = new ProctorSpecification();
        specB.setDynamicFilters(
                new DynamicFilters(Collections.singleton(exactNameMatching("b_test"))));

        final ProctorSpecifications specifications =
                new ProctorSpecifications(Arrays.asList(specA, specB));

        final Map<String, ConsumableTestDefinition> requiredTests =
                ImmutableMap.of(
                        "a_test", stubTestDefinition(),
                        "b_test", stubTestDefinition(),
                        "c_test", stubTestDefinition());

        assertThat(specifications.getResolvedTests(requiredTests))
                .containsOnly("a_test", "b_test", "d_test");
    }

    private static DynamicFilter exactNameMatching(final String value) {
        return (testName, testDefinition) -> testName.equals(value);
    }

    private static ConsumableTestDefinition stubTestDefinition() {
        return new ConsumableTestDefinition();
    }

    private static TestSpecification stubTestSpecification() {
        final TestSpecification testSpecification = new TestSpecification();
        testSpecification.setBuckets(
                ImmutableMap.of(
                        "inactive", -1,
                        "active", 1));
        return testSpecification;
    }
}
