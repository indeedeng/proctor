package com.indeed.proctor.webapp.model;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.common.TestSpecification;
import com.indeed.proctor.common.model.ConsumableTestDefinition;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * a immutable set of proctor specifications (usually, from a single app)
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
    public Set<ProctorSpecification> asSet() {
        return specifications;
    }

    /**
     * Returns union of required tests in specifications
     */
    public Map<String, Set<TestSpecification>> getRequiredTests(
            final Set<String> definedTests
    ) {
        final Map<String, Set<TestSpecification>> result = new HashMap<>();
        for (final ProctorSpecification specification : specifications) {
            for (final String name : definedTests) {
                final TestSpecification spec = specification.getTests().get(name);
                if (spec != null) {
                    result.computeIfAbsent(name, ignored -> new HashSet<>())
                            .add(spec);
                }
            }
        }
        return result;
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
