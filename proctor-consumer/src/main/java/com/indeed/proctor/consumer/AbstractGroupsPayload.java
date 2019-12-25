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
    private static final Function<Object, Long> LONG_CONVERTER = o -> ((Number) o).longValue();
    /**
     * Used because ObjectMapper does not always read in doubles as doubles (can be integer)
     */
    private static final Function<Object, Double> DOUBLE_CONVERTER = o -> ((Number) o).doubleValue();

    /**
     * @return String value contained in payload map map for given key
     */
    @Nullable
    protected String convertToStringValue(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        // historically, null was allowed for String, reasons unknown
        return extractValueFromMapPayload(payload, payloadMapKey, o -> (String) o).orElse(null);
    }

    protected Long convertToLongValue(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        return extractNonNullValueFromMapPayload(payload, payloadMapKey, LONG_CONVERTER);
    }

    protected Double convertToDoubleValue(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        return extractNonNullValueFromMapPayload(payload, payloadMapKey, DOUBLE_CONVERTER);
    }

    @SuppressWarnings("unchecked")
    protected String[] convertToStringArray(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        return extractNonNullValueFromMapPayload(payload, payloadMapKey, o -> (List<String>) o).toArray(new String[0]);
    }

    /**
     * Converts a list of Numbers in map payload to Array of Long if not possible
     */
    @SuppressWarnings("unchecked")
    protected Long[] convertToLongArray(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        return extractNonNullValueFromMapPayload(payload, payloadMapKey, o -> (List<Number>) o).stream()
                .map(LONG_CONVERTER).toArray(Long[]::new);
    }

    /**
     * Converts a list of Numbers in map payload to Array of Double, throws RuntimeException if not possible
     */
    @SuppressWarnings("unchecked")
    protected Double[] convertToDoubleArray(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        return extractNonNullValueFromMapPayload(payload, payloadMapKey, o -> (List<Number>) o).stream()
                .map(DOUBLE_CONVERTER).toArray(Double[]::new);
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
