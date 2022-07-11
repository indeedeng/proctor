package com.indeed.proctor.common;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.model.Payload;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Options to determine how to force groups in Proctor.
 * Usually this is parsed from a string value from a URL parameter.
 *
 * @see ForceGroupsOptionsStrings
 */
public class ForceGroupsOptions {
    private static final ForceGroupsOptions EMPTY = ForceGroupsOptions.builder().build();

    private final Map<String, Integer> forceGroups;
    private final Map<String, Payload> forcePayloads;
    private final ForceGroupsDefaultMode defaultMode;

    private ForceGroupsOptions(final Builder builder) {
        this.forceGroups = ImmutableMap.copyOf(builder.forceGroups);
        this.forcePayloads = ImmutableMap.copyOf(builder.forcePayloads);
        this.defaultMode = builder.defaultMode;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ForceGroupsOptions empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return equals(EMPTY);
    }

    /**
     * Returns all bucket values to force assignment.
     */
    public Map<String, Integer> getForceGroups() {
        return forceGroups;
    }

    public Map<String, Payload> getForcePayloads() {
        return forcePayloads;
    }

    /**
     * Returns bucket value of a test to force assignment. Empty if no force is given.
     */
    public Optional<Integer> getForcedBucketValue(final String testName) {
        return Optional.ofNullable(forceGroups.get(testName));
    }

    /**
     * Returns bucket value of a test to force assignment. Empty if no force is given.
     */
    public Optional<Payload> getForcedPayloadValue(final String testName) {
        return Optional.ofNullable(forcePayloads.get(testName));
    }

    /**
     * Returns the default mode that defines what bucket to force when it's not explicitly given.
     */
    public ForceGroupsDefaultMode getDefaultMode() {
        return defaultMode;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        final ForceGroupsOptions that = (ForceGroupsOptions) o;
        return Objects.equals(forceGroups, that.forceGroups) && (defaultMode == that.defaultMode) && Objects.equals(forcePayloads, that.forcePayloads);
    }

    @Override
    public int hashCode() {
        return Objects.hash(forceGroups, defaultMode);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ForceGroupsOptions.class.getSimpleName() + "[", "]")
                .add("forceGroups=" + forceGroups)
                .add("forcePayloads=" + forcePayloads)
                .add("defaultMode=" + defaultMode)
                .toString();
    }

    public static class Builder {
        private final Map<String, Integer> forceGroups = new HashMap<>();
        private final Map<String, Payload> forcePayloads = new HashMap<>();
        private ForceGroupsDefaultMode defaultMode = ForceGroupsDefaultMode.getInitial();

        public Builder putForceGroup(final String testName, final int bucketValue) {
            forceGroups.put(testName, bucketValue);
            return this;
        }

        public Builder putAllForceGroups(final Map<? extends String, ? extends Integer> values) {
            forceGroups.putAll(values);
            return this;
        }

        public Builder putForcePayload(final String testName, final Payload payloadValue) {
            forcePayloads.put(testName, payloadValue);
            return this;
        }

        public Builder putAllForcePayloads(final Map<? extends String, ? extends Payload> values) {
            forcePayloads.putAll(values);
            return this;
        }

        public Builder setDefaultMode(final ForceGroupsDefaultMode defaultMode) {
            this.defaultMode = defaultMode;
            return this;
        }

        public ForceGroupsOptions build() {
            return new ForceGroupsOptions(this);
        }
    }
}
