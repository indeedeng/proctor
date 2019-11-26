package com.indeed.proctor.common;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * infers a specification from an actual testDefinition.
 * Buckets need to have compatible payloads because code generation generates typesafe payload accessors
 */
public class SpecificationGenerator {

    /**
     * Generates a usable test specification for a given test definition
     * Uses the bucket with smallest value as the fallback value
     *
     * @param testDefinition a {@link TestDefinition}
     * @return a {@link TestSpecification} which corresponding to given test definition.
     */
    // non-static for easy mocking in tests
    @Nonnull
    public TestSpecification generateSpecification(@Nonnull final TestDefinition testDefinition) {
        final TestSpecification testSpecification = new TestSpecification();
        // Sort buckets by value ascending

        final List<TestBucket> testDefinitionBuckets = Ordering.from(new Comparator<TestBucket>() {
            @Override
            public int compare(final TestBucket lhs, final TestBucket rhs) {
                return Ints.compare(lhs.getValue(), rhs.getValue());
            }
        }).immutableSortedCopy(testDefinition.getBuckets());
        int fallbackValue = -1;
        if (!testDefinitionBuckets.isEmpty()) {
            // buckets are sorted, choose smallest value as the fallback value
            fallbackValue = testDefinitionBuckets.get(0).getValue();
            final Optional<PayloadSpecification> specOpt = generatePayloadSpecification(testDefinitionBuckets.stream()
                    .map(TestBucket::getPayload)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
            specOpt.ifPresent(testSpecification::setPayload);
        }

        final Map<String, Integer> buckets = Maps.newLinkedHashMap();
        for (final TestBucket bucket : testDefinitionBuckets) {
            buckets.put(bucket.getName(), bucket.getValue());
        }
        testSpecification.setBuckets(buckets);
        testSpecification.setDescription(testDefinition.getDescription());
        testSpecification.setFallbackValue(fallbackValue);
        return testSpecification;
    }

    /**
     * If list of payloads contains payloads, unifies all to infer a payload specification
     */
    @VisibleForTesting
    @Nonnull
    static Optional<PayloadSpecification> generatePayloadSpecification(final List<Payload> payloads) {
        return determinePayloadTypeFromPayloads(payloads).map(payloadType -> {
            final PayloadSpecification payloadSpecification = new PayloadSpecification();
            payloadSpecification.setType(payloadType.payloadTypeName);
            if (payloadType.equals(PayloadType.MAP)) {
                generateMapPayloadSchema(payloads)
                        .ifPresent(schema -> payloadSpecification.setSchema(
                                schema.entrySet().stream()
                                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().payloadTypeName))));
            }
            return payloadSpecification;
        });
    }

    /**
     * @return the unified payload type of all buckets, empty if no bucket has any payload
     * @throws IllegalArgumentException if any 2 buckets have incompatible payload types
     */
    @Nonnull
    private static Optional<PayloadType> determinePayloadTypeFromPayloads(@Nonnull final List<Payload> testDefinitionBuckets) {
        final List<PayloadType> types = testDefinitionBuckets.stream()
                .map(Payload::fetchPayloadType)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .collect(Collectors.toList());
        if (types.size() > 1) {
            throw new IllegalArgumentException("Payloads not compatible: " + types);
        }
        if (types.size() == 1) {
            return Optional.of(types.get(0));
        }
        return Optional.empty();
    }

    /**
     * creates a specification based on existing payloads. For map type payloads, unifies all information from all payloads
     */
    @Nonnull
    private static Optional<Map<String, PayloadType>> generateMapPayloadSchema(@Nonnull final List<Payload> payloads) {
        if (payloads.isEmpty()) {
            return Optional.empty();
        }

        final Set<String> emptyListValuePayloadKeys = new HashSet<>();
        final List<Map<String, PayloadType>> schemas = new ArrayList<>();
        for (final Payload payload: payloads) {
            inferSchemaForPayload(payload, schemas, emptyListValuePayloadKeys);
        }
        final Map<String, PayloadType> resultPayloadMapSchema = mergeSchemas(schemas);


        for (final String key: emptyListValuePayloadKeys) {
            if (!resultPayloadMapSchema.containsKey(key) || !resultPayloadMapSchema.get(key).isArrayType()) {
                throw new IllegalArgumentException("Cannot infer map schema type for key " + key);
            }
        }
        // do not return an Optional of emptyMap
        return Optional.ofNullable(resultPayloadMapSchema.isEmpty() ? null : resultPayloadMapSchema);
    }

    @Nonnull
    private static Map<String, PayloadType> mergeSchemas(@Nonnull final List<Map<String, PayloadType>> schemas) {
        final Map<String, PayloadType> resultPayloadMapSchema = new HashMap<>();

        for (final Map<String, PayloadType> loopPayloadMapSchema: schemas) {
            for (final Map.Entry<String, PayloadType> entry : loopPayloadMapSchema.entrySet()) {
                resultPayloadMapSchema.compute(entry.getKey(), (k, v) -> {
                    if (v != null && !v.equals(entry.getValue())) {
                        throw new IllegalArgumentException("Ambiguous map schema for key " + k);
                    }
                    return v == null ? entry.getValue() : v;
                });
            }
        }
        return resultPayloadMapSchema;
    }

    /**
     * @param payload a payload with PayloadType.MAP
     * @param schemas accumulator for schemas, this method should add a map to this list
     * @param emptyListValuePayloadKeys accumulator for keys having no payload that have no clear type
     */
    private static void inferSchemaForPayload(
            @Nonnull final Payload payload,
            @Nonnull final List<Map<String, PayloadType>> schemas,
            @Nonnull final Set<String> emptyListValuePayloadKeys
    ) {
        Preconditions.checkState(
                // should always be true
                payload.fetchPayloadType().isPresent() && payload.fetchPayloadType().get().equals(PayloadType.MAP),
                "Bug, method called with non-Map payload " + payload.fetchPayloadType());

        final Map<String, PayloadType> loopPayloadMapSchema = new HashMap<>();
        final Map<String, Object> payloadMap = payload.getMap();
        if (payloadMap != null) {
            for (final Map.Entry<String, Object> entry : payloadMap.entrySet()) {
                // empty list cannot be determined, but allow anyway for user convenience, if other buckets have type
                if (!(entry.getValue() instanceof List && ((List) entry.getValue()).isEmpty())) {
                    loopPayloadMapSchema.put(entry.getKey(), PayloadType.payloadTypeForValue(entry.getValue()));
                } else {
                    emptyListValuePayloadKeys.add(entry.getKey());
                }
            }
        }
        schemas.add(loopPayloadMapSchema);
    }

}
