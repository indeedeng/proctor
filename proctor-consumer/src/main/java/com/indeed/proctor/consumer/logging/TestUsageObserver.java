package com.indeed.proctor.consumer.logging;

import java.util.Collection;

public interface TestUsageObserver {

    /**
     * to be called by AbstractGroups (or custom code) when proctor tests value is retrieved by a method call for feature toggling.
     *
     * The intention is to help distinguish between proctor tests that were used during a request and those that were not.
     */
    void markTestsUsed(Collection<String> testNames);

    /**
     * to be called by AbstractGroups (or custom code) when a proctor test value is retrieved by a method call.
     *
     * The intention is to help distinguish between proctor tests that were used during a request and those that were not.
     */
    void markTestUsed(String testName);
}
