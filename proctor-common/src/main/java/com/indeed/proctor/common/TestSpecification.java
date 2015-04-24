package com.indeed.proctor.common;

import com.google.common.base.Preconditions;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
public class TestSpecification {
    private int fallbackValue = -1;
    @Nonnull
    private Map<String, Integer> buckets = Collections.emptyMap();
    @Nullable
    private PayloadSpecification payload;

    private String description;

    public int getFallbackValue() {
        return fallbackValue;
    }

    public void setFallbackValue(final int fallbackValue) {
        this.fallbackValue = fallbackValue;
    }

    @Nonnull
    public Map<String, Integer> getBuckets() {
        return buckets;
    }

    public void setBuckets(@Nonnull final Map<String, Integer> buckets) {
        this.buckets = Preconditions.checkNotNull(buckets, "Missing buckets");
    }


    public String getDescription(){
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @Nullable
    public PayloadSpecification getPayload() {
        return payload;
    }

    public void setPayload(@Nullable final PayloadSpecification payload) {
        this.payload = payload;
    }
}
