package com.indeed.proctor.common;

import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestBucket;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySortedMap;

/**
 * Return value from {@link Proctor#determineTestGroups(Identifiers, java.util.Map, java.util.Map)}
 *
 * @author ketan
 */
public class ProctorResult {
    public static final ProctorResult EMPTY =
            new ProctorResult(
                    Audit.EMPTY_VERSION,
                    emptySortedMap(),
                    emptySortedMap(),
                    emptyMap(),
                    new Identifiers(emptyMap()),
                    emptyMap(),
                    emptyMap());

    private final String matrixVersion;
    /** maps from testname to bucket */
    @Nonnull private final SortedMap<String, TestBucket> buckets;
    /** maps from testname to allocation */
    @Nonnull private final SortedMap<String, Allocation> allocations;
    /** maps from testname to TestDefinition */
    @Nonnull private final Map<String, ConsumableTestDefinition> testDefinitions;

    @Nonnull private final Map<String, PayloadProperty> properties;

    private final Identifiers identifiers;

    private final Map<String, Object> inputContext;

    private final HashSet<String> hasLoggedTests;

    /**
     * Create a ProctorResult with copies of the provided collections
     *
     * @deprecated this constructor creates copies of all inputs
     */
    @Deprecated
    public ProctorResult(
            @Nonnull final int matrixVersion,
            @Nonnull final Map<String, TestBucket> buckets,
            // allowing null for historical reasons
            @Nullable final Map<String, ConsumableTestDefinition> testDefinitions) {
        this(Integer.toString(matrixVersion), buckets, emptyMap(), testDefinitions);
    }

    /**
     * Create a ProctorResult with copies of the provided collections
     *
     * @deprecated this constructor creates copies of all inputs
     */
    @Deprecated
    public ProctorResult(
            @Nonnull final String matrixVersion,
            @Nonnull final Map<String, TestBucket> buckets,
            // allowing null for historical reasons
            @Nullable final Map<String, ConsumableTestDefinition> testDefinitions) {
        this(matrixVersion, buckets, emptyMap(), testDefinitions);
    }

    /**
     * Create a ProctorResult with copies of the provided collections
     *
     * @param matrixVersion any string, used for debugging
     * @param buckets the resolved bucket for each test
     * @param allocations the determined allocation for each test
     * @param testDefinitions the original test definitions
     * @deprecated this constructor creates copies of all input collections negatively effecting performance
     */
    @Deprecated
    public ProctorResult(
            final String matrixVersion,
            @Nonnull final Map<String, TestBucket> buckets,
            @Nonnull final Map<String, Allocation> allocations,
            // allowing null for historical reasons
            @Nullable final Map<String, ConsumableTestDefinition> testDefinitions) {
        // Potentially client applications might need to build ProctorResult instances in each
        // request, and some apis
        // have large proctorResult objects, so if teams use this constructor, this may have a
        // noticeable
        // impact on latency and GC, so ideally clients should avoid this constructor.
        this(
                matrixVersion,
                new TreeMap<>(buckets),
                new TreeMap<>(allocations),
                (testDefinitions == null) ? emptyMap() : new HashMap<>(testDefinitions));
    }

    /**
     * Create a ProctorResult with copies of the provided collections
     *
     * @param matrixVersion any string, used for debugging
     * @param buckets the resolved bucket for each test
     * @param allocations the determined allocation for each test
     * @param testDefinitions the original test definitions
     * @param properties the properties
     * @deprecated this constructor creates copies of all input collections negatively effecting performance
     */
    @Deprecated
    public ProctorResult(
            final String matrixVersion,
            @Nonnull final Map<String, TestBucket> buckets,
            @Nonnull final Map<String, Allocation> allocations,
            // allowing null for historical reasons
            @Nullable final Map<String, ConsumableTestDefinition> testDefinitions,
            @Nonnull final Map<String, PayloadProperty> properties) {
        // Potentially client applications might need to build ProctorResult instances in each
        // request, and some apis
        // have large proctorResult objects, so if teams use this constructor, this may have a
        // noticeable
        // impact on latency and GC, so ideally clients should avoid this constructor.
        this(
                matrixVersion,
                new TreeMap<>(buckets),
                new TreeMap<>(allocations),
                (testDefinitions == null) ? emptyMap() : new HashMap<>(testDefinitions),
                new Identifiers(emptyMap()),
                emptyMap(),
                properties);
    }

