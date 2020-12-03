package com.indeed.proctor.consumer.logging;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.indeed.proctor.common.ProctorResult;

import java.util.Collection;

/**
 * Observer that marks all used tests and provides the collected tests as ProctorResult
 */
public class TestExposureMarkingObserver implements TestUsageObserver {

    private final TestUsageMarker testUsageMarker;

    private final ProctorResult originalResult;

    public TestExposureMarkingObserver(final ProctorResult originalResult) {
        this.originalResult = originalResult;
        testUsageMarker = new TestUsageMarker(originalResult.getBuckets().size());
    }

    @Override
    public void markTestsUsed(final Collection<String> testNames) {
        testUsageMarker.markTests(testNames);
    }

    @Override
    public void markTestUsed(final String testName) {
        testUsageMarker.markTest(testName);
    }

    /**
     * @return a copy of the ProctorResult passed to the constructor, containing only the marked tests
     */
    public ProctorResult asProctorResult() {
        return new ProctorResult(
                originalResult.getMatrixVersion(),
                // using Guava views to prevent object creation overhead
                Maps.filterKeys(originalResult.getBuckets(), testUsageMarker::isMarked),
                Maps.filterKeys(originalResult.getAllocations(), testUsageMarker::isMarked),
                Maps.filterKeys(originalResult.getTestDefinitions(), testUsageMarker::isMarked),
                Sets.filter(originalResult.getDynamicallyLoadedTests(), testUsageMarker::isMarked));
    }
}
