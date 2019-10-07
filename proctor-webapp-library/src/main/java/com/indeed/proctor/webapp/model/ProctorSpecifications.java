package com.indeed.proctor.webapp.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.common.TestSpecification;
import com.indeed.proctor.common.model.ConsumableTestDefinition;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * a immutable set of proctor specifications (usually, from a single app)
 * <p>
 * This model allows multiple specifications because one application
 * may initiate more than one Proctor object with different specifications
 * for different usages/contexts
 */
public class ProctorSpecifications {
    private final Set<ProctorSpecification> specifications;

    public ProctorSpecifications(
            final Iterable<ProctorSpecification> specifications
    ) {
        this.specifications = ImmutableSet.copyOf(specifications);
    }

    /**
     * Provide a view of set object of specifications
     */
    @JsonValue
    public Set<ProctorSpecification> asSet() {
        return specifications;
    }

    /**
     * For each test name, returns the specifications for which it is a required test
     */
    public Map<String, Set<TestSpecification>> getRequiredTests(
            final Set<String> definedTests
    ) {
        final ImmutableMap.Builder<String, Set<TestSpecification>> builder
                = ImmutableMap.builder();
        for (final String testName : definedTests) {
            final Set<TestSpecification> specsForTest = specifications.stream()
                    .map(ProctorSpecification::getTests)
                    .map(x -> x.get(testName))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!specsForTest.isEmpty()) {
                builder.put(testName, specsForTest);
            }
        }
        return builder.build();
    }

    /**
     * Returns a set of proctor test names that are resolved
     * by a dynamic filter defined in one of specifications.
     */
    public Set<String> getDynamicTests(
            final Map<String, ConsumableTestDefinition> definedTests
    ) {
        return this.specifications.stream()
                .map(s -> s.getDynamicFilters()
                        .determineTests(definedTests, s.getTests().keySet())
                )
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    /**
     * Returns a set of proctor test names
     * that are resolved by a test specification or dynamic filters
     */
    public Set<String> getResolvedTests(
            final Map<String, ConsumableTestDefinition> definedTests
    ) {
        final Set<String> requiredTests =
                getRequiredTests(definedTests.keySet()).keySet();
        final Set<String> dynamicTests = getDynamicTests(definedTests);
        return Sets.union(requiredTests, dynamicTests);
    }
}
