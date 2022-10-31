package com.indeed.proctor.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
            @Nullable final IdentifierType identifier,
            @Nonnull final Map<String, Object> values,
            @Nonnull final Map<String, TestBucket> testGroups,
            @Nonnull final ForceGroupsOptions forceGroupsOptions
    ) {
        final String testName = getTestName();

        final Optional<Integer> forceGroupBucket = forceGroupsOptions.getForcedBucketValue(testName);
        if (forceGroupBucket.isPresent()) {
            final TestBucket forcedTestBucket = getTestBucket(forceGroupBucket.get());
            if (forcedTestBucket != null) {
                final Optional<Payload> forcePayloadValues = forceGroupsOptions.getForcedPayloadValue(testName);
                final Payload currentPayload = forcedTestBucket.getPayload();
                if (currentPayload != null && currentPayload != Payload.EMPTY_PAYLOAD && forcePayloadValues.isPresent()) {
                    final TestBucket forcedTestBucketWithForcedPayload = TestBucket.builder()
                            .from(forcedTestBucket)
                            .payload(validateForcePayload(currentPayload, forcePayloadValues.get()))
                            .build();
                    return new Result(forcedTestBucketWithForcedPayload, null);
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

    default Payload validateForcePayload(final Payload currentPayload, final Payload forcePayload) {
        // check if force payload exists and has the same payload type as the current payload
        if (forcePayload.sameType(currentPayload)) {
            // Payload type json currently not supported
            if (!Payload.hasType(forcePayload, PayloadType.JSON)) {
                if (Payload.hasType(forcePayload, PayloadType.MAP)) {
                    return validateForcePayloadMap(currentPayload, forcePayload);
                }
                return forcePayload;
            }
        }
        return currentPayload;
    }

    /*
     * Validated Force Payload Map by checking that each forced key exists in the current payload and is of the same instance type. If forcePayload is invalid return currentPayload to not overwrite
     */
    @Nullable
    default Payload validateForcePayloadMap(@Nullable final Payload currentPayload, @Nullable final Payload forcePayload) {
        final Map<String, Object> currentPayloadMap = currentPayload.getMap();
        final Map<String, Object> forcePayloadMap = forcePayload.getMap();
        final ObjectMapper objectMapper = new ObjectMapper();
        if (currentPayloadMap != null && forcePayloadMap != null) {
            final Map<String, Object> validatedMap = new HashMap<>(currentPayloadMap);
            for (final String keyString : forcePayloadMap.keySet()) {
                if (currentPayloadMap.containsKey(keyString)) {
                    try {
                        final Object forcePayloadValue = forcePayloadMap.get(keyString);
                        // check current class of value and try to parse force value to it. force values are strings before validation
                        if (currentPayloadMap.get(keyString) instanceof Double) {
                            validatedMap.put(keyString, forcePayloadValue);
                        } else if (currentPayloadMap.get(keyString) instanceof Double[]) {
                            validatedMap.put(keyString, ((ArrayList<Double>)forcePayloadValue).toArray(new Double[0]));
                        } else if (currentPayloadMap.get(keyString) instanceof Long) {
                            // ObjectMapper reads in as Object and automatically chooses Integer over Long this recasts to Long
                            validatedMap.put(keyString, Long.valueOf((Integer)forcePayloadValue));
                        } else if (currentPayloadMap.get(keyString) instanceof Long[]) {
                            // ObjectMapper reads in as Object and automatically chooses Integer[] over Long[] this recasts to Long[]
                            validatedMap.put(keyString, objectMapper.readValue(objectMapper.writeValueAsString(forcePayloadValue), Long[].class));
                        } else if (currentPayloadMap.get(keyString) instanceof String) {
                            validatedMap.put(keyString, forcePayloadValue);
                        } else if (currentPayloadMap.get(keyString) instanceof String[]) {
                            validatedMap.put(keyString, ((ArrayList<String>)forcePayloadValue).toArray(new String[0]));
                        } else {
                            return currentPayload;
                        }
                    } catch (final IllegalArgumentException | ArrayStoreException | ClassCastException | IOException e) {
                        return currentPayload;
                    }
                } else {
                    return currentPayload;
                }
            }
            return new Payload(validatedMap);
        }
        return currentPayload;
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
