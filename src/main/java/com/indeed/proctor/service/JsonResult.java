package com.indeed.proctor.service;

import com.google.common.collect.Maps;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.TestBucket;

import java.util.Collections;
import java.util.Map;

/**
 * ProctorResult intended for JSON serialization for the /groups/identify method.
 */
public class JsonResult {
    // Map of test name to bucket assignment.
    final private Map<String, JsonTestBucket> groups;

    // Serialized context used to process this request.
    final private Map<String, Object> context;

    final private Audit audit;

    public JsonResult(final ProctorResult result, final Map<String, Object> context, final Audit audit) {
        this.context = context;
        this.audit = audit;

        groups = generateJsonBuckets(result);
    }

    private Map<String, JsonTestBucket> generateJsonBuckets(final ProctorResult result) {
        Map<String, JsonTestBucket> jsonBuckets = Maps.newHashMap();

        // As we process each TestBucket into a JsonBucket, we also need to obtain a version for that test.
        Map<String, Integer> versions = result.getTestVersions();

        for (Map.Entry<String, TestBucket> e : result.getBuckets().entrySet())
        {
            final String testName = e.getKey();
            final TestBucket testBucket = e.getValue();

            JsonTestBucket jsonBucket = new JsonTestBucket(testBucket, versions.get(testName));
            jsonBuckets.put(testName, jsonBucket);
        }

        return jsonBuckets;
    }

    public Map<String, JsonTestBucket> getGroups() {
        return groups;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public Audit getAudit() {
        return audit;
    }
}
