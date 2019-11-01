package com.indeed.proctor.consumer;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestType;

import java.util.Collections;
import java.util.Map;

import static java.util.Collections.emptyMap;

/**
 * @author matts
 */
public class CannedProctorResults {
    public static ProctorResult of(
            final Bucket<?>... results
    ) {
        final Map<String, TestBucket> buckets = Maps.newHashMapWithExpectedSize(results.length);
        final Map<String, ConsumableTestDefinition> testVersions = Maps.newHashMap();

        for (final Bucket<?> result: results) {
            final String testName = result.getClass().getSimpleName().toLowerCase();
            final int testValue = result.getValue();

            final TestBucket testBucket = new TestBucket(testName, testValue, testName);

            Preconditions.checkState(!buckets.containsKey(testName), "Attempted to provide two values for dummy test bucket %s", testName);
            buckets.put(testName, testBucket);
            final ConsumableTestDefinition testDefinition = addTestDefinition(testName, testVersions);

            testDefinition.getBuckets().add(testBucket);
        }

        return (new ProctorResult(Audit.EMPTY_VERSION, buckets, emptyMap(), testVersions));
    }

    public static ProctorResult of(
            final CannedProctorResult<?>... results
    ) {
        final Map<String, TestBucket> buckets = Maps.newHashMapWithExpectedSize(results.length);
        final Map<String, ConsumableTestDefinition> testVersions = Maps.newHashMap();

        for (final CannedProctorResult<?> result: results) {
            final String testName = result.testVal.getClass().getSimpleName().toLowerCase();
            final int testValue = result.testVal.getValue();

            final TestBucket testBucket = new TestBucket(testName, testValue, testName, result.payload);

            Preconditions.checkState(!buckets.containsKey(testName), "Attempted to provide two values for dummy test bucket %s", testName);
            buckets.put(testName, testBucket);

            // add a minimal test definition for each test
            final ConsumableTestDefinition testDefinition = addTestDefinition(testName, testVersions);

            testDefinition.getBuckets().add(testBucket);
        }

        return (new ProctorResult(Audit.EMPTY_VERSION, buckets, emptyMap(), testVersions));
    }


    private static ConsumableTestDefinition addTestDefinition(final String testName, final Map<String, ConsumableTestDefinition> testVersions) {
        // add a minimal test definition for each test
        ConsumableTestDefinition testDefinition = testVersions.get(testName);
        if (null == testDefinition) {
            testDefinition = new ConsumableTestDefinition(
                    "",
                    null, // no rule
                    TestType.RANDOM,
                    null, // no salt
                    Lists.newArrayList(),
                    Lists.newArrayListWithCapacity(1),
                    emptyMap(),
                    null);

            testVersions.put(testName, testDefinition);
        }
        return testDefinition;
    }
}
