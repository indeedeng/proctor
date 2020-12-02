package com.indeed.proctor.consumer.logging;

import java.util.Collection;

public interface TestUsageObserver {

    /**
     * notify the event where the code has requested the resolved value for this test,
     * and the given value was determined, and the given Bucket represent the value
     */
    void testsUsed(Collection<String> testNames);
}
