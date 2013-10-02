package com.indeed.proctor.common;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Map;

public class ProctorSpecification {
    @Nonnull
    private Map<String, String> providedContext = Collections.emptyMap();
    @Nonnull
    private Map<String, TestSpecification> tests = Collections.emptyMap();

    @Nonnull
    public Map<String, String> getProvidedContext() {
        return providedContext;
    }

    public void setProvidedContext(@Nonnull final Map<String, String> providedContext) {
        this.providedContext = providedContext;
    }

    @Nonnull
    public Map<String, TestSpecification> getTests() {
        return tests;
    }

    public void setTests(@Nonnull final Map<String, TestSpecification> tests) {
        this.tests = tests;
    }
}