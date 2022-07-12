package com.indeed.proctor.common;

import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import org.apache.logging.log4j.core.config.plugins.util.ResolverUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

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

    /**
     * Do not directly call this outside this interface.
     * We should call {@link #choose(Object, Map, Map, ForceGroupsOptions)}, instead.
     */
    @Nonnull
    TestChooser.Result chooseInternal(
            @Nullable IdentifierType identifier,
            @Nonnull Map<String, Object> values,
            @Nonnull Map<String, TestBucket> testGroups
    );

    @Nonnull
    default TestChooser.Result choose(
            @Nullable IdentifierType identifier,
            @Nonnull Map<String, Object> values,
            @Nonnull Map<String, TestBucket> testGroups,
            @Nonnull ForceGroupsOptions forceGroupsOptions
    ) {
        final String testName = getTestName();

        final Optional<Integer> forceGroupBucket = forceGroupsOptions.getForcedBucketValue(testName);
        if (forceGroupBucket.isPresent()) {
            final TestBucket forcedTestBucket = getTestBucket(forceGroupBucket.get());
            if (forcedTestBucket != null) {
                final Optional<Payload> forceGroupPayload = forceGroupsOptions.getForcedPayloadValue(testName);
                if (forcedTestBucket.getPayload() != null && forceGroupPayload.isPresent()) {
                    final Payload payload = new Payload();
                    final Optional<PayloadType> forcePayloadType = forceGroupPayload.get().fetchPayloadType();
                    if (forcePayloadType.isPresent() && Payload.hasType(forcedTestBucket.getPayload(), forcePayloadType.get())) {
                        switch (forcePayloadType.get()) {
                            case DOUBLE_VALUE: {
                                payload.setDoubleValue(forceGroupPayload.get().getDoubleValue());
                                break;
                            }
                            case DOUBLE_ARRAY: {
                                payload.setDoubleArray(forceGroupPayload.get().getDoubleArray());
                                break;
                            }
                            case LONG_VALUE: {
                                payload.setLongValue(forceGroupPayload.get().getLongValue());
                                break;
                            }
                            case LONG_ARRAY: {
                                payload.setLongArray(forceGroupPayload.get().getLongArray());
                                break;
                            }
                            case STRING_VALUE: {
                                payload.setStringValue(forceGroupPayload.get().getStringValue());
                                break;
                            }
                            case STRING_ARRAY: {
                                payload.setStringArray(forceGroupPayload.get().getStringArray());
                                break;
                            }
                            case MAP: {
                                payload.setMap(forceGroupPayload.get().getMap());
                                break;
                            }
                        }
                        final TestBucket forcedTestBucketWithForcedPayload = TestBucket.builder().from(forcedTestBucket).payload(payload).build();
                        return new Result(forcedTestBucketWithForcedPayload, null);
                    }
                }
                // use a forced bucket, skip choosing an allocation
                return new Result(forcedTestBucket, null);
            }
        }

        if (forceGroupsOptions.getDefaultMode().equals(ForceGroupsDefaultMode.FALLBACK)) {
            // skip choosing a test bucket and an allocation
            return Result.EMPTY;
        }

        final TestChooser.Result result = chooseInternal(identifier, values, testGroups);

        if (forceGroupsOptions.getDefaultMode().equals(ForceGroupsDefaultMode.MIN_LIVE)) {
            // replace the bucket with the minimum active bucket in the resolved allocation.
            return Optional.ofNullable(result.getAllocation())
                    .map(Allocation::getRanges)
                    .map(Collection::stream)
                    .orElse(Stream.empty())
                    .filter(allocationRange -> allocationRange.getLength() > 0) // filter out 0% allocation ranges
                    .map(Range::getBucketValue)
                    .min(Integer::compareTo) // find the minimum bucket value
                    .flatMap(minActiveBucketValue -> Optional.ofNullable(getTestBucket(minActiveBucketValue)))
                    .map(minActiveBucket -> new Result(minActiveBucket, null))
                    .orElse(Result.EMPTY); // skip choosing a test bucket if failed to find the minimum active bucket
        }

        return result;
    }

    /**
     * Models a result of an assigned bucket and allocation by {@code TestChooser}.
     */
    class Result {
        /**
         * Empty result (no chosen buckets or no chosen allocations) which is typically used when
         * 1: all allocation rules aren't matched to a context, and
         * 2: forcing to use a fallback bucket.
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
