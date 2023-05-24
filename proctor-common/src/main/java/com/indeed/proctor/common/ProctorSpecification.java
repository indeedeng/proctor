package com.indeed.proctor.common;

import com.indeed.proctor.common.dynamic.DynamicFilters;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ProctorSpecification {
    @Nonnull private Map<String, String> providedContext;
    @Nonnull private Map<String, TestSpecification> tests;
    @Nonnull private DynamicFilters dynamicFilters;

    public ProctorSpecification() {
        this(Collections.emptyMap(), Collections.emptyMap(), new DynamicFilters());
    }

    public ProctorSpecification(
            @Nonnull final Map<String, String> providedContext,
            @Nonnull final Map<String, TestSpecification> tests,
            @Nonnull final DynamicFilters dynamicFilters) {
        this.providedContext = Objects.requireNonNull(providedContext);
        this.tests = Objects.requireNonNull(tests);
        this.dynamicFilters = Objects.requireNonNull(dynamicFilters);
    }

    public ProctorSpecification(@Nonnull final ProctorSpecification other) {
        this(other.providedContext, new HashMap<>(other.tests), other.dynamicFilters);
    }

    @Nonnull
    public Map<String, String> getProvidedContext() {
        return providedContext;
    }

    public void setProvidedContext(@Nonnull final Map<String, String> providedContext) {
        this.providedContext = providedContext;
    }

    /** @return the test specification for each named test. */
    @Nonnull
    public Map<String, TestSpecification> getTests() {
        return tests;
    }

    public void setTests(@Nonnull final Map<String, TestSpecification> tests) {
        this.tests = Objects.requireNonNull(tests);
    }

    @Nonnull
    public DynamicFilters getDynamicFilters() {
        return dynamicFilters;
    }

    public void setDynamicFilters(@Nonnull final DynamicFilters dynamicFilters) {
        this.dynamicFilters = dynamicFilters;
    }
}
