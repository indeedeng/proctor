package com.indeed.proctor.common.model;

import com.google.common.collect.ImmutableMap;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public class TestMatrixDefinition {
    @Nonnull private Map<String, TestDefinition> tests = Collections.emptyMap();

    public TestMatrixDefinition() {
        /* intentionally empty */
    }

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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        final TestMatrixDefinition that = (TestMatrixDefinition) o;
        return tests.equals(that.tests);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tests);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TestMatrixDefinition.class.getSimpleName() + "[", "]")
                .add("tests=" + tests)
                .toString();
    }
}
