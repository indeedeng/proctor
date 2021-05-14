package com.indeed.proctor.common;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestDependency;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A collection of utility functions for test dependencies
 */
public class TestDependencies {
    private TestDependencies() {
    }

    /**
     * Returns test names and reasons of all tests with invalid dependency relationship.
     * It's invalid when a test directly or indirectly depends on
     * <ul>
     *     <li>an unknown test, or</li>
     *     <li>a test with circular dependency (depending on itself), or</li>
     *     <li>a test with a different test type, or</li>
     *     <li>a test with the same salt, or</li>
     *     <li>a bucket undefined in the test</li>
     * </ul>
     *
     * It's expected to be used in test matrix loading time to filter out invalid test definitions.
     */
    public static Map<String, String> validateDependenciesAndReturnReasons(
            final Map<String, ConsumableTestDefinition> testDefinitions
    ) {
        final Set<String> traversed = new HashSet<>();
        final Map<String, String> errorReasonMap = new HashMap<>();
        for (final String testName : traverseDependencyTreesBFS(testDefinitions,
                testsWithoutDependencyOrUnknownDependency(testDefinitions))) {
            final TestDependency dependsOn = testDefinitions.get(testName).getDependsOn();
            final boolean isParentInvalid = (dependsOn != null)
                    && errorReasonMap.containsKey(dependsOn.getTestName());
            final Optional<String> errorReason = validateDependencyAndReturnReason(
                    testName,
                    testDefinitions.get(testName),
                    testDefinitions
            );

            traversed.add(testName);
            if (errorReason.isPresent()) {
                errorReasonMap.put(testName, errorReason.get());
            } else if (isParentInvalid) {
                errorReasonMap.put(testName, "A test " + testName + " directly or indirectly depends on an invalid test");
            }
        }

        for (final String testName : testDefinitions.keySet()) {
            if (!traversed.contains(testName)) {
                errorReasonMap.put(testName, "A test " + testName + " depends on a test with circular dependency");
            }
        }

        return ImmutableMap.copyOf(errorReasonMap);
    }

    /**
     * Validate a direct dependency relationship of the given test
     * and returns reason if the dependency is invalid.
     *
     * It's expected to be used by Proctor Webapp to validate a test modification or deletion.
     */
    public static Optional<String> validateDependencyAndReturnReason(
            final String testName,
            final ConsumableTestDefinition definition,
            final Map<String, ConsumableTestDefinition> testDefinitions
    ) {
        final TestDependency dependsOn = definition.getDependsOn();
        if (dependsOn == null) {
            return Optional.empty();
        }

        final String parentName = dependsOn.getTestName();
        final ConsumableTestDefinition parentDefinition = testDefinitions.get(parentName);
        if (parentDefinition == null) {
            return Optional.of("A test " + testName + " depends on an unknown or incompatible test "
                    + dependsOn.getTestName());
        }

        /*
          Using different test type could cause bias.
         */
        if (!definition.getTestType().equals(parentDefinition.getTestType())) {
            return Optional.of("A test " + testName + " depends on "
                    + parentName + " with different test type: expected "
                    + definition.getTestType() + " but " + parentDefinition.getTestType());
        }

        /*
          Using the same salt will cause confusing behavior
          E.g. If test X and Y has 50% control / 50% active and shares the same salt,
          Y will be 100% active when Y depends on X's active.
         */
        if (Objects.equals(definition.getSalt(), parentDefinition.getSalt())) {
            // FIXME: This has to check indirect parents to cover more invalid cases
            return Optional.of("A test " + testName + " depends on "
                    + parentName + " with the same salt: "
                    + parentDefinition.getSalt());
        }

        /*
          Depending on negative bucket value is prohibited to avoid potential issues with fallback or logging behavior
         */
        if (dependsOn.getBucketValue() < 0) {
            return Optional.of(
                    "A test " + testName + " depends on negative bucket value "
                            + dependsOn.getBucketValue() + " of "
                            + dependsOn.getTestName()
            );
        }

        final boolean isBucketDefined = parentDefinition.getBuckets().stream()
                .anyMatch(x -> x.getValue() == dependsOn.getBucketValue());
        if (!isBucketDefined) {
            return Optional.of("A test " + testName + " depends on "
                    + "an undefined bucket " + dependsOn.getBucketValue());
        }

        return Optional.empty();
    }

    /**
     * Returns a list of all test names in arbitrary order satisfying the following condition
     * - If test X depends on test Y, test Y comes earlier than X in the list.
     *
     * It's expected to be used in test matrix loading time to precompute evaluation order.
     *
     * @throws IllegalArgumentException when it detects circular dependency or dependency to unknown test
     */
    public static List<String> determineEvaluationOrder(
            final Map<String, ConsumableTestDefinition> testDefinitions
    ) {
        final List<String> evaluationOrder = traverseDependencyTreesBFS(testDefinitions, testsWithoutDependency(testDefinitions));
        if (evaluationOrder.size() != testDefinitions.size()) {
            throw new IllegalArgumentException("Detected invalid dependency links. Unable to determine order");
        }
        return evaluationOrder;
    }

