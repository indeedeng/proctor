package com.indeed.proctor.webapp.util;

import com.google.common.base.Strings;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.TestDefinition;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class TestSearchUtil {
    public static boolean matchTestName(final String testName, final String lowerQuery) {
        return testName.toLowerCase().contains(lowerQuery);
    }

    public static boolean matchDescription(
            final TestDefinition definition, final String lowerQuery) {
        return Strings.nullToEmpty(definition.getDescription()).toLowerCase().contains(lowerQuery);
    }

    public static boolean matchRule(final TestDefinition definition, final String lowerQuery) {
        return Strings.nullToEmpty(definition.getRule()).toLowerCase().contains(lowerQuery)
                || definition.getAllocations().stream()
                        .map(allocation -> Strings.nullToEmpty(allocation.getRule()).toLowerCase())
                        .anyMatch(rule -> rule.contains(lowerQuery));
    }

    public static boolean matchBucket(final TestDefinition definition, final String lowerQuery) {
        return definition.getBuckets().stream()
                .map(testBucket -> testBucket.getName().toLowerCase())
                .anyMatch(name -> name.contains(lowerQuery));
    }

    public static boolean matchBucketDescription(
            final TestDefinition definition, final String lowerQuery) {
        return definition.getBuckets().stream()
                .map(testBucket -> Strings.nullToEmpty(testBucket.getDescription()).toLowerCase())
                .anyMatch(description -> description.contains(lowerQuery));
    }

    public static boolean matchTestType(final TestDefinition definition, final String lowerQuery) {
        return definition.getTestType().toString().toLowerCase().contains(lowerQuery);
    }

    public static boolean matchSalt(final TestDefinition definition, final String lowerQuery) {
        return definition.getSalt().toLowerCase().contains(lowerQuery);
    }

    public static boolean matchAll(
            final String testName, final TestDefinition definition, final String lowerQuery) {
        return matchTestName(testName, lowerQuery)
                || matchDescription(definition, lowerQuery)
                || matchRule(definition, lowerQuery)
                || matchBucket(definition, lowerQuery)
                || matchBucketDescription(definition, lowerQuery)
                || matchTestType(definition, lowerQuery)
                || matchMetaTags(definition, lowerQuery)
                || matchSalt(definition, lowerQuery);
    }

    public static boolean matchActiveAllocation(final List<Allocation> allocations) {
        return allocations.stream()
                .anyMatch(
                        allocation ->
                                allocation.getRanges().stream()
                                        .allMatch(range -> range.getLength() < 1));
    }

    public static boolean matchMetaTags(final TestDefinition definition, final String lowerQuery) {
        return Strings.isNullOrEmpty(lowerQuery)
                || definition.getMetaTags().stream()
                        .map(metaTag -> metaTag.toLowerCase())
                        .anyMatch(name -> name.contains(lowerQuery));
    }

    /**
     * @param testNames test names to sort at first
     * @return a comparator of an entry of map
     */
    public static Comparator<String> givenSetFirstComparator(final Set<String> testNames) {
        return Comparator.<String, Boolean>comparing(testNames::contains).reversed();
    }
}
