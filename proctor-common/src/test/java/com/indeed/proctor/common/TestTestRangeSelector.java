package com.indeed.proctor.common;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestDependency;
import com.indeed.proctor.common.model.TestType;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class TestTestRangeSelector {
    @Test
    public void testFindMatchingRule_matchingAllocations() {
        final TestRangeSelector selector =
                createTestRangeSelector(
                        stubTestDefinition(
                                        Arrays.asList(
                                                "country == 'US'",
                                                "country == 'US' && lang == 'en'",
                                                "lang == 'en'"))
                                .build());
        assertThat(
                        selector.findMatchingRule(
                                ImmutableMap.of("country", "US", "lang", "ja"), emptyMap(), ""))
                .isEqualTo(0);
        assertThat(
                        selector.findMatchingRule(
                                ImmutableMap.of("country", "US", "lang", "en"), emptyMap(), ""))
                .isEqualTo(0); // matching 0 and 1 and 2 and earliest one is chosen.
        assertThat(
                        selector.findMatchingRule(
                                ImmutableMap.of("country", "JP", "lang", "en"), emptyMap(), ""))
                .isEqualTo(2);
        assertThat(
                        selector.findMatchingRule(
                                ImmutableMap.of("country", "JP", "lang", "ja"), emptyMap(), ""))
                .isEqualTo(-1);
    }

    @Test
    public void testFindMatchingRule_testRule() {
        final TestRangeSelector selector =
                createTestRangeSelector(
                        stubTestDefinition(Arrays.asList("country == 'US'", ""))
                                .setRule("var == 1")
                                .build());
        assertThat(
                        selector.findMatchingRule(
                                ImmutableMap.of("country", "US", "var", 1), emptyMap(), ""))
                .isEqualTo(0);
        assertThat(
                        selector.findMatchingRule(
                                ImmutableMap.of("country", "US", "var", 0), emptyMap(), ""))
                .isEqualTo(-1);
        assertThat(
                        selector.findMatchingRule(
                                ImmutableMap.of("country", "JA", "var", 1), emptyMap(), ""))
                .isEqualTo(1);
    }

    @Test
    public void testFindMatchingRule_testDependency() {
        final TestRangeSelector selector =
                createTestRangeSelector(
                        stubTestDefinition(Arrays.asList("country == 'US'", ""))
                                .setDependsOn(new TestDependency("another_tst", 1))
                                .build());
        assertThat(
                        selector.findMatchingRule(
                                ImmutableMap.of("country", "US"),
                                ImmutableMap.of("another_tst", new TestBucket("active", 1, "")),
                                ""))
                .isEqualTo(0);

        assertThat(
                        selector.findMatchingRule(
                                ImmutableMap.of("country", "US"),
                                ImmutableMap.of("another_tst", new TestBucket("control", 0, "")),
                                ""))
                .isEqualTo(-1);

        assertThat(selector.findMatchingRule(ImmutableMap.of("country", "US"), emptyMap(), ""))
                .isEqualTo(-1);
    }

    @Test
    public void testFindMatchingRule_Error() {
        final TestRangeSelector selector =
                createTestRangeSelector(
                        stubTestDefinition(singletonList("trait.ad.country == 'US'"), false)
                                .build());
        final Map<String, Object> context = new HashMap<>();

        assertThat(selector.findMatchingRule(context, emptyMap(), "")).isEqualTo(-1);
    }

    @Test
    public void testFindMatchingRule_UnitlessAllocation_NotUnitless() {
        final TestRangeSelector selector =
                createTestRangeSelector(
                        stubTestDefinition(
                                        Arrays.asList("missingExperimentalUnit && country == 'US'"),
                                        false)
                                .build());
        assertThat(
                        selector.findMatchingRule(
                                ImmutableMap.of(
                                        "country", "US", "missingExperimentalUnit", "false"),
                                ImmutableMap.of("another_tst", new TestBucket("active", 1, "")),
                                ""))
                .isEqualTo(-1);

        assertThat(
                        selector.findMatchingRule(
                                ImmutableMap.of(
                                        "country", "JP", "missingExperimentalUnit", "false"),
                                ImmutableMap.of("another_tst", new TestBucket("control", 1, "")),
                                ""))
                .isEqualTo(-1);

        assertThat(
                        selector.findMatchingRule(
                                ImmutableMap.of(
                                        "country", "US", "missingExperimentalUnit", "false"),
                                emptyMap(),
                                ""))
                .isEqualTo(-1);
    }

    private static TestDefinition.Builder stubTestDefinition(final List<String> allocationRules) {
        return stubTestDefinition(allocationRules, false);
    }

    private static TestDefinition.Builder stubTestDefinition(
            final List<String> allocationRules, final boolean unitless) {
        return TestDefinition.builder()
                .setTestType(TestType.ANONYMOUS_USER)
                .setSalt("")
                .setEnableUnitlessAllocations(unitless)
                .addBuckets(new TestBucket("active", 1, ""))
                .setAllocations(
                        allocationRules.stream()
                                .map(rule -> new Allocation(rule, singletonList(new Range(1, 1.0))))
                                .collect(Collectors.toList()));
    }

    private static TestRangeSelector createTestRangeSelector(final TestDefinition definition) {
        return new TestRangeSelector(
                RuleEvaluator.createDefaultRuleEvaluator(emptyMap()),
                "dummy_test",
                ConsumableTestDefinition.fromTestDefinition(definition));
    }
}
