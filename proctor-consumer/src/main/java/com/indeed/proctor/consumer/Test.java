package com.indeed.proctor.consumer;

public interface Test {
    /**
     * @returns the name of the test
     * */
    String getName();

    /**
     *
     * @return the fallback bucket value
     */
    int getFallbackValue();
}
