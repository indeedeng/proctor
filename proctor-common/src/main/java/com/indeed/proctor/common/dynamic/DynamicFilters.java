package com.indeed.proctor.common.dynamic;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.indeed.proctor.common.model.ConsumableTestDefinition;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DynamicFilters {
    private final List<DynamicFilter> filters;

    public DynamicFilters() {
        this.filters = Collections.emptyList();
    }

    public DynamicFilters(final Collection<DynamicFilter> filters) {
        this.filters = ImmutableList.copyOf(filters);
    }

    public Set<String> determineTests(
            final Map<String, ConsumableTestDefinition> definedTests,
            final Set<String> requiredTests
    ) {
        final ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (final Map.Entry<String, ConsumableTestDefinition> entry : definedTests.entrySet()) {
            final String testName = entry.getKey();
            final ConsumableTestDefinition testDefinition = entry.getValue();
            if ((testDefinition != null) && !requiredTests.contains(testName)) {
                for (final DynamicFilter filter : filters) {
                    if (filter.matches(testName, testDefinition)) {
                        builder.add(testName);
                    }
                }
            }
        }
        return builder.build();
    }
}
