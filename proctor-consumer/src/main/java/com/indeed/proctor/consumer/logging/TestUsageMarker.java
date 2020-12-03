package com.indeed.proctor.consumer.logging;

import com.google.common.collect.MapMaker;
import com.indeed.proctor.common.ProctorResult;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Helper class to mark testNames when used.
 */
public class TestUsageMarker {

    private final Set<String> testMarkers;

    public TestUsageMarker(final ProctorResult proctorResult) {
        // using MapMaker to get the equivalent of ConcurrentHashMap with less thread-contention
        testMarkers = Collections.newSetFromMap(new MapMaker()
                .initialCapacity(proctorResult.getBuckets().size())
                .makeMap());
    }

    public void markTests(final Collection<String> testNames) {
        testMarkers.addAll(testNames);
    }

    public void markTest(final String testName) {
        testMarkers.add(testName);
    }

    public boolean isMarked(final String testName) {
        return testMarkers.contains(testName);
    }
}
