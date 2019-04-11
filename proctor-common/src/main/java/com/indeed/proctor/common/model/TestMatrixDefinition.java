package com.indeed.proctor.common.model;

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

    @Nonnull
    public Map<String, TestDefinition> getTests() {
        return tests;
    }

    public void setTests(@Nonnull final Map<String, TestDefinition> tests) {
        this.tests = tests;
    }
}
