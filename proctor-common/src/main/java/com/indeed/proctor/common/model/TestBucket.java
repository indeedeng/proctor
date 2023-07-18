package com.indeed.proctor.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.indeed.proctor.common.ProctorResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Models a single bucket in a test, generally meant to have one bucket per varying behavior
 *
 * @author ketan
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = TestBucket.Builder.class)
public class TestBucket {
    /** Validated by IdentifierValidationUtil */
    @Nonnull private String name = "";

    private int value;
    @Nullable private String description;
    @Nullable private Payload payload;

    /**
     * @deprecated Use {@link TestBucket#TestBucket(String, int, String, Payload)} or {@link
     *     TestBucket#builder()} to construct an instance.
     */
    @Deprecated
    public TestBucket() {
        /* intentionally empty */
    }

    // For backward compatiblity with pre-payload code.
    public TestBucket(
            @Nonnull final String name, final int value, @Nullable final String description) {
        this.name = name;
        this.value = value;
        this.description = description;
        this.payload = null;
    }

    public TestBucket(
            @Nonnull final String name,
            final int value,
            @Nullable final String description,
            @Nullable final Payload payload) {
        this.name = name;
        this.value = value;
        this.description = description;
        this.payload = payload;
    }

    public TestBucket(@Nonnull final TestBucket other) {
        this.name = other.name;
        this.value = other.value;
        this.description = other.description;
        if (other.payload != null) {
            this.payload = new Payload(other.payload);
        }
    }

    @Nonnull
    public String getName() {
        return name;
    }

    /**
     * @deprecated Use {@link TestBucket#builder()} and {@link Builder#name} instead. Using setter
     *     of bucket is a possible cause of a major bug that invalidate A/B testing result because
     *     two {@link ProctorResult} share same object. This will be removed in a future release.
     */
    @Deprecated
    public void setName(@Nonnull final String name) {
        this.name = Strings.nullToEmpty(name);
    }

    public int getValue() {
        return value;
    }

    /**
     * @deprecated Use {@link TestBucket#builder()} and {@link Builder#value} instead. Using setter
     *     of bucket is a possible cause of a major bug that invalidate A/B testing result because
     *     two {@link ProctorResult} share same object. This will be removed in a future release.
     */
    @Deprecated
    public void setValue(final int value) {
        this.value = value;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    /**
     * @deprecated Use {@link TestBucket#builder()} and {@link Builder#description} instead. Using
     *     setter of bucket is a possible cause of a major bug that invalidate A/B testing result
     *     because two {@link ProctorResult} share same object. This will be removed in a future
     *     release.
     */
    @Deprecated
    public void setDescription(@Nullable final String description) {
        this.description = description;
    }

    @Nullable
    public Payload getPayload() {
        return payload;
    }

    /**
     * @deprecated Use {@link TestBucket#builder()} and {@link Builder#payload} instead. Using
     *     setter of bucket is a possible cause of a major bug that invalidate A/B testing result
     *     because two {@link ProctorResult} share same object. This will be removed in a future
     *     release.
     */
    @Deprecated
    public void setPayload(@Nullable final Payload payload) {
        this.payload = payload;
    }

    @Nullable
    @Override
    public String toString() {
        return name + " = " + value + ((payload == null) ? "" : " " + payload);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(@Nullable final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return name.equals(((TestBucket) obj).name);
    }

    /*
     * because TestBucket.equals() only compares name for unknown reasons,
     * comparing all other values here
     */
    boolean fullEquals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TestBucket that = (TestBucket) o;
        return value == that.value
                && Objects.equal(name, that.name)
                && Objects.equal(description, that.description)
                && Objects.equal(payload, that.payload);
    }

    public int fullHashCode() {
        return Objects.hashCode(name, value, description, payload);
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        @Nonnull private String name = "";
        private int value;
        @Nullable private String description;
        @Nullable private Payload payload;

        private Builder() {}

        public Builder from(@Nonnull final TestBucket bucket) {
            this.name = bucket.name;
            this.value = bucket.value;
            this.description = bucket.description;
            this.payload = bucket.payload;
            return this;
        }

        public Builder name(@Nonnull final String name) {
            this.name = Strings.nullToEmpty(name);
            return this;
        }

        public Builder value(final int value) {
            this.value = value;
            return this;
        }

        public Builder description(@Nullable final String description) {
            this.description = description;
            return this;
        }

        public Builder payload(@Nullable final Payload payload) {
            this.payload = payload;
            return this;
        }

        public TestBucket build() {
            return new TestBucket(this.name, this.value, this.description, this.payload);
        }
    }
}
