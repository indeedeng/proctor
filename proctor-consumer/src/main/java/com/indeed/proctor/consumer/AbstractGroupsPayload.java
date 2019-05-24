package com.indeed.proctor.consumer;


import com.indeed.proctor.common.model.Payload;

import java.util.ArrayList;

public abstract class AbstractGroupsPayload {

    protected String convertToStringValue(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        checkPayloadExist(payload, payloadMapKey);
        return (String) payload.getMap().get(payloadMapKey);
    }

    protected Long convertToLongValue(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        checkPayloadExist(payload, payloadMapKey);
        return ((Number) payload.getMap().get(payloadMapKey)).longValue();
    }

    protected Double convertToDoubleValue(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        checkPayloadExist(payload, payloadMapKey);
        return ((Number) payload.getMap().get(payloadMapKey)).doubleValue();
    }

    protected String[] convertToStringArray(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        checkPayloadExist(payload, payloadMapKey);
        final Object[] toConvert = ((ArrayList) payload.getMap().get(payloadMapKey)).toArray();
        return convertObjectArrToStringArr(toConvert);
    }

    protected Long[] convertToLongArray(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        checkPayloadExist(payload, payloadMapKey);
        final Object[] toConvert = ((ArrayList) payload.getMap().get(payloadMapKey)).toArray();
        return convertObjectArrToLongArr(toConvert);
    }

    protected Double[] convertToDoubleArray(final Payload payload, final String payloadMapKey) throws IllegalArgumentException {
        checkPayloadExist(payload, payloadMapKey);
        final Object[] toConvert = ((ArrayList) payload.getMap().get(payloadMapKey)).toArray();
        return convertObjectArrToDoubleArr(toConvert);

    }

    private void checkPayloadExist(final Payload payload, final String payloadMapKey) {
        if (payload != null && payload.getMap() != null && payload.getMap().containsKey(payloadMapKey)) {
            return;
        }
        throw new IllegalArgumentException("Missing payload for constructor");
    }

    /*
     * Used because ObjectMapper does not always read in doubles as doubles
     * When it is a Map<String,Object>
     */
    private Double[] convertObjectArrToDoubleArr(final Object[] list) {
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
    private Long[] convertObjectArrToLongArr(final Object[] list) {
        final Long[] toReturn = new Long[list.length];
        for (int i = 0; i < list.length; i++) {
            toReturn[i] = ((Number)list[i]).longValue();
        }
        return toReturn;
    }

    private String[] convertObjectArrToStringArr(final Object[] list) {
        final String[] toReturn = new String[list.length];
        for (int i = 0; i < list.length; i++) {
            toReturn[i] = (String)list[i];
        }
        return toReturn;
    }
}
