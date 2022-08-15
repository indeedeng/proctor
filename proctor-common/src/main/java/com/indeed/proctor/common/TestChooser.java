package com.indeed.proctor.common;

import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.util.Arrays;
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
        final Payload validatedPayload = new Payload();
        final Optional<PayloadType> forcePayloadType = forcePayload.fetchPayloadType();
        // check if force payload exists and has the same payload type as the current payload
        if (forcePayloadType.isPresent() && Payload.hasType(currentPayload, forcePayloadType.get())) {
            switch (forcePayloadType.get()) {
                case DOUBLE_VALUE: {
                    validatedPayload.setDoubleValue(forcePayload.getDoubleValue());
                    break;
                }
                case DOUBLE_ARRAY: {
                    validatedPayload.setDoubleArray(forcePayload.getDoubleArray());
                    break;
                }
                case LONG_VALUE: {
                    validatedPayload.setLongValue(forcePayload.getLongValue());
                    break;
                }
                case LONG_ARRAY: {
                    validatedPayload.setLongArray(forcePayload.getLongArray());
                    break;
                }
                case STRING_VALUE: {
                    validatedPayload.setStringValue(forcePayload.getStringValue());
                    break;
                }
                case STRING_ARRAY: {
                    validatedPayload.setStringArray(forcePayload.getStringArray());
                    break;
                }
                case MAP: {
                    // need to validate the map contains the same value class types, can not simply set
                    final Map<String, Object> validatedPayloadMap = validateForcePayloadMap(new HashMap<>(currentPayload.getMap()), forcePayload.getMap());
                    validatedPayload.setMap(validatedPayloadMap);
                    break;
                }
            }
            return validatedPayload;
        }
        return currentPayload;
    }

    /*
     * Validated Force Payload Map by checking that each forced key exists in the current payload and is of the same instance type. If forcePayload is invalid return currentPayload to not overwrite
     */
    @Nullable
    default Map<String, Object> validateForcePayloadMap(@Nullable final Map<String, Object> currentPayloadMap, @Nullable final Map<String, Object> forcePayloadMap) {
        if (currentPayloadMap != null) {
            if (forcePayloadMap != null) {
                final Map<String, Object> validatedMap = new HashMap<>(currentPayloadMap);
                for (final String keyString : forcePayloadMap.keySet()) {
                    if (currentPayloadMap.containsKey(keyString)) {
                        try {
                            final String forcePayloadValue = (String) forcePayloadMap.get(keyString);
                            // check current class of value and try to parse force value to it. force values are strings before validation
                            if (currentPayloadMap.get(keyString) instanceof Double) {
                                validatedMap.put(keyString, Double.parseDouble(forcePayloadValue));
                            } else if (currentPayloadMap.get(keyString) instanceof Double[]) {
                                validatedMap.put(keyString, Arrays.stream(ForceGroupsOptionsStrings.getPayloadArray(forcePayloadValue))
                                        .map(Double::valueOf)
                                        .toArray(Double[]::new));
                            } else if (currentPayloadMap.get(keyString) instanceof Long) {
                                validatedMap.put(keyString, Long.parseLong(forcePayloadValue));
                            } else if (currentPayloadMap.get(keyString) instanceof Long[]) {
                                validatedMap.put(keyString, Arrays.stream(ForceGroupsOptionsStrings.getPayloadArray(forcePayloadValue))
                                        .map(Long::valueOf)
                                        .toArray(Long[]::new));
                            } else if (currentPayloadMap.get(keyString) instanceof String) {
                                validatedMap.put(keyString, forcePayloadValue.substring(1,forcePayloadValue.length()-1));
                            } else if (currentPayloadMap.get(keyString) instanceof String[]) {
                                validatedMap.put(keyString, ForceGroupsOptionsStrings.getPayloadStringArray(forcePayloadValue.substring(1,forcePayloadValue.length()-1)));
                            } else {
                                return currentPayloadMap;
                            }
                        } catch (final IllegalArgumentException | ArrayStoreException | ClassCastException e) {
                            return currentPayloadMap;
                        }
                    } else {
                        return currentPayloadMap;
                    }
                }
                return validatedMap;
            }
            return currentPayloadMap;
        }
        return null;
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
