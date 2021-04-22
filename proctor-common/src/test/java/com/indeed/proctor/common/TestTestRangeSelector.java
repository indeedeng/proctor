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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class TestTestRangeSelector {
    @Test
    public void testFindMatchingRule_matchingAllocations() {
        final TestRangeSelector selector = createTestRangeSelector(
                stubTestDefinition(Arrays.asList("country == 'US'", "country == 'US' && lang == 'en'", "lang == 'en'"))
                        .build()
        );
        assertThat(selector.findMatchingRule(ImmutableMap.of("country", "US", "lang", "ja")))
                .isEqualTo(0);
        assertThat(selector.findMatchingRule(ImmutableMap.of("country", "US", "lang", "en")))
                .isEqualTo(0); // matching 0 and 1 and 2 and earliest one is chosen.
        assertThat(selector.findMatchingRule(ImmutableMap.of("country", "JP", "lang", "en")))
                .isEqualTo(2);
        assertThat(selector.findMatchingRule(ImmutableMap.of("country", "JP", "lang", "ja")))
                .isEqualTo(-1);
    }

    @Test
    public void testFindMatchingRule_testRule() {
        final TestRangeSelector selector = createTestRangeSelector(
                stubTestDefinition(Arrays.asList("country == 'US'", ""))
                        .setRule("var == 1")
                        .build()
        );
        assertThat(selector.findMatchingRule(ImmutableMap.of("country", "US", "var", 1)))
                .isEqualTo(0);
        assertThat(selector.findMatchingRule(ImmutableMap.of("country", "US", "var", 0)))
                .isEqualTo(-1);
        assertThat(selector.findMatchingRule(ImmutableMap.of("country", "JA", "var", 1)))
                .isEqualTo(1);
    }

    private static TestDefinition.Builder stubTestDefinition(final List<String> rules) {
        return TestDefinition.builder()
                .setTestType(TestType.ANONYMOUS_USER)
                .setSalt("")
                .setBuckets(singletonList(new TestBucket("active", 1, "")))
                .setAllocations(
                        rules.stream()
                                .map(rule -> new Allocation(rule, singletonList(new Range(1, 1.0))))
                                .collect(Collectors.toList())
                );
    }

    private static TestRangeSelector createTestRangeSelector(final TestDefinition definition) {
        return new TestRangeSelector(
                RuleEvaluator.createDefaultRuleEvaluator(Collections.emptyMap()),
                "dummy_test",
                ConsumableTestDefinition.fromTestDefinition(definition)
        );
    }
}
