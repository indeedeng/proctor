package com.indeed.proctor.consumer;

import com.google.common.collect.Maps;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.TestBucket;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public abstract class AbstractGroups {
    private final ProctorResult proctorResult;
    private final LinkedHashMap<String, TestBucket> buckets;

    protected AbstractGroups(final ProctorResult proctorResult) {
        this.proctorResult = proctorResult;
        this.buckets = Maps.newLinkedHashMap();
        for (final Entry<String, TestBucket> entry : proctorResult.getBuckets().entrySet()) {
            final TestBucket testBucket = entry.getValue();
            this.buckets.put(entry.getKey(), testBucket);
        }
    }

    public Map<String, Integer> getTestVersions() {
        return proctorResult.getTestVersions();
    }

    protected boolean isBucketActive(final String testName, final int value) {
        final TestBucket testBucket = buckets.get(testName);
        return ((testBucket != null) && (value == testBucket.getValue()));
    }

    protected int getValue(final String testName, final int defaultValue) {
        final TestBucket testBucket = buckets.get(testName);
        if (testBucket == null) {
            return defaultValue;
        }
        return testBucket.getValue();
    }

    public Map<String, Integer> getTestVersions(final Set<String> tests) {
        final Map<String, Integer> selectedTestVersions = Maps.newLinkedHashMap();
        final Map<String, ConsumableTestDefinition> testDefinitions = proctorResult.getTestDefinitions();
        for (final String testName : tests) {
            final ConsumableTestDefinition testDefinition = testDefinitions.get(testName);
            if (testDefinition != null) {
                selectedTestVersions.put(testName, testDefinition.getVersion());
            }
        }
        return selectedTestVersions;
    }

    /**
     * Return the Payload attached to the current active bucket for |test|.
     * Always returns a payload so the client doesn't crash on a malformed
     * test definition.
     */
    protected Payload getPayload(final String testName) {
        // Get the current bucket.
        final TestBucket testBucket = buckets.get(testName);

        // Lookup Payloads for this test
        if (testBucket != null) {
            final Payload payload = testBucket.getPayload();
            if (payload != null) {
                return payload;
            }
        }
        // Else we didn't find something.  Return our emptyPayload
        return Payload.EMPTY_PAYLOAD;
    }


     /**
     * Return the TestBucket, as defined in the current test matrix, for the test called testName with bucket value targetBucket.getValue().
     *
     * Can return null if it can't find any such bucket.
     *
     * This does a linear search over the list of defined buckets.  There shouldn't be too many buckets in any test,
     * so this should be fast enough.
     */
    protected @Nullable
    TestBucket getTestBucketForBucket(final String testName, Bucket<?> targetBucket) {
        final @Nullable Map<String, ConsumableTestDefinition> testDefinitions = proctorResult.getTestDefinitions();
        if (testDefinitions != null) {
            final @Nullable ConsumableTestDefinition testDefinition = testDefinitions.get(testName);
            if (testDefinition != null) {
                final @Nullable List<TestBucket> buckets = testDefinition.getBuckets();
                if (buckets != null) {
                    for (final TestBucket testBucket : buckets) {
                        if (targetBucket.getValue() == testBucket.getValue()) {
                            return testBucket;
                        }
                    }
                }
            }
        }
        return null;
    }

    public String toLongString() {
        if (proctorResult.getBuckets().isEmpty()) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        for (final Entry<String, TestBucket> entry : proctorResult.getBuckets().entrySet()) {
            final String testName = entry.getKey();
            final TestBucket testBucket = entry.getValue();
            sb.append(testName).append('-').append(testBucket.getName()).append(',');
        }
        return sb.deleteCharAt(sb.length() - 1)
                .toString();
    }

    /**
     * To be called when logging ONLY
     */
    @Override
    public String toString() {
        if (isEmpty()) {
            return "";
        }
        final StringBuilder sb = buildTestGroupString();
        if (sb.length() == 0) {
            return "";
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public StringBuilder buildTestGroupString() {
        final StringBuilder sb = new StringBuilder();
        appendTestGroups(sb);
        return sb;
    }

    public void appendTestGroups(final StringBuilder sb) {
        appendTestGroups(sb, ',');
    }

    /**
     * Return a value indicating if the groups are empty
     * and should be represented by an empty-string
     * {@link #toString()}
     * @return
     */
    protected boolean isEmpty() {
        return proctorResult.getBuckets().isEmpty();
    }

    /**
     * Appends each group to the StringBuilder using the separator to delimit
     * group names. the separator should be appended for each group added
       * to the string builder
     *
     * {@link #toString()}
     * {@link #buildTestGroupString()} or {@link #appendTestGroups(StringBuilder)}
     * @param sb
     */
    public void appendTestGroups(final StringBuilder sb, char separator) {
        for (final Entry<String, TestBucket> entry : proctorResult.getBuckets().entrySet()) {
            final String testName = entry.getKey();
            final TestBucket testBucket = entry.getValue();
            if (testBucket.getValue() < 0) {
                continue;
            }
            sb.append(testName).append(testBucket.getValue()).append(separator);
        }
    }

    /**
     * Generates a Map that be serialized to JSON and used with
     * indeed.proctor.groups.init and
     * indeed.proctor.groups.inGroup(tstName, bucketValue)
     *
     * When we create a generated JavaScript representation of indeed.proctor.AbstractGroups,
     * this config can be updated for use in it's constructor.
     *
     * @return
     */
    public Map<String, Integer> getJavaScriptConfig() {
        // For now this is a simple mapping from {testName to bucketValue}
        final Map<String, Integer> groups = Maps.newHashMapWithExpectedSize(proctorResult.getBuckets().size());
        for (final Entry<String, TestBucket> entry : proctorResult.getBuckets().entrySet()) {
            final String testName = entry.getKey();
            final TestBucket testBucket = entry.getValue();
            // mirrors appendTestGroups method by skipping *inactive* tests
            if (testBucket.getValue() < 0) {
                continue;
            }
            groups.put(testName, testBucket.getValue());
        }
        return groups;
    }

    public ProctorResult getProctorResult() {
        return proctorResult;
    }
}
