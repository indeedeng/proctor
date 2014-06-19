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
public class TestDefinition {
    private String version;
    @Nonnull
    private Map<String, Object> constants = Collections.emptyMap();
    @Nonnull
    private Map<String, Object> specialConstants = Collections.emptyMap();
    @Nonnull
    private String salt;
    @Nullable
    private String rule;
    @Nonnull
    private List<TestBucket> buckets = Collections.emptyList();
    //  there are multiple ways to allocate the buckets based on rules, but most tests will probably just have one Allocation
    @Nonnull
    private List<Allocation> allocations = Collections.emptyList();

    /**
     * For advisory purposes only
     */
    @Nonnull
    private TestType testType;
    @Nullable
    private String description;

    public TestDefinition() { /* intentionally empty */ }

    public TestDefinition(
            final String version,
            @Nullable final String rule,
            @Nonnull final TestType testType,
            @Nonnull final String salt,
            @Nonnull final List<TestBucket> buckets,
            @Nonnull final List<Allocation> allocations,
            @Nonnull final Map<String, Object> constants,
            @Nonnull final Map<String, Object> specialConstants,
            @Nullable final String description
    ) {
        this.version = version;
        this.constants = constants;
        this.specialConstants = specialConstants;
        this.salt = salt;
        this.rule = rule;
        this.buckets = buckets;
        this.allocations = allocations;
        this.testType = testType;
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    @Nonnull
    public Map<String, Object> getConstants() {
        return constants;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setConstants(@Nonnull final Map<String, Object> constants) {
        this.constants = constants;
    }

    @Nonnull
    public Map<String, Object> getSpecialConstants() {
        return specialConstants;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setSpecialConstants(@Nonnull final Map<String, Object> specialConstants) {
        this.specialConstants = specialConstants;
    }

    @Nullable
    public String getRule() {
        return rule;
    }

    @SuppressWarnings("UnusedDeclaration")
    @Deprecated()
    /**
     * Provided only for backwards-compatibility when parsing JSON data that still uses 'subrule'
     */
    public void setSubrule(@Nullable final String subrule) {
        setRule(subrule);
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setRule(@Nullable final String rule) {
        this.rule = rule;
    }


    @Nonnull
    public String getSalt() {
        return salt;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setSalt(@Nonnull final String salt) {
        this.salt = salt;
    }

    @Nonnull
    public List<TestBucket> getBuckets() {
        return buckets;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setBuckets(@Nonnull final List<TestBucket> buckets) {
        this.buckets = buckets;
    }

    @Nonnull
    public List<Allocation> getAllocations() {
        return allocations;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setAllocations(@Nonnull final List<Allocation> allocations) {
        this.allocations = allocations;
    }

    @Nonnull
    public TestType getTestType() {
        return testType;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setTestType(final TestType testType) {
        this.testType = testType;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    @Nullable
    public String getDescription() {
        return description;
    }
}
