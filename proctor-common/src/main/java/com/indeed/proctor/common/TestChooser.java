package com.indeed.proctor.common;

import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestBucket;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.util.Map;

interface TestChooser<IdentifierType> {

    void printTestBuckets(@Nonnull final PrintWriter writer);

    @Nullable
    TestBucket getTestBucket(final int value);

    @Nonnull
    String[] getRules();

    @Nonnull
    ConsumableTestDefinition getTestDefinition();

    @Nonnull
    String getTestName();

    @Nonnull
    TestChooser.Result choose(@Nullable IdentifierType identifier, @Nonnull Map<String, Object> values, @Nonnull Map<String, TestBucket> testGroups);

    /**
     * Models a result of an assigned bucket and allocation by {@code TestChooser}.
     */
    class Result {
        /**
         * Empty result for the case when all allocation rules aren't matched to a context
         */
        public static final Result EMPTY = new Result(null, null);

        @Nullable
        private final TestBucket testBucket;

        @Nullable
        private final Allocation allocation;

        Result(@Nullable final TestBucket testBucket,
               @Nullable final Allocation allocation) {
            this.testBucket = testBucket;
            this.allocation = allocation;
        }

        /**
         * Returns a chosen test in {@code TestChooser}. Returns null if any bucket isn't chosen.
         */
        @Nullable
        public TestBucket getTestBucket() {
            return testBucket;
        }

        /**
         * Returns a matched allocation in {@code TestChooser}. Returns null if any rules isn't matched.
         */
        @Nullable
        public Allocation getAllocation() {
            return allocation;
        }
    }
}
