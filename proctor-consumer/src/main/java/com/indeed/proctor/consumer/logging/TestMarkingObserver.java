package com.indeed.proctor.consumer.logging;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.TestBucket;

import java.util.Collection;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Observer that collects tests and provides the collected tests as ProctorResult.
 * The purpose is to only log a subset of all known tests, based on their usage during a request.
 */
public class TestMarkingObserver implements TestUsageObserver {

    private final Set<String> testMarkers;

    private final ProctorResult originalResult;

    public TestMarkingObserver(final ProctorResult originalResult) {
        this.originalResult = originalResult;
        // Use ConcurrentHashMap to avoid ConcurrentModificationExceptions without locking on retrieval
        // the typical access should not be multithreaded anyway, this is just caution for unusual usage
        testMarkers = ConcurrentHashMap.newKeySet(originalResult.getBuckets().size());
    }

    @Override
    public void markUsedForToggling(final Collection<String> testNames) {
        testMarkers.addAll(testNames);
    }

    @Override
    public void markUsedForToggling(final String testName) {
        testMarkers.add(testName);
    }

    /**
     * to mark tests that should be logged, but might not have been used during the request for feature toggling
     */
    public void markTestsUsedForLogging(final Collection<String> testNames) {
        // currently no difference in effect
        markUsedForToggling(testNames);
    }

    /**
     * to mark tests that should be logged, but might not have been used during the request for feature toggling
     */
    public void markTestsUsedForLogging(final String testName) {
        // currently no difference in effect
        markUsedForToggling(testName);
    }

    /**
     * @return a copy of the ProctorResult passed to the constructor, containing only the marked tests
     */
    public ProctorResult asProctorResult() {
        return new ProctorResult(
                originalResult.getMatrixVersion(),
                // using Guava views to prevent object creation overhead
                Maps.filterKeys((SortedMap<String, TestBucket>)originalResult.getBuckets(), testMarkers::contains),
                Maps.filterKeys((SortedMap<String, Allocation>)originalResult.getAllocations(), testMarkers::contains),
                Maps.filterKeys(originalResult.getTestDefinitions(), testMarkers::contains),
                originalResult.getIdentifiers(),
                originalResult.getInputContext()
        );
    }
}
