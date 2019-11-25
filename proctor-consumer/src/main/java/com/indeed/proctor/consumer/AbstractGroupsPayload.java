package com.indeed.proctor.consumer;

import com.indeed.proctor.common.model.Payload;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public abstract class AbstractGroupsPayload {

    @Nullable
    protected String convertToStringValue(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        checkPayloadExist(payload, payloadMapKey);
        // historically, null was allowed for String, reasons unknown
        return (String) payload.getMap().get(payloadMapKey);
    }

    protected Long convertToLongValue(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        checkPayloadExist(payload, payloadMapKey);
        return extractNonNullValueFromMapPayload(payload, payloadMapKey, Number.class).longValue();
    }

    protected Double convertToDoubleValue(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        checkPayloadExist(payload, payloadMapKey);
        return extractNonNullValueFromMapPayload(payload, payloadMapKey, Number.class).doubleValue();
    }

    protected String[] convertToStringArray(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        checkPayloadExist(payload, payloadMapKey);
        final Object[] toConvert = extractNonNullValueFromMapPayload(payload, payloadMapKey, List.class).toArray();
        return convertObjectArrToStringArr(toConvert);
    }

    protected Long[] convertToLongArray(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        checkPayloadExist(payload, payloadMapKey);
        final Object[] toConvert = extractNonNullValueFromMapPayload(payload, payloadMapKey, List.class).toArray();
        return convertObjectArrToLongArr(toConvert);
    }

    protected Double[] convertToDoubleArray(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        checkPayloadExist(payload, payloadMapKey);
        final Object[] toConvert = extractNonNullValueFromMapPayload(payload, payloadMapKey, List.class).toArray();
        return convertObjectArrToDoubleArr(toConvert);
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
            final Class<T> clazz
    ) {
        // assumes (from checkPayloadExist) that payload != null, payload.getMap() != null, ...
        final T result = (T) payload.getMap().get(payloadMapKey);
        if (result != null) {
            return result;
        }
        throw new NullPointerException(
                "Null payload value for constructor for key '" + payloadMapKey + '\'' + " in Payload: " + payload);
    }

    /*
     * Used because ObjectMapper does not always read in doubles as doubles
     * When it is a Map<String,Object>
     */
    private static Double[] convertObjectArrToDoubleArr(final Object[] list) {
        final Double[] toReturn = new Double[list.length];
        for (int i = 0; i < list.length; i++) {
            toReturn[i] = ((Number)list[i]).doubleValue();
        }
        return toReturn;
    }

    /*
    * Used because ObjectMapper does not always read in longs as longs (can be integer)
     * When it is a Map<String,Object>
     */
    private static Long[] convertObjectArrToLongArr(final Object[] list) {
        final Long[] toReturn = new Long[list.length];
        for (int i = 0; i < list.length; i++) {
            toReturn[i] = ((Number)list[i]).longValue();
        }
        return toReturn;
    }

    private static String[] convertObjectArrToStringArr(final Object[] list) {
        final String[] toReturn = new String[list.length];
        for (int i = 0; i < list.length; i++) {
            toReturn[i] = (String)list[i];
        }
        return toReturn;
    }
}
