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

/**
 * @author matts
 */
public class CannedProctorResults {
    public static ProctorResult of(
            Bucket<?>... results
    ) {
        Map<String, TestBucket> buckets = Maps.newHashMapWithExpectedSize(results.length);
        Map<String, ConsumableTestDefinition> testVersions = Collections.emptyMap();

        for (final Bucket<?> result: results) {
            final String testName = result.getClass().getSimpleName().toLowerCase();
            final int testValue = result.getValue();

            TestBucket testBucket = new TestBucket(testName, testValue, testName);

            Preconditions.checkState(!buckets.containsKey(testName), "Attempted to provide two values for dummy test bucket %s", testName);
            buckets.put(testName, testBucket);
        }

        return (new ProctorResult(Audit.EMPTY_VERSION, buckets, testVersions));
    }

    public static ProctorResult of(
            CannedProctorResult<?>... results
    ) {
        Map<String, TestBucket> buckets = Maps.newHashMapWithExpectedSize(results.length);
        Map<String, ConsumableTestDefinition> testVersions = Maps.newHashMap();

        for (final CannedProctorResult<?> result: results) {
            final String testName = result.testVal.getClass().getSimpleName().toLowerCase();
            final int testValue = result.testVal.getValue();

            TestBucket testBucket = new TestBucket(testName, testValue, testName);
            if (result.payload != null) {
                testBucket.setPayload(result.payload);
            }

            Preconditions.checkState(!buckets.containsKey(testName), "Attempted to provide two values for dummy test bucket %s", testName);
            buckets.put(testName, testBucket);

            // add a minimal test definition for each test
            ConsumableTestDefinition testDefinition = testVersions.get(testName);
            if (null == testDefinition) {
                testDefinition = new ConsumableTestDefinition(
                        "",
                        null, // no rule
                        TestType.RANDOM,
                        null, // no salt
                        Lists.<TestBucket>newArrayList(),
                        Lists.<Allocation>newArrayListWithCapacity(1),
                        Collections.<String, Object>emptyMap(),
                        null);

                testVersions.put(testName, testDefinition);
            }

            testDefinition.getBuckets().add(testBucket);
        }

        return (new ProctorResult(Audit.EMPTY_VERSION, buckets, testVersions));
    }
}
