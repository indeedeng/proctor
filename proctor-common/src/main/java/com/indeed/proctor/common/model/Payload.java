package com.indeed.proctor.common.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.PayloadType;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Models a payload value for a bucket in a test, generally meant to have one kind of value per bucket.
 *
 * @author pwp
 *
 * NOTE: if you add a payload type here, also please add it to
 * proctor webapp's buckets.js indeed.proctor.editor.BucketsEditor.prototype.prettyPrintPayloadValue_
 */
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class Payload {
    @Nullable
    private Double doubleValue;
    @Nullable
    private Double[] doubleArray;
    @Nullable
    private Long longValue;
    @Nullable
    private Long[] longArray;
    @Nullable
    private String stringValue;
    @Nullable
    private String[] stringArray;
    @Nullable
    private Map<String, Object> map;
    // Used for returning something when we can't return a null.
    public static final Payload EMPTY_PAYLOAD = new Payload();
    // Error message for invalid user input
    public static final String PAYLOAD_OVERWRITE_EXCEPTION = "Expected all properties to be empty: ";

    public Payload(final String value) {
        this.stringValue = value;
    }

    public Payload(final Double value) {
        this.doubleValue = value;
    }

    public Payload(final Long value) {
        this.longValue = value;
    }

    public Payload(final Map<String, Object> value) {
        this.map = new HashMap<>(value);
    }

    public Payload(final String[] values) {
        this.stringArray = Arrays.copyOf(values, values.length);
    }

    public Payload(final Double[] values) {
        this.doubleArray = Arrays.copyOf(values, values.length);
    }

    public Payload(final Long[] values) {
        this.longArray = Arrays.copyOf(values, values.length);
    }

    public Payload() { /* intentionally empty */ }

    public Payload(@Nonnull final Payload other) {
        this.doubleValue = other.doubleValue;
        if (other.doubleArray != null) {
            this.doubleArray = Arrays.copyOf(other.doubleArray, other.doubleArray.length);
        }
        this.longValue = other.longValue;
        if (other.longArray != null) {
            this.longArray = Arrays.copyOf(other.longArray, other.longArray.length);
        }
        this.stringValue = other.stringValue;
        if (other.stringArray != null) {
            this.stringArray = Arrays.copyOf(other.stringArray, other.stringArray.length);
        }
        if (other.map != null) {
            this.map = new HashMap<>(other.map);
        }
    }


    @Nullable
    public Double getDoubleValue() {
        return doubleValue;
    }
    public void setDoubleValue(@Nullable final Double doubleValue) {
        precheckStateAllNull();
        this.doubleValue = doubleValue;
    }

    @Nullable
    public Double[] getDoubleArray() {
        return doubleArray;
    }
    public void setDoubleArray(@Nullable final Double[] doubleArray) {
        precheckStateAllNull();
        this.doubleArray = doubleArray;
    }

    @Nullable
    public Long getLongValue() {
        return longValue;
    }
    public void setLongValue(@Nullable final Long longValue) {
        precheckStateAllNull();
        this.longValue = longValue;
    }

    @Nullable
    public Long[] getLongArray() {
        return longArray;
    }
    public void setLongArray(@Nullable final Long[] longArray) {
        precheckStateAllNull();
        this.longArray = longArray;
    }

    @Nullable
    public String getStringValue() {
        return stringValue;
    }
    public void setStringValue(@Nullable final String stringValue) {
        precheckStateAllNull();
        this.stringValue = stringValue;
    }

    @Nullable
    public String[] getStringArray() {
        return stringArray;
    }
    public void setStringArray(@Nullable final String[] stringArray) {
        precheckStateAllNull();
        this.stringArray = stringArray;
    }

    @Nullable
    public Map<String, Object> getMap() {
        return map;
    }
    public void setMap(@Nullable final Map<String, Object> map) {
        precheckStateAllNull();
        this.map = map;
    }
    // Sanity check precondition for above setters
    private void precheckStateAllNull() throws IllegalStateException {
        if ((doubleValue != null) || (doubleArray != null)
            || (longValue != null) || (longArray != null)
            || (stringValue != null) || (stringArray != null)
            || (map != null)) {
            throw new IllegalStateException(PAYLOAD_OVERWRITE_EXCEPTION + this);
        }
    }

    @Nonnull
    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder(50).append('{');
        // careful of the autoboxing...
        if (map != null) {
            s.append(" map : [");
            for (final Map.Entry<String, Object> entry : map.entrySet()) {
                s.append('(').append(entry.getKey()).append(',').append(entry.getValue()).append(')');
            }
            s.append(']');
        }
        if (doubleValue != null) {
            s.append(" doubleValue : ").append(doubleValue);
        }
        if (doubleArray != null) {
            s.append(" doubleArray : [");
            s.append(StringUtils.join(doubleArray, ", "));
            s.append(']');
        }
        if (longValue != null) {
            s.append(" longValue : ").append(longValue);
        }
        if (longArray != null) {
            s.append(" longArray : [");
            s.append(StringUtils.join(longArray, ", "));
            s.append(']');
        }
        if (stringValue != null) {
            s.append(" stringValue : \"").append(stringValue).append('"');
        }
        if (stringArray != null) {
            s.append(" stringArray : [");
            if (stringArray.length > 0) {
                s.append('"');
                s.append(String.join("\", \"", stringArray));
                s.append('"');
            }
            s.append(']');
        }
        s.append(" }");
        return s.toString();
    }

    /**
     * infers payloadtype based on the value that is set.
     * @return payloadType unless emptyPayload
     */
    @Nonnull
    public Optional<PayloadType> fetchPayloadType() {
        return Optional.ofNullable(getPayloadType());
    }

    public static boolean hasType(final Payload payload, final PayloadType payloadType) {
        if (payload == null) {
            return false;
        }
        final Function<Payload, Object> resolver = resolvers.get(payloadType);
        return resolver != null && resolver.apply(payload) != null;
    }

    @CheckForNull
    private PayloadType getPayloadType() {
        return Stream.of(PayloadType.values())
                .filter(pt -> resolvers.get(pt).apply(this) != null)
                .findFirst()
                .orElse(null);
    }

    /**
     * @return the payload type as a string.  Used by Proctor Webapp.
     * @deprecated use fetchPayloadType
     */
    @Nonnull
    @Deprecated
    public String fetchType() {
        return fetchPayloadType()
                .map(t -> t.payloadTypeName)
                .orElse("none");
    }

    public boolean sameType(@Nullable final Payload other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }

        // Both this and that must have either null
        // or something filled in for each slot.
        return this.getPayloadType() == other.getPayloadType();
    }

    public int numFieldsDefined() {
        return (int) Stream.of(PayloadType.values())
                .filter(pt -> resolvers.get(pt).apply(this) != null)
                .count();
    }

    /**
     * @return "the" value of this Payload, stuffed into an Object.
     * This is used for evaluating the "validator" portion of a
     * PayloadSpecification against these Payloads.
     *
     * We don't want the JsonSerializer to know about this, so
     * renamed to not begin with "get".
     */
    @CheckForNull
    public Object fetchAValue() {
        return Stream.of(PayloadType.values())
                .map(pt -> resolvers.get(pt).apply(this))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean equals(final Object o) {
        /*
         * WARNING: Do not implement equals using Objects.equals for the arrays,
         * because new String[]{"a"}.equals(new String[]{"a"}) is false
         */
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Payload payload = (Payload) o;
        return Objects.equals(doubleValue, payload.doubleValue) &&
                Arrays.equals(doubleArray, payload.doubleArray) &&
                Objects.equals(longValue, payload.longValue) &&
                Arrays.equals(longArray, payload.longArray) &&
                Objects.equals(stringValue, payload.stringValue) &&
                Arrays.equals(stringArray, payload.stringArray) &&
                Objects.equals(map, payload.map);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(doubleValue, longValue, stringValue, map);
        result = 31 * result + Arrays.hashCode(doubleArray);
        result = 31 * result + Arrays.hashCode(longArray);
        result = 31 * result + Arrays.hashCode(stringArray);
        return result;
    }


    private final static Map<PayloadType, Function<Payload, Object>> resolvers = ImmutableMap.<PayloadType, Function<Payload, Object>>builder()
            .put(PayloadType.DOUBLE_VALUE, Payload::getDoubleValue)
            .put(PayloadType.DOUBLE_ARRAY, Payload::getDoubleArray)
            .put(PayloadType.LONG_VALUE, Payload::getLongValue)
            .put(PayloadType.LONG_ARRAY, Payload::getLongArray)
            .put(PayloadType.STRING_VALUE, Payload::getStringValue)
            .put(PayloadType.STRING_ARRAY, Payload::getStringArray)
            .put(PayloadType.MAP, Payload::getMap)
            .build();
}
