package com.indeed.proctor.pipet.core.model;

import com.google.common.annotations.VisibleForTesting;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.ConsumableTestDefinition;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * ProctorResult intended for JSON serialization for the /groups/identify method.
 */
public class JsonResult {
    // Map of test name to bucket assignment.
    private final Map<String, JsonTestBucket> groups;

    // Serialized context used to process this request.
    private final Map<String, Object> context;

    private final Audit audit;

    public JsonResult(final ProctorResult result,
                      final Map<String, Object> context,
                      final Audit audit) {
        this.context = context;
        this.audit = audit;

        groups = generateJsonBuckets(result);
    }


    @VisibleForTesting
    static Map<String, JsonTestBucket> generateJsonBuckets(final ProctorResult result) {
        final Map<String, ConsumableTestDefinition> definitions = result.getTestDefinitions();
        // As we process each TestBucket into a JsonBucket, we also need to obtain a version for that test.
        return result.getBuckets().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> new JsonTestBucket(
                                e.getValue(),
                                definitions.get(e.getKey()).getVersion())));
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
