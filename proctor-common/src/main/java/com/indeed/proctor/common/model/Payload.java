package com.indeed.proctor.common.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;

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
        final StringBuilder s = new StringBuilder("{");
        // careful of the autoboxing...
        if (map != null) {
            s.append(" map : [");
            for(Map.Entry<String,Object> Entry : map.entrySet()) {
                s.append("(" + Entry.getKey() + "," + Entry.getValue() + ")");
            }
            s.append("]");
        }
        if (doubleValue != null) {
            s.append(" doubleValue : ").append(doubleValue);
        }
        if (doubleArray != null) {
            s.append(" doubleArray : [");
            Joiner.on(", ").appendTo(s, doubleArray);
            s.append("]");
        }
        if (longValue != null) {
            s.append(" longValue : ").append(longValue);
        }
        if (longArray != null) {
            s.append(" longArray : [");
            Joiner.on(", ").appendTo(s, longArray);
            s.append("]");
        }
        if (stringValue != null) {
            s.append(" stringValue : \"").append(stringValue).append('"');
        }
        if (stringArray != null) {
            s.append(" stringArray : [");
            if (stringArray.length > 0) {
                s.append('"');
                Joiner.on("\", \"").appendTo(s, stringArray);
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Payload payload = (Payload) o;

        if (map != null ? !map.equals(payload.map) : payload.map != null) return false;
        if (!Arrays.equals(doubleArray, payload.doubleArray)) return false;
        if (doubleValue != null ? !doubleValue.equals(payload.doubleValue) : payload.doubleValue != null) return false;
        if (!Arrays.equals(longArray, payload.longArray)) return false;
        if (longValue != null ? !longValue.equals(payload.longValue) : payload.longValue != null) return false;
        if (!Arrays.equals(stringArray, payload.stringArray)) return false;
        if (stringValue != null ? !stringValue.equals(payload.stringValue) : payload.stringValue != null) return false;
        if (map != null ? !map.equals(payload.map) : payload.map != null) return false;

        return true;
    }
}
