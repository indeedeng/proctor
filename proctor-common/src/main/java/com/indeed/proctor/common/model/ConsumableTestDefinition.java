package com.indeed.proctor.common.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Models a single test
 * @author ketan
 */
public class ConsumableTestDefinition {
    @Nonnull
    private Map<String, Object> constants = Collections.emptyMap();
    private int version;
    @Nullable
    private String salt;
    @Nullable
    private String rule;
    @Nonnull
    private List<TestBucket> buckets = Collections.emptyList();
    @Nonnull
    private List<Allocation> allocations = Collections.emptyList();

    @Nonnull
    private TestType testType;
    @Nullable
    private String description;

    public ConsumableTestDefinition() { /* intentionally empty */ }

    public ConsumableTestDefinition(
            final int version,
            @Nullable final String rule,
            @Nonnull final TestType testType,
            @Nullable final String salt,
            @Nonnull final List<TestBucket> buckets,
            @Nonnull final List<Allocation> allocations,
            @Nonnull final Map<String, Object> constants,
            @Nullable final String description
    ) {
        this.constants = constants;
        this.version = version;
        this.salt = salt;
        this.rule = rule;
        this.buckets = buckets;
        this.allocations = allocations;
        this.testType = testType;
        this.description = description;
    }

    @Nonnull
    public Map<String, Object> getConstants() {
        return constants;
    }

    public void setConstants(@Nonnull final Map<String, Object> constants) {
        this.constants = constants;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(final int version) {
        this.version = version;
    }

    @Nullable
    public String getRule() {
        return rule;
    }

    public void setRule(@Nullable final String rule) {
        this.rule = rule;
    }

    @Nullable
    public String getSalt() {
        return salt;
    }

    public void setSalt(@Nullable final String salt) {
        this.salt = salt;
    }

    @Nonnull
    public List<TestBucket> getBuckets() {
        return buckets;
    }

    public void setBuckets(@Nonnull final List<TestBucket> buckets) {
        this.buckets = buckets;
    }

    @Nonnull
    public List<Allocation> getAllocations() {
        return allocations;
    }

    public void setAllocations(@Nonnull final List<Allocation> allocations) {
        this.allocations = allocations;
    }

    @Nonnull
    public TestType getTestType() {
        return testType;
    }

    public void setTestType(@Nonnull final TestType testType) {
        this.testType = testType;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable final String description) {
        this.description = description;
    }
}