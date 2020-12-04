package com.indeed.proctor.consumer.logging;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.indeed.proctor.common.ProctorResult;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Observer that marks all used tests and provides the collected tests as ProctorResult
 */
public class TestExposureMarkingObserver implements TestUsageObserver {

    private final Set<String> testMarkers;

    private final ProctorResult originalResult;

    public TestExposureMarkingObserver(final ProctorResult originalResult) {
        this.originalResult = originalResult;
        // Use ConcurrentHashMap to avoid ConcurrentModificationExceptions without locking on retrieval
        // the typical access should not be multithreaded anyway, this is just caution for unusual usage
        testMarkers = ConcurrentHashMap.newKeySet(originalResult.getBuckets().size());
    }

    @Override
    public void markTestsUsed(final Collection<String> testNames) {
        testMarkers.addAll(testNames);
    }

    @Override
    public void markTestUsed(final String testName) {
        testMarkers.add(testName);
    }

    /**
     * @return a copy of the ProctorResult passed to the constructor, containing only the marked tests
     */
    public ProctorResult asProctorResult() {
        return new ProctorResult(
                originalResult.getMatrixVersion(),
                // using Guava views to prevent object creation overhead
                Maps.filterKeys(originalResult.getBuckets(), testMarkers::contains),
                Maps.filterKeys(originalResult.getAllocations(), testMarkers::contains),
                Maps.filterKeys(originalResult.getTestDefinitions(), testMarkers::contains),
                Sets.filter(originalResult.getDynamicallyLoadedTests(), testMarkers::contains));
    }
}
