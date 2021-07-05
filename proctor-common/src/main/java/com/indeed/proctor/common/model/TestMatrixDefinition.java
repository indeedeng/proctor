package com.indeed.proctor.common.model;

import com.google.common.collect.ImmutableMap;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

public class TestMatrixDefinition {
    @Nonnull
    private Map<String, TestDefinition> tests = Collections.emptyMap();

    public TestMatrixDefinition() { /* intentionally empty */ }

    public TestMatrixDefinition(@Nonnull final Map<String, TestDefinition> tests) {
        this.tests = tests;
    }

    public TestMatrixDefinition(@Nonnull final TestMatrixDefinition testMatrixDefinition) {
        this.tests = ImmutableMap.copyOf(testMatrixDefinition.tests);
    }

    @Nonnull
    public Map<String, TestDefinition> getTests() {
        return tests;
    }

    public void setTests(@Nonnull final Map<String, TestDefinition> tests) {
        this.tests = tests;
    }
}
