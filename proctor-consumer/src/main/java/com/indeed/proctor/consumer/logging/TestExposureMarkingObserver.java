package com.indeed.proctor.consumer.logging;

import com.indeed.proctor.common.ProctorResult;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Observer that marks all used tests and provides the collected tests as ProctorResult
 */
@ThreadSafe
public class TestExposureMarkingObserver implements TestUsageObserver {

    private final TestUsageMarker testUsageMarker;

    private final ProctorResult originalResult;

    public TestExposureMarkingObserver(final ProctorResult originalResult) {
        this.originalResult = originalResult;
        testUsageMarker = new TestUsageMarker(originalResult);
    }

    @Override
    public void testsUsed(final Collection<String> testName) {
        testUsageMarker.markTests(testName);
    }

    public ProctorResult asProctorResult() {
        final Predicate<Map.Entry<String, ?>> observedTestnamesFilter = entry -> testUsageMarker.isMarked(entry.getKey());
        return new ProctorResult(
                originalResult.getMatrixVersion(),
                filteredMap(originalResult.getBuckets(), observedTestnamesFilter),
                filteredMap(originalResult.getAllocations(), observedTestnamesFilter),
                filteredMap(originalResult.getTestDefinitions(), observedTestnamesFilter),
                originalResult.getDynamicallyLoadedTests());
    }

    private static <T> Map<String, T> filteredMap(
            final Map<String, T> map,
            final Predicate<Map.Entry<String, ?>> observedTestnamesFilter) {
        return map.entrySet()
                .stream()
                .filter(observedTestnamesFilter)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
