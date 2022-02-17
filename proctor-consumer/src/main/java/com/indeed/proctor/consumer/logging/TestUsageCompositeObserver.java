package com.indeed.proctor.consumer.logging;

import java.util.Collection;

/**
 * A helper observer that composites multiple observers into one
 */
public class TestUsageCompositeObserver implements TestUsageObserver {
    private final TestUsageObserver[] observers;

    public TestUsageCompositeObserver(final TestUsageObserver... observers) {
        this.observers = observers;
    }

    @Override
    public void markUsedForToggling(final Collection<String> testNames) {
        for (final TestUsageObserver observer : observers) {
            observer.markUsedForToggling(testNames);
        }
    }

    @Override
    public void markUsedForToggling(final String testName) {
        for (final TestUsageObserver observer : observers) {
            observer.markUsedForToggling(testName);
        }
    }
}
