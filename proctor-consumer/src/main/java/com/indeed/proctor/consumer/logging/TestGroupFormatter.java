package com.indeed.proctor.consumer.logging;

import com.indeed.proctor.common.model.TestBucket;
import org.apache.commons.lang3.StringUtils;

/**
 * Formatters append to a provided StringBuilder for efficiency reasons, but generally exist to
 * create a String representing a resolved proctor group
 */
public interface TestGroupFormatter {

    char DEFAULT_ALLOCATION_GROUP_SEPARATOR = ':';

    /**
     * Appends test groups in the form with allocation ids as [test-name + bucket-value] for given
     * test names.
     */
    TestGroupFormatter WITHOUT_ALLOC_ID =
            (sb, testName, allocationId, bucketValue) ->
                    sb.append(testName).append(bucketValue.getValue());

    /**
     * Appends test groups in the form with allocation ids as [allocation-id + ":" + test-name +
     * bucket-value] for given test names. If allocation Id is empty, appends nothing
     */
    TestGroupFormatter WITH_ALLOC_ID =
            (sb, testName, allocationId, bucketValue) -> {
                if (!StringUtils.isEmpty(allocationId)) {
                    sb.append(allocationId).append(DEFAULT_ALLOCATION_GROUP_SEPARATOR);
                    WITHOUT_ALLOC_ID.appendProctorTestGroup(
                            sb, testName, allocationId, bucketValue);
                }
            };

    /**
     * append representation for the given testbucket of this testname in the given allocation to
     * the StringBuilder
     *
     * @param stringBuilder A stringBuilder to which to append to. it's empty or ends with
     *     groupSeparator.
     * @param testName the proctor testname for the group
     * @param allocationId allocation (might be empty String for historic data)
     * @param bucketValue the selected bucket / group in the allocation
     */
    void appendProctorTestGroup(
            final StringBuilder stringBuilder,
            final String testName,
            final String allocationId,
            final TestBucket bucketValue);
}
