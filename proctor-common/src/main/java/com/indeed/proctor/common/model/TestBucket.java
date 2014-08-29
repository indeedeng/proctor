package com.indeed.proctor.common.model;

import com.google.common.base.Strings;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Models a single bucket in a test, generally meant to have one bucket per varying behavior
 * @author ketan
 */
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class TestBucket {
    @Nonnull
    private String name = "";
    private int value;
    @Nullable
    private String description;
    @Nullable
    private Payload payload;

    public TestBucket() { /* intentionally empty */ }

    // For backward compatiblity with pre-payload code.
    public TestBucket(
            @Nonnull final String name,
            final int value,
            @Nullable final String description
    ) {
        this.name = name;
        this.value = value;
        this.description = description;
        this.payload = null;
    }

    public TestBucket(
            @Nonnull final String name,
            final int value,
            @Nullable final String description,
            @Nullable final Payload payload
    ) {
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

    public void setName(@Nonnull final String name) {
        this.name = Strings.nullToEmpty(name);
    }

    public int getValue() {
        return value;
    }

    public void setValue(final int value) {
        this.value = value;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable final String description) {
        this.description = description;
    }

    @Nullable
    public Payload getPayload() {
        return payload;
    }

    public void setPayload(@Nullable final Payload payload) {
        this.payload = payload;
    }

    @Nullable
    @Override
    public String toString() {
        return name + " = " + value + ((payload == null) ? "" : " "+payload);
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
}