    /**
     * Returns maximum length (number of edges) of dependency chains through the given test.
     *
     * It's expected to be used by Proctor Webapp to limit the allowed depth in edit.
     */
    public static int computeMaximumDependencyChains(
            final Map<String, ConsumableTestDefinition> testDefinitions,
            final String testName
    ) {
        final Map<String, Integer> depthMap = new HashMap<>();
        for (final String name : traverseDependencyTreesBFS(testDefinitions, ImmutableSet.of(testName))) {
            final ConsumableTestDefinition definition = testDefinitions.get(name);
            if (testName.equals(name) || (definition.getDependsOn() == null)) {
                depthMap.put(name, 0);
            } else {
                depthMap.put(name, depthMap.get(definition.getDependsOn().getTestName()) + 1);
            }
        }
        final int childDepth = depthMap.values().stream().max(Comparator.naturalOrder()).orElse(0);
        final int parentNums = computeTransitiveDependencies(testDefinitions, ImmutableSet.of(testName)).size() - 1;
        return parentNums + childDepth;
    }

    /**
     * Returns all test names required to evaluate all the given tests.
     * It runs in linear time to the size of response instead of all tests.
     *
     * @throws IllegalArgumentException when it detects dependency on an unknown name
     */
    public static Set<String> computeTransitiveDependencies(
            final Map<String, ConsumableTestDefinition> testDefinitions,
            final Set<String> testNames
    ) {
        testNames.stream().filter(testName -> !testDefinitions.containsKey(testName)).findAny()
                .ifPresent((testName) -> {
                    throw new IllegalArgumentException("BUG: unknown test name " + testName + " is given");
                });

        final Queue<String> testNameQueue = new ArrayDeque<>(testNames);
        final Set<String> transitiveDependencies = new HashSet<>(testNames);

        while (!testNameQueue.isEmpty()) {
            final String testName = testNameQueue.poll();
            final ConsumableTestDefinition testDefinition = testDefinitions.get(testName);
            if (testDefinition == null) {
                throw new IllegalArgumentException("Detected dependency on an unknown test " + testName);
            }

            final TestDependency dependsOn = testDefinition.getDependsOn();
            if (dependsOn == null) {
                continue;
            }

            final String parentTestName = dependsOn.getTestName();
            if (transitiveDependencies.contains(parentTestName)) {
                continue;
            }

            testNameQueue.add(parentTestName);
            transitiveDependencies.add(parentTestName);
        }

        return ImmutableSet.copyOf(transitiveDependencies);
    }

    /**
     * Returns visiting order of proctor tests by breadth-first search
     * in a graph where a edge from X to Y is added if Y depends on X
     * starting from given tests
     */
    private static List<String> traverseDependencyTreesBFS(
            final Map<String, ConsumableTestDefinition> testDefinitions,
            final Set<String> sourceTestNames
    ) {
        sourceTestNames.stream().filter(testName -> !testDefinitions.containsKey(testName)).findAny()
                .ifPresent((testName) -> {
                    throw new IllegalArgumentException("BUG: unknown test name " + testName + " is given");
                });

        final Multimap<String, String> parentToChildrenMap = HashMultimap.create();
        testDefinitions.forEach((testName, definition) -> {
            if (definition.getDependsOn() != null) {
                parentToChildrenMap.put(definition.getDependsOn().getTestName(), testName);
            }
        });

        final Queue<String> testNameQueue = new ArrayDeque<>(sourceTestNames);
        final Set<String> testNameSet = new HashSet<>();
        final ImmutableList.Builder<String> builder = ImmutableList.builder();
        while (!testNameQueue.isEmpty()) {
            final String testName = testNameQueue.poll();
            if (!testNameSet.add(testName)) {
                throw new IllegalArgumentException("BUG: circular dependency detect around " + testName);
            }
            builder.add(testName);
            testNameQueue.addAll(parentToChildrenMap.get(testName));
        }

        return builder.build();
    }

    private static Set<String> testsWithoutDependency(
            final Map<String, ConsumableTestDefinition> testDefinitions
    ) {
        return testDefinitions.entrySet().stream()
                .filter(entry -> entry.getValue().getDependsOn() == null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private static Set<String> testsWithoutDependencyOrUnknownDependency(
            final Map<String, ConsumableTestDefinition> testDefinitions
    ) {
        return testDefinitions.entrySet().stream()
                .filter(entry -> (entry.getValue().getDependsOn() == null) ||
                        !testDefinitions.containsKey(entry.getValue().getDependsOn().getTestName()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
