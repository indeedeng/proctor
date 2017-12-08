package com.indeed.proctor.common;

import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestBucket;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Maps;

import javax.annotation.Nonnull;

/**
 * Return value from {@link Proctor#determineTestGroups(Identifiers, java.util.Map, java.util.Map)}
 * @author ketan
 *
 */
public class ProctorResult {
    public static final ProctorResult EMPTY = new ProctorResult(
            Audit.EMPTY_VERSION,
            Collections.<String, TestBucket>emptyMap(),
            Collections.<String, Allocation>emptyMap(),
            Collections.<String, ConsumableTestDefinition>emptyMap()
    );

    private final String matrixVersion;
    @Nonnull
    private final Map<String, TestBucket> buckets;
    @Nonnull
    private final Map<String, Allocation> allocations;
    @Nonnull
    private final Map<String, ConsumableTestDefinition> testDefinitions;

    @Deprecated
    public ProctorResult(
            final int matrixVersion,
            @Nonnull final Map<String, TestBucket> buckets,
            @Nonnull final Map<String, ConsumableTestDefinition> testDefinitions
    ) {
        this(new Integer(matrixVersion).toString(), buckets, Collections.<String, Allocation>emptyMap(), testDefinitions);
    }

    @Deprecated
    public ProctorResult(
            final String matrixVersion,
            @Nonnull final Map<String, TestBucket> buckets,
            @Nonnull final Map<String, ConsumableTestDefinition> testDefinitions
    ) {
        this(matrixVersion, buckets, Collections.<String, Allocation>emptyMap(), testDefinitions);
    }

    public ProctorResult(
            final String matrixVersion,
            @Nonnull final Map<String, TestBucket> buckets,
            @Nonnull final Map<String, Allocation> allocations,
            @Nonnull final Map<String, ConsumableTestDefinition> testDefinitions
    ) {
        this.matrixVersion = matrixVersion;
        this.buckets = Maps.newTreeMap();
        this.buckets.putAll(buckets);
        this.allocations = Maps.newTreeMap();
        this.allocations.putAll(allocations);
        this.testDefinitions = testDefinitions;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getMatrixVersion() {
        return matrixVersion;
    }

    @Nonnull
    public Map<String, TestBucket> getBuckets() {
        return buckets;
    }

    @Nonnull
    public Map<String, Allocation> getAllocations() {
        return allocations;
    }

    @Nonnull
    public Map<String, ConsumableTestDefinition> getTestDefinitions() {
        return testDefinitions;
    }

    @Nonnull
    public Map<String, String> getTestVersions() {
        // TODO ImmutableMap?
        final Map<String, String> testVersions = Maps.newHashMap();
        for (final Entry<String, ConsumableTestDefinition> entry : testDefinitions.entrySet()) {
            testVersions.put(entry.getKey(), entry.getValue().getVersion());
        }
        return testVersions;
    }
}
