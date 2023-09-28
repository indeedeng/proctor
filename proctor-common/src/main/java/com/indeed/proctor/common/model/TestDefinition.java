package com.indeed.proctor.common.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.emptyList;

/**
 * Models a single test
 *
 * @author ketan
 */
public class TestDefinition {

    /**
     * "-1" when for definitions on the trunk branch. On other branches, the revision of the
     * definition on the trunk branch from which it was promoted
     */
    private String version;

    @Nonnull private Map<String, Object> constants = Collections.emptyMap();
    @Nonnull private Map<String, Object> specialConstants = Collections.emptyMap();
    @Nonnull private String salt;
    @Nullable private String rule;
    @Nonnull private List<TestBucket> buckets = emptyList();
    //  there are multiple ways to allocate the buckets based on rules, but most tests will probably
    // just have one Allocation
    @Nonnull private List<Allocation> allocations = emptyList();
    private boolean silent;
    /**
     * Mutable tags used by applications for any kind of purpose (filtering, special treatments)
     * Validated by IdentifierValidationUtil
     */
    @Nonnull private List<String> metaTags = emptyList();

    /** For advisory purposes only */
    @Nonnull private TestType testType;

    @Nullable private String description;

    /** @see #getDependsOn() */
    @Nullable private TestDependency dependsOn;

    private boolean evaluteForIncognitoUsers;

    public TestDefinition() {
        /* intentionally empty */
    }

    /** @deprecated Use {@link #builder()} */
    @Deprecated
    public TestDefinition(
            final String version,
            @Nullable final String rule,
            @Nonnull final TestType testType,
            @Nonnull final String salt,
            @Nonnull final List<TestBucket> buckets,
            @Nonnull final List<Allocation> allocations,
            @Nonnull final Map<String, Object> constants,
            @Nonnull final Map<String, Object> specialConstants,
            @Nullable final String description) {
        this(
                version,
                rule,
                testType,
                salt,
                buckets,
                allocations,
                false,
                constants,
                specialConstants,
                description,
                emptyList());
    }

    /** @deprecated Use {@link #builder()} */
    @Deprecated
    public TestDefinition(
            final String version,
            @Nullable final String rule,
            @Nonnull final TestType testType,
            @Nonnull final String salt,
            @Nonnull final List<TestBucket> buckets,
            @Nonnull final List<Allocation> allocations,
            final boolean silent,
            @Nonnull final Map<String, Object> constants,
            @Nonnull final Map<String, Object> specialConstants,
            @Nullable final String description) {
        this(
                version,
                rule,
                testType,
                salt,
                buckets,
                allocations,
                silent,
                constants,
                specialConstants,
                description,
                emptyList());
    }

    /** @deprecated Use {@link #builder()} */
    @Deprecated
    public TestDefinition(
            final String version,
            @Nullable final String rule,
            @Nonnull final TestType testType,
            @Nonnull final String salt,
            @Nonnull final List<TestBucket> buckets,
            @Nonnull final List<Allocation> allocations,
            final boolean silent,
            @Nonnull final Map<String, Object> constants,
            @Nonnull final Map<String, Object> specialConstants,
            @Nullable final String description,
            @Nonnull final List<String> metaTags) {
        this.version = version;
        this.constants = constants;
        this.specialConstants = specialConstants;
        this.salt = salt;
        this.rule = rule;
        this.buckets = buckets;
        this.allocations = allocations;
        this.silent = silent;
        this.testType = testType;
        this.description = description;
        this.metaTags = metaTags;
        this.evaluteForIncognitoUsers = false;
    }

    public TestDefinition(@Nonnull final TestDefinition other) {
        this(builder().from(other));
    }

