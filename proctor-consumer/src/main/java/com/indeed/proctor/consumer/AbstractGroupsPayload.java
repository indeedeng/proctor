package com.indeed.proctor.consumer;

import com.indeed.proctor.common.model.Payload;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Superclass of generated Payload classes, provides utility methods
 */
public abstract class AbstractGroupsPayload {

    /**
     * Used because ObjectMapper does not always read in longs as longs (can be integer)
     */
    private static final Function<Object, Long> LONG_CONVERTER = o -> convertToNumber(o).longValue();
    /**
     * Used because ObjectMapper does not always read in doubles as doubles (can be integer)
     */
    private static final Function<Object, Double> DOUBLE_CONVERTER = o -> convertToNumber(o).doubleValue();
    /**
     * Used only to provide better error messages for bad type, allows null (reason unknown)
     */
    public static final Function<Object, String> STRING_CONVERTER = o -> {
        if ((o == null) || (o instanceof String)) {
            return (String) o;
        }
        throw new ClassCastException("Cannot convert '" + o + "' to String");
    };

    /**
     * @return String value contained in payload map for given key
     */
    @Nullable
    protected String convertToStringValue(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        // historically, null was allowed for String, reasons unknown
        return extractValueFromMapPayload(payload, payloadMapKey, STRING_CONVERTER).orElse(null);
    }

    protected Long convertToLongValue(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        return extractNonNullValueFromMapPayload(payload, payloadMapKey, LONG_CONVERTER);
    }

    protected Double convertToDoubleValue(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        return extractNonNullValueFromMapPayload(payload, payloadMapKey, DOUBLE_CONVERTER);
    }

    protected String[] convertToStringArray(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        // historically, null values are allowed in the result, reasons unknown
        return extractNonNullValueFromMapPayload(payload, payloadMapKey, o -> convertToList(o)).stream()
                .map(STRING_CONVERTER).toArray(String[]::new);
    }

    /**
     * Converts a list of Numbers in map payload to Array of Long if not possible
     */
    protected Long[] convertToLongArray(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        return extractListWithouNullsFromMapPayload(payload, payloadMapKey, o -> convertToList(o)).stream()
                .map(LONG_CONVERTER).toArray(Long[]::new);
    }

    /**
     * Converts a list of Numbers in map payload to Array of Double, throws RuntimeException if not possible
     */
    protected Double[] convertToDoubleArray(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        return extractListWithouNullsFromMapPayload(payload, payloadMapKey, o -> convertToList(o)).stream()
                .map(DOUBLE_CONVERTER).toArray(Double[]::new);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> convertToList(final Object o) {
        if (o instanceof List) {
            return (List<T>) o;
        }
        throw new ClassCastException("Cannot convert '" + o + "' to List");
    }

    private static Number convertToNumber(final Object o) {
        if (o instanceof Number) {
            return (Number) o;
        }
        throw new ClassCastException("Cannot convert '" + o + "' to Number");
    }

    /**
     * Extracts a list, and checks the list contains no nulls, throws NullPointerException else
     */
    @Nonnull
    private static <T> List<T> extractListWithouNullsFromMapPayload(
            final Payload payload,
            final String payloadMapKey,
            final Function<Object, List<T>> converter
    ) {
        final List<T> list = extractNonNullValueFromMapPayload(payload, payloadMapKey, converter);
        if (list.contains(null)) {
            throw new NullPointerException("Null payload list value for constructor for key '" + payloadMapKey + '\'' + " in Payload: " + payload);
        }
        return list;
    }

    /**
     * @param converter: is applied if map value is not null, must not return null.
     * @throws IllegalArgumentException when payload is null, payload.map is null, map does not contain key
     * @throws NullPointerException else if map value is null
     * @return extracted payload Value converted to given class if not null, else throws NPE
     */
    @Nonnull
    private static <T> T extractNonNullValueFromMapPayload(
            final Payload payload,
            final String payloadMapKey,
            final Function<Object, T> converter
    ) {
        return extractValueFromMapPayload(payload, payloadMapKey, converter).orElseThrow(() ->
                new NullPointerException("Null payload value for constructor for key '" + payloadMapKey + '\'' + " in Payload: " + payload));
    }

    /**
     * @param converter: is applied if map value is not null, must not return null
     * @throws IllegalArgumentException when payload is null, payload.map is null, map does not contain key
     * @return optional of extracted payload Value converted to given class
     */
    @Nonnull
    private static <T> Optional<T> extractValueFromMapPayload(
            final Payload payload,
            final String payloadMapKey,
            final Function<Object, T> converter
    ) {
        final Optional<Map<String, Object>> payloadMapOpt = Optional.ofNullable(payload).map(Payload::getMap);
        if (payloadMapOpt.isPresent() && payloadMapOpt.get().containsKey(payloadMapKey)) {
            return payloadMapOpt.map(m -> m.get(payloadMapKey)).map(converter);
        }
        throw new IllegalArgumentException("Missing payload for constructor for key '" + payloadMapKey + '\'' + " in Payload: " + payload);
    }
}
