package com.indeed.proctor.common.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

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
    private Map<String,Object> map;
    // Used for returning something when we can't return a null.
    public static final Payload EMPTY_PAYLOAD = new Payload();

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
            this.map = Maps.newHashMap(other.map);
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
    public Map<String,Object> getMap() {
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
            throw new IllegalStateException("Expected all properties to be empty: " + this);
        }
    }

    @Nonnull
    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder(50).append('{');
        // careful of the autoboxing...
        if (map != null) {
            s.append(" map : [");
            for (final Map.Entry<String,Object> entry : map.entrySet()) {
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
     * @return the payload type as a string.  Used by Proctor Webapp.
     */
    @Nonnull
    public String fetchType() {
        if (doubleValue != null) {
            return "doubleValue";
        }
        if (doubleArray != null) {
            return "doubleArray";
        }
        if (longValue != null) {
            return "longValue";
        }
        if (longArray != null) {
            return "longArray";
        }
        if (stringValue != null) {
            return "stringValue";
        }
        if (stringArray != null) {
            return "stringArray";
        }
        if (map != null) {
            return "map";
        }
        return "none";
    }

    public boolean sameType(@Nullable final Payload that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }

        // Both this and that must have either null
        // or something filled in for each slot.
        return (((doubleValue == null) == (that.doubleValue == null))
                && ((doubleArray == null) == (that.doubleArray == null))
                && ((longValue == null) == (that.longValue == null))
                && ((longArray == null) == (that.longArray == null))
                && ((stringValue == null) == (that.stringValue == null))
                && ((stringArray == null) == (that.stringArray == null))
                && ((map == null) == (that.map == null)));
    }

    public int numFieldsDefined() {
        int i = 0;
        if (map != null) {
            i++;
        }
        if (doubleValue != null) {
            i++;
        }
        if (doubleArray != null) {
            i++;
        }
        if (longValue != null) {
            i++;
        }
        if (longArray != null) {
            i++;
        }
        if (stringValue != null) {
            i++;
        }
        if (stringArray != null) {
            i++;
        }
        return i;
    }

    /**
     * @return "the" value of this Payload, stuffed into an Object.
     * This is used for evaluating the "validator" portion of a
     * PayloadSpecification against these Payloads.
     *
     * We don't want the JsonSerializer to know about this, so
     * renamed to not begin with "get".
     */
    @Nullable
    public Object fetchAValue() {
        if (doubleValue != null) {
            return doubleValue;
        }
        if (doubleArray != null) {
            return doubleArray;
        }
        if (longValue != null) {
            return longValue;
        }
        if (longArray != null) {
            return longArray;
        }
        if (stringValue != null) {
            return stringValue;
        }
        if (stringArray != null) {
            return stringArray;
        }
        if (map != null) {
            return map;
        }
        return null;
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
}
