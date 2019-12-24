package com.indeed.proctor.consumer;

import com.indeed.proctor.common.model.Payload;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.List;
import java.util.function.Function;

public abstract class AbstractGroupsPayload {

    /**
     * Used because ObjectMapper does not always read in longs as longs (can be integer)
     */
    private static final Function<Object, Long> LONG_CONVERTER = o -> ((Number) o).longValue();
    /**
     * Used because ObjectMapper does not always read in doubles as doubles (can be integer)
     */
    private static final Function<Object, Double> DOUBLE_CONVERTER = o -> ((Number) o).doubleValue();

    @Nullable
    protected String convertToStringValue(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        checkPayloadExist(payload, payloadMapKey);
        // historically, null was allowed for String, reasons unknown
        return (String) payload.getMap().get(payloadMapKey);
    }

    protected Long convertToLongValue(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        checkPayloadExist(payload, payloadMapKey);
        return extractNonNullValueFromMapPayload(payload, payloadMapKey, LONG_CONVERTER);
    }

    protected Double convertToDoubleValue(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        checkPayloadExist(payload, payloadMapKey);
        return extractNonNullValueFromMapPayload(payload, payloadMapKey, DOUBLE_CONVERTER);
    }

    @SuppressWarnings("unchecked")
    protected String[] convertToStringArray(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        checkPayloadExist(payload, payloadMapKey);
        final List<Object> list = extractNonNullValueFromMapPayload(payload, payloadMapKey, o -> (List) o);
        return convertToTypedArray(list, o -> (String) o, String.class);
    }

    @SuppressWarnings("unchecked")
    protected Long[] convertToLongArray(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        checkPayloadExist(payload, payloadMapKey);
        final List<Object> list = extractNonNullValueFromMapPayload(payload, payloadMapKey, o -> (List) o);
        return convertToTypedArray(list, LONG_CONVERTER, Long.class);
    }

    @SuppressWarnings("unchecked")
    protected Double[] convertToDoubleArray(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        checkPayloadExist(payload, payloadMapKey);
        final List<Object> list = extractNonNullValueFromMapPayload(payload, payloadMapKey, o -> (List) o);
        return convertToTypedArray(list, DOUBLE_CONVERTER, Double.class);
    }

    private static void checkPayloadExist(final Payload payload, final String payloadMapKey) {
        if (payload != null && payload.getMap() != null && payload.getMap().containsKey(payloadMapKey)) {
            return;
        }
        throw new IllegalArgumentException(
                "Missing payload for constructor for key '" + payloadMapKey + '\'' + " in Payload: " + payload);
    }

    /**
     * @throws NullPointerException else if map value is null
     * @return extracted payload Value cast to given class if not null
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    private static <T> T extractNonNullValueFromMapPayload(
            final Payload payload,
            final String payloadMapKey,
            final Function<Object, T> converter
    ) {
        // assumes (from checkPayloadExist) that payload != null, payload.getMap() != null, ...
        final T result = converter.apply(payload.getMap().get(payloadMapKey));
        if (result != null) {
            return result;
        }
        throw new NullPointerException(
                "Null payload value for constructor for key '" + payloadMapKey + '\'' + " in Payload: " + payload);
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] convertToTypedArray(final List<Object> list, final Function<Object, T> converter, final Class<T> clazz) {
        final T[] toReturn = (T[]) Array.newInstance(clazz, list.size());
        for (int i = 0; i < list.size(); i++) {
            toReturn[i] = converter.apply(list.get(i));
        }
        return toReturn;
    }
}
