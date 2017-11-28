package com.indeed.proctor.common.model;

import javax.annotation.Nullable;

/**
 * TODO: Find more appropriate name
 */
public class ChooseResult {
    @Nullable
    final TestBucket testBucket;

    @Nullable
    final Allocation allocation;

    public static ChooseResult EMPTY = new ChooseResult(null, null);

    public ChooseResult(@Nullable final TestBucket testBucket,
                        @Nullable final Allocation allocation) {
        this.testBucket = testBucket;
        this.allocation = allocation;
    }

    @Nullable
    public TestBucket getTestBucket() {
        return testBucket;
    }

    @Nullable
    public Allocation getAllocation() {
        return allocation;
    }
}