    private TestDefinition(@Nonnull final Builder builder) {
        version = builder.version;
        rule = builder.rule;
        testType = Objects.requireNonNull(builder.testType, "testType must be set");
        salt = Objects.requireNonNull(builder.salt, "salt must be set");
        buckets = builder.buckets.build();
        allocations = builder.allocations.build();
        silent = builder.silent;
        constants = builder.constants.build();
        specialConstants = builder.specialConstants.build();
        description = builder.description;
        metaTags = builder.metaTags.build();
        dependsOn = builder.dependsOn;
        evaluteForIncognitoUsers = builder.evaluteForIncognitoUsers;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getVersion() {
        return version;
    }

    /** @deprecated Use {@link #builder()} */
    @Deprecated
    public void setVersion(final String version) {
        this.version = version;
    }

    @Nonnull
    public Map<String, Object> getConstants() {
        return constants;
    }

    /** @deprecated Use {@link #builder()} */
    @Deprecated
    @SuppressWarnings("UnusedDeclaration")
    public void setConstants(@Nonnull final Map<String, Object> constants) {
        this.constants = constants;
    }

    @Nonnull
    public Map<String, Object> getSpecialConstants() {
        return specialConstants;
    }

    /** @deprecated Use {@link #builder()} */
    @Deprecated
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

    /** @deprecated Use {@link #builder()} */
    @Deprecated
    @SuppressWarnings("UnusedDeclaration")
    public void setRule(@Nullable final String rule) {
        this.rule = rule;
    }

    @Nonnull
    public String getSalt() {
        return salt;
    }

    /** @deprecated Use {@link #builder()} */
    @Deprecated
    @SuppressWarnings("UnusedDeclaration")
    public void setSalt(@Nonnull final String salt) {
        this.salt = salt;
    }

    @Nonnull
    public List<TestBucket> getBuckets() {
        return buckets;
    }

    /** @deprecated Use {@link #builder()} */
    @Deprecated
    @SuppressWarnings("UnusedDeclaration")
    public void setBuckets(@Nonnull final List<TestBucket> buckets) {
        this.buckets = buckets;
    }

    @Nonnull
    public List<Allocation> getAllocations() {
        return allocations;
    }

    /** @deprecated Use {@link #builder()} */
    @Deprecated
    @SuppressWarnings("UnusedDeclaration")
    public void setAllocations(@Nonnull final List<Allocation> allocations) {
        this.allocations = allocations;
    }

    /** @deprecated Use {@link #builder()} */
    @Deprecated
    public void setSilent(final boolean silent) {
        this.silent = silent;
    }

    public boolean getSilent() {
        return silent;
    }

    @Nonnull
    public TestType getTestType() {
        return testType;
    }

    /** @deprecated Use {@link #builder()} */
    @Deprecated
    @SuppressWarnings("UnusedDeclaration")
    public void setTestType(final TestType testType) {
        this.testType = testType;
    }

    /** @deprecated Use {@link #builder()} */
    @Deprecated
    public void setDescription(final String description) {
        this.description = description;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    /** metaTags allow to group and filter tests. */
    @Nonnull
    public List<String> getMetaTags() {
        return this.metaTags;
    }

    /** @deprecated Use {@link #builder()} */
    @Deprecated
    public void setMetaTags(final List<String> metaTags) {
        this.metaTags = metaTags;
    }

    /**
     * Dependency to activate this test. This test won't be evaluated if the dependency condition
     * isn't satisfied.
     */
    @Nullable
    public TestDependency getDependsOn() {
        return dependsOn;
    }

    /** @deprecated Use {@link #builder()} */
    @Deprecated
    public void setDependsOn(@Nullable final TestDependency dependsOn) {
        this.dependsOn = dependsOn;
    }

    public boolean getEvaluteForIncognitoUsers() {
        return evaluteForIncognitoUsers;
    }

    @Override
    public String toString() {
        return "TestDefinition{"
                + "version='"
                + version
                + '\''
                + ", constants="
                + constants
                + ", specialConstants="
                + specialConstants
                + ", salt='"
                + salt
                + '\''
                + ", rule='"
                + rule
                + '\''
                + ", buckets="
                + buckets
                + ", allocations="
                + allocations
                + ", silent="
                + silent
                + ", testType="
                + testType
                + ", description='"
                + description
                + '\''
                + ", metaTags="
                + metaTags
                + ", dependsOn="
                + dependsOn
                + ", evaluteForIncognitoUsers="
                + evaluteForIncognitoUsers
                + '}';
    }

    @Override
    public int hashCode() {
        // because TestBuckets.hashCode() only considers name for unknown reasons, need to use
        // testBuckets.fullHashCode()
        final List<Object> bucketWrappers = new ArrayList<>();
        if (buckets != null) {
            for (final TestBucket bucket : buckets) {
                bucketWrappers.add(
                        new Object() {
                            @Override
                            public int hashCode() {
                                return bucket.fullHashCode();
                            }
                        });
            }
        }
        return Objects.hash(
                version,
                constants,
                specialConstants,
                salt,
                rule,
                bucketWrappers,
                allocations,
                silent,
                testType,
                description,
                metaTags,
                dependsOn,
                evaluteForIncognitoUsers);
    }

    /**
     * similar to generated equals() method, but special treatment of buckets, because testBucket
     * has unconventional equals/hashcode implementation for undocumented reason.
     *
     * <p>Difference is checked by Unit test.
     */
    @Override
    public boolean equals(final Object otherDefinition) {
        if (this == otherDefinition) {
            return true;
        }
        if (otherDefinition == null || getClass() != otherDefinition.getClass()) {
            return false;
        }
        final TestDefinition that = (TestDefinition) otherDefinition;
        return silent == that.silent
                && Objects.equals(version, that.version)
                && Objects.equals(constants, that.constants)
                && Objects.equals(specialConstants, that.specialConstants)
                && Objects.equals(salt, that.salt)
                && Objects.equals(rule, that.rule)
                && bucketListEqual(buckets, that.buckets)
                && // difference here
                Objects.equals(allocations, that.allocations)
                && Objects.equals(testType, that.testType)
                && Objects.equals(description, that.description)
                && Objects.equals(metaTags, that.metaTags)
                && Objects.equals(dependsOn, that.dependsOn)
                && Objects.equals(evaluteForIncognitoUsers, that.evaluteForIncognitoUsers);
    }

    @VisibleForTesting
    static boolean bucketListEqual(
            final List<TestBucket> bucketsA, final List<TestBucket> bucketsB) {
        if (bucketsA == bucketsB) {
            return true;
        }
        // TestBucket Equal returns true too often, but false means false. This also handles
        // single-sided null cases and different list size.
        if (!Objects.equals(bucketsA, bucketsB)) {
            return false;
        }
        final Iterator<TestBucket> itA = bucketsA.iterator();
        final Iterator<TestBucket> itB = bucketsB.iterator();
        while (itA.hasNext() && itB.hasNext()) {
            final TestBucket bucketA = itA.next();
            final TestBucket bucketB = itB.next();
            if ((bucketA != null) && !bucketA.fullEquals(bucketB)) {
                return false;
            }
        }
        return true;
    }

    public static class Builder {
        private String version;
        private String rule;
        private TestType testType;
        private String salt;
        private ImmutableList.Builder<TestBucket> buckets = ImmutableList.builder();
        private ImmutableList.Builder<Allocation> allocations = ImmutableList.builder();
        private boolean silent;
        private ImmutableMap.Builder<String, Object> constants = ImmutableMap.builder();
        private ImmutableMap.Builder<String, Object> specialConstants = ImmutableMap.builder();
        private String description;
        private ImmutableList.Builder<String> metaTags = ImmutableList.builder();
        private TestDependency dependsOn;
        private boolean evaluteForIncognitoUsers;

        public Builder from(@Nonnull final TestDefinition other) {
            setVersion(other.version);
            setRule(other.rule);
            setTestType(other.testType);
            setSalt(other.salt);
            setBuckets(other.buckets);
            setAllocations(other.allocations);
            setSilent(other.silent);
            setConstants(other.constants);
            setSpecialConstants(other.specialConstants);
            setDescription(other.description);
            setMetaTags(other.metaTags);
            setDependsOn(other.dependsOn);
            setEvaluteForIncognitoUsers(other.evaluteForIncognitoUsers);
            return this;
        }

        public Builder setVersion(@Nullable final String version) {
            this.version = version;
            return this;
        }

        public Builder setRule(@Nullable final String rule) {
            this.rule = rule;
            return this;
        }

        public Builder setTestType(@Nonnull final TestType testType) {
            this.testType = Objects.requireNonNull(testType);
            return this;
        }

        public Builder setSalt(@Nonnull final String salt) {
            this.salt = Objects.requireNonNull(salt);
            return this;
        }

        public Builder setBuckets(@Nonnull final Iterable<TestBucket> buckets) {
            this.buckets = ImmutableList.builder();
            return addAllBuckets(buckets);
        }

        public Builder addBuckets(@Nonnull final TestBucket... buckets) {
            this.buckets.add(buckets);
            return this;
        }

        public Builder addAllBuckets(@Nonnull final Iterable<TestBucket> buckets) {
            this.buckets.addAll(buckets);
            return this;
        }

        public Builder setAllocations(@Nonnull final Iterable<Allocation> allocations) {
            this.allocations = ImmutableList.builder();
            return addAllAllocations(allocations);
        }

        public Builder addAllocations(@Nonnull final Allocation... allocations) {
            this.allocations.add(allocations);
            return this;
        }

        public Builder addAllAllocations(@Nonnull final Iterable<Allocation> allocations) {
            this.allocations.addAll(allocations);
            return this;
        }

        public Builder setSilent(final boolean silent) {
            this.silent = silent;
            return this;
        }

        public Builder setConstants(@Nonnull final Map<String, Object> constants) {
            this.constants = ImmutableMap.builder();
            return putAllConstants(constants);
        }

        public Builder putAllConstants(@Nonnull final Map<String, Object> constants) {
            this.constants.putAll(constants);
            return this;
        }

        public Builder setSpecialConstants(@Nonnull final Map<String, Object> specialConstants) {
            this.specialConstants = ImmutableMap.builder();
            return putAllSpecialConstants(specialConstants);
        }

        public Builder putAllSpecialConstants(@Nonnull final Map<String, Object> specialConstants) {
            this.specialConstants.putAll(specialConstants);
            return this;
        }

        public Builder setDescription(@Nullable final String description) {
            this.description = description;
            return this;
        }

        public Builder setMetaTags(@Nonnull final Iterable<String> metaTags) {
            this.metaTags = ImmutableList.builder();
            return addAllMetaTags(metaTags);
        }

        public Builder addAllMetaTags(@Nonnull final Iterable<String> metaTags) {
            this.metaTags.addAll(metaTags);
            return this;
        }

        public Builder setDependsOn(@Nullable final TestDependency dependsOn) {
            this.dependsOn = dependsOn;
            return this;
        }

        public Builder setEvaluteForIncognitoUsers(final boolean evaluteForIncognitoUsers) {
            this.evaluteForIncognitoUsers = evaluteForIncognitoUsers;
            return this;
        }

        public TestDefinition build() {
            return new TestDefinition(this);
        }
    }
}
