package com.indeed.proctor.common;

import com.indeed.proctor.common.model.Payload;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * enum for dealing with Payload types.  Used in code generation for
 * mapping between payload type names, java class names, and accessor names.
 *
 * @author pwp
 */
public enum PayloadType {
    DOUBLE_VALUE(
            "doubleValue",
            "Double",
            "number",
            "-1",
            "getDoubleValue",
            false),
    DOUBLE_ARRAY(
            "doubleArray",
            "Double[]",
            "Array.<number>",
            "[]",
            "getDoubleArray",
            true),
    LONG_VALUE(
            "longValue",
            "Long",
            "number",
            "-1",
            "getLongValue",
            false),
    LONG_ARRAY(
            "longArray",
            "Long[]",
            "Array.<number>",
            "[]",
            "getLongArray",
            true),
    STRING_VALUE(
            "stringValue",
            "String",
            "string",
            "''",
            "getStringValue",
            false),
    STRING_ARRAY(
            "stringArray",
            "String[]",
            "Array.<string>",
            "[]",
            "getStringArray",
            true),
    MAP(
            "map",
            "Map<String,Object>",
            "Object.<string, Object>",
            "{}",
            "getMap",
            false);

    @Nonnull
    public final String payloadTypeName;
    @Nonnull
    public final String javaClassName;
    @Nonnull
    public final String javascriptTypeName;
    @Nonnull
    private final String javascriptDefaultValue;
    @Nonnull
    public final String javaAccessorName;
    private final boolean isArraysType;

    PayloadType(
            @Nonnull final String payloadTypeName,
            @Nonnull final String javaClassName,
            @Nonnull final String javascriptTypeName,
            @Nonnull String javascriptDefaultValue,
            @Nonnull final String javaAccessorName,
            boolean isArraysType
    ) {
        this.payloadTypeName = payloadTypeName;
        this.javaClassName = javaClassName;
        this.javascriptTypeName = javascriptTypeName;
        this.javascriptDefaultValue = javascriptDefaultValue;
        this.javaAccessorName = javaAccessorName;
        this.isArraysType = isArraysType;
    }

    /**
     * See whether a given Payload has a specified type.  This could also
     * be done with introspection, and might belong in the Payload bean
     * itself.
     *
     * @param payload A payload
     * @return true if the payload has the type
     * @deprecated use Payload.hasType
     */
    @Deprecated
    public boolean payloadHasThisType(@Nullable Payload payload) {
        return Payload.hasType(payload, this);
    }

    public String getDefaultJavascriptValue() {
        return javascriptDefaultValue;
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
        for (final PayloadType p : PayloadType.values()) {
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
     * @throws IllegalArgumentException if type cannot be determined
     */
    @Nonnull
    public static PayloadType payloadTypeForValue(@Nullable final Object payloadValue) throws IllegalArgumentException {
        if (payloadValue == null) {
            throw new IllegalArgumentException("Cannot infer payload type for null value");
        } else if (payloadValue instanceof List) {
            final Set<PayloadType> types = new HashSet<>();
            for (final Object value: ((List) payloadValue)) {
                types.add(payloadTypeForValue(value));
            }
            if (types.size() == 2 && types.contains(LONG_VALUE) && types.contains(DOUBLE_VALUE)) {
                // treat a mix of doubles and longs as doubles, because some clients might treat 0.0 as 0
                types.remove(LONG_VALUE);
            }
            if (types.size() != 1) {
                throw new IllegalArgumentException("Cannot infer payload type for list " + payloadValue);
            }
            switch(types.iterator().next()) {
                case LONG_VALUE:
                    return PayloadType.LONG_ARRAY;
                case DOUBLE_VALUE:
                    return PayloadType.DOUBLE_ARRAY;
                case STRING_VALUE:
                    return PayloadType.STRING_ARRAY;
                default:
                    // should never happen
                    throw new IllegalStateException("Bug: unexpected type returned from " + types);
            }
        } else if (payloadValue instanceof String[]) {
            return PayloadType.STRING_ARRAY;
        } else if (payloadValue instanceof Long[] || payloadValue instanceof Integer[]) {
            return PayloadType.LONG_ARRAY;
        } else if (payloadValue instanceof Double[] || payloadValue instanceof Float[]) {
            return PayloadType.DOUBLE_ARRAY;
        } else if (payloadValue instanceof Map) {
            return PayloadType.MAP;
        }
        return payloadTypeForPrimitiveValue(payloadValue);
    }

    private static PayloadType payloadTypeForPrimitiveValue(@Nonnull final Object payloadValue) {
        if (payloadValue instanceof Long || payloadValue instanceof Integer) {
            return PayloadType.LONG_VALUE;
        } else if (payloadValue instanceof Double || payloadValue instanceof Float) {
            return PayloadType.DOUBLE_VALUE;
        } else if (payloadValue instanceof String) {
            return PayloadType.STRING_VALUE;
        }
        throw new IllegalArgumentException("Payload value " + payloadValue.getClass().getSimpleName() + " : " + payloadValue + " does not correspond to a payload type");
    }

    /**
     * For printing useful error messages, the proctor webapp, and for testing.

     * @return all types name
     */
    @Nonnull
    public static List<String> allTypeNames() {
        final List<String> names = new ArrayList<>();
        for (final PayloadType p : PayloadType.values()) {
            names.add(p.payloadTypeName);
        }
        return names;
    }

    public boolean isArrayType() {
        return isArraysType;
    }
}
