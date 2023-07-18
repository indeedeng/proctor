package com.indeed.proctor.common.el;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRuleAnalyzer {
    @Test
    public void testIsReferencedInString() {
        final String variableName = "application";

        assertThat(RuleAnalyzer.getReferencedVariables("${application == 'ExampleApplication'}"))
                .contains(variableName);
        assertThat(
                        RuleAnalyzer.getReferencedVariables(
                                "${foo == bar && application == 'ExampleApplication'}"))
                .contains("foo", "bar", variableName);
        assertThat(
                        RuleAnalyzer.getReferencedVariables(
                                "${ua.name == 'ie9' && application == 'ExampleApplication'}"))
                .contains(variableName);
        assertThat(
                        RuleAnalyzer.getReferencedVariables(
                                "${false && application == 'ExampleApplication'}"))
                .contains(variableName);
        assertThat(
                        RuleAnalyzer.getReferencedVariables(
                                "${nested.array[1] == 7 && application == 'ExampleApplication'}"))
                .contains(variableName);
        assertThat(RuleAnalyzer.getReferencedVariables("${nested.array[1] == 7 && foo == bar}"))
                .doesNotContain(variableName);
        assertThat(
                        RuleAnalyzer.getReferencedVariables(
                                "${nested.array[1] == 'application' && foo == bar}"))
                .doesNotContain(variableName);
        assertThat(RuleAnalyzer.getReferencedVariables("${null}")).isEmpty();
    }
}
