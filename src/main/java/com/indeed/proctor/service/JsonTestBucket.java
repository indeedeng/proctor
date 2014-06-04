package com.indeed.proctor.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.TestBucket;

/**
 * Representation of TestBucket intended for serialization into JSON.
 *
 * Mostly a rewriting of TestBucket with a few extras like skipping nulls and included test version for caching.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonTestBucket {
    private final String name;
    private final int value;
    private final Object payload;
    private final int version;

    /**
     * Serializes the object using an existing bucket and a separate version.
     *
     * Version needs to be obtained outside of the bucket through ProctorResult.getTestVersions()
     */
    public JsonTestBucket(final TestBucket bucket, final int version) {
        name = bucket.getName();
        value = bucket.getValue();
        this.version = version;

        // The json serializer will automatically make this into whatever json type it should be.
        // So we don't have to worry about figuring out the type of the payload.
        final Payload bucketPayload = bucket.getPayload();
        if (bucketPayload != null) {
            payload = bucket.getPayload().fetchAValue();
        } else {
            payload = null;
        }
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    public Object getPayload() {
        return payload;
    }

    public int getVersion() {
        return version;
    }
}
