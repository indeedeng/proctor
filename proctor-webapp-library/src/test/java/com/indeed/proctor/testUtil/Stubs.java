package com.indeed.proctor.testUtil;

import com.indeed.proctor.common.EnvironmentVersion;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Stubs {

    public static TestDefinition createTestDefinition(final String bucketsString, final double[] ranges) {
        return createTestDefinition(bucketsString, TestType.RANDOM, "salt", ranges);
    }

    public static TestDefinition createTestDefinition(
            final String bucketsString,
            final TestType testType,
            final double[] ranges
    ) {
        return createTestDefinition(bucketsString, testType, "salt", ranges);
    }

    public static TestDefinition createTestDefinition(
            final String bucketsString,
            final TestType testType,
            final String salt,
            final double[] ranges
    ) {
        return createTestDefinition(bucketsString, testType, salt, ranges, null);
    }

    public static TestDefinition createTestDefinition(
            final String bucketsString,
            final TestType testType,
            final String salt,
            final double[] ranges,
            final Payload[] payloads
    ) {
        return createTestDefinition(bucketsString, testType, salt, ranges, payloads, null);
    }

    public static TestDefinition createTestDefinition(
            final String bucketsString,
            final double[] ranges,
            final List<String> allocationIds
    ) {
        return createTestDefinition(bucketsString, TestType.RANDOM, "salt", ranges, null, allocationIds);
    }

    public static TestDefinition createTestDefinition(
            final String bucketsString,
            final TestType testType,
            final String salt,
            final double[] ranges,
            final Payload[] payloads,
            final List<String> allocationIds
    ) {
        final List<Range> rangeList = new ArrayList<Range>();
        final String[] buckets = bucketsString.split(",");
        final List<TestBucket> buckList = new ArrayList<TestBucket>();

        for (int i = 0; i < buckets.length; i++) {
            final String bucket = buckets[i];
            final int colonInd = bucket.indexOf(':');
            final int bucketValue = Integer.parseInt(bucket.substring(colonInd + 1));
            final TestBucket tempBucket = new TestBucket(
                    bucket.substring(0, colonInd),
                    bucketValue,
                    "description",
                    (payloads == null) ? null : payloads[i]
            );
            buckList.add(tempBucket);
            final double range = i >= ranges.length ? 0 : ranges[i];
            rangeList.add(new Range(bucketValue, range));
        }

        final List<Allocation> allocList = new ArrayList<Allocation>();
        if (allocationIds == null) {
            allocList.add(new Allocation(null, rangeList));
        } else {
            for (final String allocationId : allocationIds) {
                allocList.add(new Allocation(null, rangeList, allocationId));
            }
        }
        return new TestDefinition(EnvironmentVersion.UNKNOWN_REVISION, null, testType, salt, buckList, allocList, Collections.<String, Object>emptyMap(), Collections.<String, Object>emptyMap(), null);
    }
}
