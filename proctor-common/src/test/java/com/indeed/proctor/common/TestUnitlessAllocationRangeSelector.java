package com.indeed.proctor.common;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestType;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class TestUnitlessAllocationRangeSelector {
    @Test
    public void testFindMatchingRule_UnitlessAllocation() {
        final TestRangeSelector selector =
                createTestRangeSelector(
                        stubTestDefinition(
                                        Arrays.asList(
                                                "missingExperimentalUnit && country == 'US'",
                                                "missingExperimentalUnit && country == 'UK'",
                                                "country == 'US'"),
                                        true)
                                .build(),
                        new IdentifierValidator.NoEmpty());
        assertThat(
                        selector.findMatchingRule(
                                ImmutableMap.of("country", "US", "missingExperimentalUnit", "true"),
                                emptyMap(),
                                ""))
                .isEqualTo(0);

        assertThat(
                        selector.findMatchingRule(
                                ImmutableMap.of("country", "UK", "missingExperimentalUnit", "true"),
                                emptyMap(),
                                ""))
                .isEqualTo(1);

        assertThat(
                        selector.findMatchingRule(
                                ImmutableMap.of(
                                        "country", "US", "missingExperimentalUnit", "false"),
                                emptyMap(),
                                ""))
                .isEqualTo(-1);

        assertThat(
                        selector.findMatchingRule(
                                ImmutableMap.of(
                                        "country", "US", "missingExperimentalUnit", "false"),
                                emptyMap(),
                                "1"))
                .isEqualTo(2);
    }

    private static TestDefinition.Builder stubTestDefinition(
            final List<String> rules, final boolean unitless) {
        return TestDefinition.builder()
                .setTestType(TestType.ANONYMOUS_USER)
                .setSalt("")
                .setEnableUnitlessAllocations(unitless)
                .addBuckets(new TestBucket("active", 1, ""))
                .setAllocations(
                        rules.stream()
                                .map(rule -> new Allocation(rule, singletonList(new Range(1, 1.0))))
                                .collect(Collectors.toList()));
    }

    private static TestRangeSelector createTestRangeSelector(
            final TestDefinition definition, final IdentifierValidator id) {
        return new UnitlessAllocationRangeSelector(
                RuleEvaluator.createDefaultRuleEvaluator(emptyMap()),
                "dummy_test",
                ConsumableTestDefinition.fromTestDefinition(definition),
                id);
    }
}
