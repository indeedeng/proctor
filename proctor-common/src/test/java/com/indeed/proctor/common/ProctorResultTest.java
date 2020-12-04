package com.indeed.proctor.common;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestBucket;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

public class ProctorResultTest {

    @SuppressWarnings("deprecation") // intentionally using deprecated Constructor
    @Test
    public void testDeprecatedConstructorMapPreservation() {
        final Map<String, TestBucket> buckets = new HashMap<>();
        buckets.put("bucket1", new TestBucket("inactive", -1, ""));
        final Map<String, Allocation> allocations = new HashMap<>();
        allocations.put("allocation1", new Allocation());
        // intentionally using deprecated Constructor
        final ProctorResult proctorResult = new ProctorResult("", buckets, allocations, null);
        // historical behavior of deprecated constructor
        assertThat(proctorResult.getBuckets()).isInstanceOf(SortedMap.class).isEqualTo(buckets);
        assertThat(proctorResult.getAllocations()).isInstanceOf(SortedMap.class).isEqualTo(allocations);
        assertThat(proctorResult.getTestDefinitions()).isNotNull().isEmpty();
    }

    @Test
    public void testNewConstructorMap() {
        final SortedMap<String, TestBucket> buckets = new TreeMap<>();
        buckets.put("bucket1", new TestBucket("inactive", -1, ""));
        final SortedMap<String, Allocation> allocations = new TreeMap<>();
        allocations.put("allocation1", new Allocation());
        final Map<String, ConsumableTestDefinition> definitions = ImmutableMap.of("test1", new ConsumableTestDefinition());
        final ProctorResult proctorResult = new ProctorResult("", buckets, allocations, definitions);
        assertThat(proctorResult.getBuckets()).isInstanceOf(SortedMap.class).isEqualTo(buckets);
        assertThat(proctorResult.getAllocations()).isInstanceOf(SortedMap.class).isEqualTo(allocations);
        assertThat(proctorResult.getTestDefinitions()).isSameAs(definitions);
    }

    @Test
    public void testConstructorImmutableSortedMap() {
        final SortedMap<String, TestBucket> buckets = ImmutableSortedMap.of(
                "bucket1", new TestBucket("inactive", -1, ""));
        final SortedMap<String, Allocation> allocations = ImmutableSortedMap.of(
                "allocation1", new Allocation()
        );
        final Map<String, ConsumableTestDefinition> definitions = ImmutableMap.of("test1", new ConsumableTestDefinition());
        final ProctorResult proctorResult = new ProctorResult("", buckets, allocations, definitions);
        /*
         * Relying on Guava to not create expensive copies needlessly.
         * Using isSame as intentionally instead of isEqualTo, because this test tries to ensure no entries are copied
         */
        assertThat(proctorResult.getBuckets()).isSameAs(buckets);
        assertThat(proctorResult.getAllocations()).isSameAs(allocations);
        assertThat(proctorResult.getTestDefinitions()).isSameAs(definitions);
    }

    @Test
    public void testImmutableCopy() {
        final SortedMap<String, TestBucket> buckets = ImmutableSortedMap.of(
                "bucket1", new TestBucket("inactive", -1, ""));
        final SortedMap<String, Allocation> allocations = ImmutableSortedMap.of(
                "allocation1", new Allocation()
        );
        final Map<String, ConsumableTestDefinition> definitions = ImmutableMap.of("test1", new ConsumableTestDefinition());
        final ProctorResult proctorResult1 = new ProctorResult("", buckets, allocations, definitions);
        final ProctorResult proctorResult2 = ProctorResult.immutableCopy(proctorResult1);

        /*
         * Relying on Guava to not create expensive copies needlessly
         * Using isSame as intentionally instead of isEqualTo, because this test tries to ensure no entries are copied
         */
        assertThat(proctorResult2.getBuckets()).isSameAs(buckets);
        assertThat(proctorResult2.getAllocations()).isSameAs(allocations);
        assertThat(proctorResult2.getTestDefinitions()).isSameAs(definitions);
    }
}
