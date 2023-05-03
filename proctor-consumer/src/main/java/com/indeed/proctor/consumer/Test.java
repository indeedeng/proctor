package com.indeed.proctor.consumer;

public interface Test {
    /**
     * @return the name of the test
     */
    String getName();

    /**
     * @return the fallback bucket value
     */
    int getFallbackValue();
}
