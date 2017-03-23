package com.indeed.proctor.common;

import com.indeed.proctor.common.model.Payload;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * enum for dealing with Payload types.  Used in code generation for
 * mapping between payload type names, java class names, and accessor names.
 *
 * @author pwp
 */
public enum PayloadType {
    DOUBLE_VALUE ("doubleValue", "Double", "number", "getDoubleValue"),
    DOUBLE_ARRAY ("doubleArray", "Double[]", "Array.<number>", "getDoubleArray"),
    LONG_VALUE   ("longValue", "Long", "number", "getLongValue"),
    LONG_ARRAY   ("longArray", "Long[]", "Array.<number>", "getLongArray"),
    STRING_VALUE ("stringValue", "String", "string", "getStringValue"),
    STRING_ARRAY ("stringArray", "String[]", "Array.<string>", "getStringArray"),
    MAP ("map", "Map<String,Object>", "Object.<string, Object>", "getMap");

    @Nonnull
    public final String payloadTypeName;
    @Nonnull
    public final String javaClassName;
    @Nonnull
    public final String javascriptTypeName;
    @Nonnull
    public final String javaAccessorName;

    private PayloadType(
            @Nonnull final String payloadTypeName,
            @Nonnull final String javaClassName,
            @Nonnull final String javascriptTypeName,
            @Nonnull final String javaAccessorName
    ) {
        this.payloadTypeName = payloadTypeName;
        this.javaClassName = javaClassName;
        this.javascriptTypeName = javascriptTypeName;
        this.javaAccessorName = javaAccessorName;
    }

    /**
     * See whether a given Payload has a specified type.  This could also
     * be done with introspection, and might belong in the Payload bean
     * itself.
     *
     * @param payload A payload
     * @return true if the payload has the type
     */
    public boolean payloadHasThisType(@Nullable Payload payload) {
        if (payload == null) {
            return false;
        }
        switch (this) {
            case DOUBLE_VALUE:
                return (payload.getDoubleValue() != null);
            case DOUBLE_ARRAY:
                return (payload.getDoubleArray() != null);
            case LONG_VALUE:
                return (payload.getLongValue() != null);
            case LONG_ARRAY:
                return (payload.getLongArray() != null);
            case STRING_VALUE:
                return (payload.getStringValue() != null);
            case STRING_ARRAY:
                return (payload.getStringArray() != null);
            case MAP:
                return (payload.getMap() != null);
            default:
                throw new IllegalStateException("Unknown payload type: " + this);
        }
    }

    public String getDefaultJavascriptValue() {
        switch (this) {
            case DOUBLE_VALUE:
            case LONG_VALUE:
                return "-1";
            case STRING_VALUE:
                return "''";
            case DOUBLE_ARRAY:
            case LONG_ARRAY:
            case STRING_ARRAY:
                return "[]";
            case MAP:
                return "{}";
            default:
                throw new IllegalStateException("Unknown payload type: " + this);
        }
    }

    /**
     * Given a Payload field name, return its corresponding PayloadType.
     *
     * @param payloadTypeName a string of payload type
     * @return null if it isn't an existing field name.
     */
    @Nonnull
    public static PayloadType payloadTypeForName(@Nonnull final String payloadTypeName)
            throws IllegalArgumentException {
        for (PayloadType p : PayloadType.values()) {
            if (payloadTypeName.equals(p.payloadTypeName)) {
                return p;
            }
        }
        throw new IllegalArgumentException("Payload type name " + payloadTypeName + " is not in the list of standard values: " + PayloadType.allTypeNames().toString());
    }

    /**
     * Flexible method for determining payload type from a value - helpful for
     * determining the schema of map payload types
     *
     * @param payloadValue a payload value
     * @return the payload type determined from the given value
     */
    @Nonnull
    public static PayloadType payloadTypeForValue(@Nonnull final Object payloadValue) throws IllegalArgumentException {
        if(payloadValue instanceof List) {
            if(((List)payloadValue).size() > 0) {
                final Object firstValue = ((List) payloadValue).get(0);
                if (firstValue instanceof Long || firstValue instanceof Integer) {
                    return PayloadType.LONG_ARRAY;
                } else if (firstValue instanceof Double || firstValue instanceof Float) {
                    return PayloadType.DOUBLE_ARRAY;
                } else if (firstValue instanceof String) {
                    return PayloadType.STRING_ARRAY;
                }
                throw new IllegalArgumentException("Cannot determine array-type from " + firstValue);
            } else {
                throw new IllegalArgumentException("No items in payload List, cannot determine type");
            }
        } else if (payloadValue instanceof Long || payloadValue instanceof Integer) {
            return PayloadType.LONG_VALUE;
        } else if (payloadValue instanceof Double || payloadValue instanceof Float) {
            return PayloadType.DOUBLE_VALUE;
        } else if (payloadValue instanceof String) {
            return PayloadType.STRING_VALUE;
        } else if (payloadValue instanceof String[]) {
            return PayloadType.STRING_ARRAY;
        } else if (payloadValue instanceof Long[] || payloadValue instanceof Integer[]) {
            return PayloadType.LONG_ARRAY;
        } else if (payloadValue instanceof Double[] || payloadValue instanceof Float[]) {
            return PayloadType.DOUBLE_ARRAY;
        } else if (payloadValue instanceof Map) {
            return PayloadType.MAP;
        }
        throw new IllegalArgumentException("Payload value " + payloadValue.getClass().getSimpleName() + " : " + payloadValue + "  does not correspond to a payload type");
    }

    /**
     * For printing useful error messages, the proctor webapp, and for testing.

     * @return all types name
     */
    @Nonnull
    public static List<String> allTypeNames() {
        final List<String> names = new ArrayList<String>();
        for (PayloadType p : PayloadType.values()) {
            names.add(p.payloadTypeName);
        }
        return names;
    }
}
