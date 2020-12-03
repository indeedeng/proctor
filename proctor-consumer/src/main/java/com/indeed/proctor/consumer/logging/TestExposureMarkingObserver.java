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
    public void testsUsed(final Collection<String> testNames) {
        testUsageMarker.markTests(testNames);
    }

    @Override
    public void testUsed(final String testName) {
        testUsageMarker.markTest(testName);
    }

    public ProctorResult asProctorResult() {
        return new ProctorResult(
                originalResult.getMatrixVersion(),
                filteredMap(originalResult.getBuckets(), testUsageMarker::isMarked),
                filteredMap(originalResult.getAllocations(), testUsageMarker::isMarked),
                filteredMap(originalResult.getTestDefinitions(), testUsageMarker::isMarked),
                originalResult.getDynamicallyLoadedTests().stream()
                        .filter(testUsageMarker::isMarked)
                        .collect(Collectors.toSet()));
    }

    private static <T> Map<String, T> filteredMap(
            final Map<String, T> map,
            final Predicate<String> observedTestnamesFilter) {
        return map.entrySet()
                .stream()
                .filter(e -> observedTestnamesFilter.test(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
