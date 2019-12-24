package com.indeed.proctor.common;

import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestBucket;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.util.Collections.emptyMap;

/**
 * Return value from {@link Proctor#determineTestGroups(Identifiers, java.util.Map, java.util.Map)}
 * @author ketan
 *
 */
public class ProctorResult {
    public static final ProctorResult EMPTY = new ProctorResult(
            Audit.EMPTY_VERSION,
            emptyMap(),
            emptyMap(),
            emptyMap()
    );

    private final String matrixVersion;
    /**
     * maps from testname to bucket
     */
    @Nonnull
    private final SortedMap<String, TestBucket> buckets;
    /**
     * maps from testname to allocation
     */
    @Nonnull
    private final SortedMap<String, Allocation> allocations;
    /**
     * maps from testname to TestDefinition
     */
    @Nonnull
    private final Map<String, ConsumableTestDefinition> testDefinitions;

    @Deprecated
    public ProctorResult(
            final int matrixVersion,
            @Nonnull final Map<String, TestBucket> buckets,
            @Nullable final Map<String, ConsumableTestDefinition> testDefinitions
    ) {
        this(Integer.toString(matrixVersion), buckets, emptyMap(), testDefinitions);
    }

    @Deprecated
    public ProctorResult(
            final String matrixVersion,
            @Nonnull final Map<String, TestBucket> buckets,
            @Nullable final Map<String, ConsumableTestDefinition> testDefinitions
    ) {
        this(matrixVersion, buckets, emptyMap(), testDefinitions);
    }

    public ProctorResult(
            final String matrixVersion,
            @Nonnull final Map<String, TestBucket> buckets,
            @Nonnull final Map<String, Allocation> allocations,
            // allowing null for historical reasons
            @Nullable final Map<String, ConsumableTestDefinition> testDefinitions
    ) {
        this.matrixVersion = matrixVersion;
        this.buckets = new TreeMap<>(buckets);
        this.allocations = new TreeMap<>(allocations);
        this.testDefinitions = (testDefinitions == null) ? emptyMap() : new HashMap<>(testDefinitions);
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getMatrixVersion() {
        return matrixVersion;
    }

    /**
     * Returns a map ordered by testname
     */
    @Nonnull
    // returning Map instead of SortedMap for historic reasons
    public Map<String, TestBucket> getBuckets() {
        return buckets;
    }

    /**
     * Returns a map ordered by testname
     */
    @Nonnull
    // returning Map instead of SortedMap for historic reasons
    public Map<String, Allocation> getAllocations() {
        return allocations;
    }

    @Nonnull
    public Map<String, ConsumableTestDefinition> getTestDefinitions() {
        return testDefinitions;
    }


}
