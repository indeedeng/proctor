package com.indeed.proctor.common.dynamic;

import com.google.common.collect.ImmutableSet;
import com.indeed.proctor.common.model.ConsumableTestDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DynamicFilters {
    private final List<DynamicFilter> filters = new ArrayList<>();

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

    public boolean add(final DynamicFilter dynamicFilter) {
        return filters.add(dynamicFilter);
    }

    public boolean addAll(final Collection<? extends DynamicFilter> c) {
        return filters.addAll(c);
    }

    public boolean addAll(final DynamicFilters other) {
        return filters.addAll(other.filters);
    }
}