    /**
     * Plain constructor, not creating TreeMaps.
     *
     * @param matrixVersion any string, used for debugging
     * @param buckets the resolved bucket for each test
     * @param allocations the determined allocation for each test
     * @param testDefinitions the original test definitions
     */
    @Deprecated
    public ProctorResult(
            @Nonnull final String matrixVersion,
            @Nonnull final SortedMap<String, TestBucket> buckets,
            @Nonnull final SortedMap<String, Allocation> allocations,
            @Nonnull final Map<String, ConsumableTestDefinition> testDefinitions) {
        this(
                matrixVersion,
                buckets,
                allocations,
                testDefinitions,
                new Identifiers(emptyMap()),
                emptyMap());
    }

    /**
     * Plain constructor, not creating TreeMaps.
     *
     * @param matrixVersion any string, used for debugging
     * @param buckets the resolved bucket for each test
     * @param allocations the determined allocation for each test
     * @param testDefinitions the original test definitions
     * @param identifiers the identifiers used for determine groups
     * @param inputContext the context variables
     */
    public ProctorResult(
            @Nonnull final String matrixVersion,
            @Nonnull final SortedMap<String, TestBucket> buckets,
            @Nonnull final SortedMap<String, Allocation> allocations,
            @Nonnull final Map<String, ConsumableTestDefinition> testDefinitions,
            @Nonnull final Identifiers identifiers,
            @Nonnull final Map<String, Object> inputContext) {
        this(
                matrixVersion,
                buckets,
                allocations,
                testDefinitions,
                identifiers,
                inputContext,
                emptyMap());
    }

    /**
     * Plain constructor, not creating TreeMaps.
     *
     * @param matrixVersion any string, used for debugging
     * @param buckets the resolved bucket for each test
     * @param allocations the determined allocation for each test
     * @param testDefinitions the original test definitions
     * @param identifiers the identifiers used for determine groups
     * @param inputContext the context variables
     * @param properties the aggregated properties of payload experiments
     */
    public ProctorResult(
            @Nonnull final String matrixVersion,
            @Nonnull final SortedMap<String, TestBucket> buckets,
            @Nonnull final SortedMap<String, Allocation> allocations,
            @Nonnull final Map<String, ConsumableTestDefinition> testDefinitions,
            @Nonnull final Identifiers identifiers,
            @Nonnull final Map<String, Object> inputContext,
            @Nonnull final Map<String, PayloadProperty> properties) {
        this.matrixVersion = matrixVersion;
        this.buckets = buckets;
        this.allocations = allocations;
        this.testDefinitions = testDefinitions;
        this.identifiers = identifiers;
        this.inputContext = inputContext;
        this.hasLoggedTests = new HashSet<>();
        this.properties = properties;
    }

    /**
     * @return a new Proctor Result, which does not allow modifying the contained collections. The
     *     result's fields are views of the original fields, to reduce memory allocation effort.
     */
    public static ProctorResult unmodifiableView(final ProctorResult proctorResult) {
        return new ProctorResult(
                proctorResult.matrixVersion,
                // using fields directly because methods do not expose SortedMap type
                Collections.unmodifiableSortedMap(proctorResult.buckets),
                Collections.unmodifiableSortedMap(proctorResult.allocations),
                Collections.unmodifiableMap(proctorResult.testDefinitions),
                proctorResult.identifiers,
                Collections.unmodifiableMap(proctorResult.inputContext),
                Collections.unmodifiableMap(proctorResult.properties));
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getMatrixVersion() {
        return matrixVersion;
    }

    /** @return a SortedMap (should be ordered by testname) */
    @Nonnull
    // returning Map instead of SortedMap for historic reasons (changing breaks compiled libraries)
    public Map<String, TestBucket> getBuckets() {
        return buckets;
    }

    /** @return a SortedMap (should be ordered by testname) */
    @Nonnull
    // returning Map instead of SortedMap for historic reasons (changing breaks compiled libraries)
    public Map<String, Allocation> getAllocations() {
        return allocations;
    }

    @Nonnull
    public Map<String, ConsumableTestDefinition> getTestDefinitions() {
        return testDefinitions;
    }

    @Nonnull
    public Identifiers getIdentifiers() {
        return identifiers;
    }

    @Nonnull
    public Map<String, Object> getInputContext() {
        return inputContext;
    }

    @Nonnull
    public Map<String, PayloadProperty> getProperties() {
        return properties;
    }

    public boolean markTestAsLogged(final String test) {
        return this.hasLoggedTests.add(test);
    }
}
