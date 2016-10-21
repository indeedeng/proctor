package com.indeed.proctor.pipet.core.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.TestBucket;

/**
 * Representation of TestBucket intended for serialization into JSON.
 *
 * Mostly a rewriting of TestBucket with a few extras like skipping nulls and included test version for caching.
 */
public class JsonTestBucket {
    private final String name;
    private final int value;
    private final Payload payload;
    private final String version;

    /**
     * Serializes the object using an existing bucket and a separate version.
     *
     * Version needs to be obtained outside of the bucket through ProctorResult.getTestVersions()
     */
    public JsonTestBucket(final TestBucket bucket, final String version) {
        name = bucket.getName();
        value = bucket.getValue();
        this.version = version;

        // This means the JSON output will have type names like "stringValue" and "doubleArray".
        // It makes the API look less clean, especially for clients that use duck-typed languages.
        // But it may make deserialization easier for clients with rigid types, especially if they use something like
        // Jackson's data binding in Java.
        // This is also consistent with the test matrix definition.
        payload = bucket.getPayload();
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    // Many tests do not use payloads, so we should just omit this field if it's not set.
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    public Object getPayload() {
        return payload;
    }

    public String getVersion() {
        return version;
    }
}
