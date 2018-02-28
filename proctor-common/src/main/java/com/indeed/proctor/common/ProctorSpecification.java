package com.indeed.proctor.common;

import com.indeed.proctor.common.dynamic.DynamicFilter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProctorSpecification {
    @Nonnull
    private Map<String, String> providedContext = Collections.emptyMap();
    @Nullable
    private Map<String, TestSpecification> tests = Collections.emptyMap();
    @Nonnull
    private List<DynamicFilter> dynamicFilters = Collections.emptyList();

    public ProctorSpecification() {
    }

    public ProctorSpecification(
            @Nonnull final Map<String, String> providedContext,
            @Nullable final Map<String, TestSpecification> tests,
            @Nonnull final List<DynamicFilter> dynamicFilters
    ) {
        this.providedContext = providedContext;
        this.tests = tests;
        this.dynamicFilters = dynamicFilters;
    }

    public ProctorSpecification(@Nonnull final ProctorSpecification other) {
        this.providedContext = new HashMap<>(other.providedContext);
        this.tests = new HashMap<>(other.tests);
        this.dynamicFilters = new ArrayList<>(other.dynamicFilters);
    }

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

    public List<DynamicFilter> getDynamicFilters() {
        return dynamicFilters;
    }

    public void setDynamicFilters(@Nonnull final List<DynamicFilter> dynamicFilters) {
        this.dynamicFilters = dynamicFilters;
    }
}