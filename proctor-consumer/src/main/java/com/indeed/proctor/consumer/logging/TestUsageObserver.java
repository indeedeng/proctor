package com.indeed.proctor.consumer.logging;

import java.util.Collection;

/**
 * Interface for classes to be notified about test usages within AbstractGroups methods for feature
 * toggling.
 */
public interface TestUsageObserver {

    void markUsedForToggling(Collection<String> testNames);

    void markUsedForToggling(String testName);
}
