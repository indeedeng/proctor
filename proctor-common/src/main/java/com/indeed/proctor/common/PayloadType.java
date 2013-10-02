package com.indeed.proctor.common;

import com.indeed.proctor.common.model.Payload;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * enum for dealing with Payload types.  Used in code generation for
 * mapping between payload type names, java class names, and accessor names.
 *
 * @author pwp
 */
public enum PayloadType {
    DOUBLE_VALUE ("doubleValue", "Double", "getDoubleValue"),
    DOUBLE_ARRAY ("doubleArray", "Double[]", "getDoubleArray"),
    LONG_VALUE   ("longValue", "Long", "getLongValue"),
    LONG_ARRAY   ("longArray", "Long[]", "getLongArray"),
    STRING_VALUE ("stringValue", "String", "getStringValue"),
    STRING_ARRAY ("stringArray", "String[]", "getStringArray");

    @Nonnull
    public final String payloadTypeName;
    @Nonnull
    public final String javaClassName;
    @Nonnull
    public final String javaAccessorName;

    private PayloadType(
            @Nonnull final String payloadTypeName,
            @Nonnull final String javaClassName,
            @Nonnull final String javaAccessorName
    ) {
        this.payloadTypeName = payloadTypeName;
        this.javaClassName = javaClassName;
        this.javaAccessorName = javaAccessorName;
    }

    /**
     * See whether a given Payload has a specified type.  This could also
     * be done with introspection, and might belong in the Payload bean
     * itself.
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
            default:
                throw new IllegalStateException("Unknown payload type: " + this);
        }
    }

    /**
     * Given a Payload field name, return its corresponding PayloadType.
     * Return null if it isn't an existing field name.
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
     * For printing useful error messages, the proctor webapp, and for testing.
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
