package com.indeed.proctor.webapp.jobs;

import com.indeed.proctor.common.model.Range;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class AllocationUtil {

    private AllocationUtil() {
    }

    public static Map<Integer, Double> generateAllocationRangeMap(final List<Range> ranges) {
        final Map<Integer, Double> bucketToTotalAllocationMap = new HashMap<>();
        for (final Range range : ranges) {
            final int bucketVal = range.getBucketValue();
            double sum = range.getLength();
            final Double allocationValue = bucketToTotalAllocationMap.get(bucketVal);
            if (allocationValue != null) {
                sum += allocationValue;
            }
            bucketToTotalAllocationMap.put(bucketVal, sum);
        }
        return bucketToTotalAllocationMap;
    }
}
