package com.indeed.proctor.common;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

public class ProctorSpecification {
    @Nonnull
    private Map<String, String> providedContext = Collections.emptyMap();
    @Nullable
    private Map<String, TestSpecification> tests = Collections.emptyMap();

    @Nonnull
    public Map<String, String> getProvidedContext() {
        return providedContext;
    }

    public void setProvidedContext(@Nonnull final Map<String, String> providedContext) {
        this.providedContext = providedContext;
    }

    /**
     * @return the test specification for each named test.
     * If null, tests is intentionally omitted. All tests in the test matrix should be considered.
     */
    @Nullable
    public Map<String, TestSpecification> getTests() {
        return tests;
    }

    public void setTests(@Nullable final Map<String, TestSpecification> tests) {
        this.tests = tests;
    }
}