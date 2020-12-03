package com.indeed.proctor.consumer.logging;

import com.indeed.proctor.common.ProctorResult;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Helper class to mark testNames when used.
 */
@ThreadSafe
public class TestUsageMarker {

    /**
     * Using AtomicBoolean Map to stay thread-safe without synchronization penalty.
     * Thread-safety is only relevant for unusual multi-threaded usages of this class.
     */
    private final Map<String, AtomicBoolean> testMarkers;

    public TestUsageMarker(final ProctorResult proctorResult) {
        testMarkers = proctorResult.getBuckets().keySet().stream()
                .collect(Collectors.toMap(k -> k, k -> new AtomicBoolean(false)));
    }

    public void markTests(final Collection<String> testNames) {
        for (final String testName: testNames) {
            final AtomicBoolean marker = testMarkers.get(testName);
            if (marker != null) {
                marker.set(true);
            }
        }
    }

    public boolean isMarked(final String testName) {
        final AtomicBoolean atomicBoolean = testMarkers.get(testName);
        if (atomicBoolean == null) {
            return false;
        }
        return atomicBoolean.get();
    }
}
