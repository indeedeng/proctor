package com.indeed.proctor.consumer;


import com.indeed.proctor.common.model.TestBucket;

import java.util.ArrayList;

public abstract class AbstractGroupsPayload {


    protected String convertToStringValue(final TestBucket bucket, final String payloadMapKey) throws IllegalArgumentException {
        checkBucketAndPayloadExist(bucket, payloadMapKey);
        return (String)bucket.getPayload().getMap().get(payloadMapKey);
    }

    protected Long convertToLongValue(final TestBucket bucket, final String payloadMapKey) throws IllegalArgumentException {
        checkBucketAndPayloadExist(bucket, payloadMapKey);
        return (Long)bucket.getPayload().getMap().get(payloadMapKey);
    }

    protected Double convertToDoubleValue(final TestBucket bucket, final String payloadMapKey) throws IllegalArgumentException {
        checkBucketAndPayloadExist(bucket, payloadMapKey);
        return (Double)bucket.getPayload().getMap().get(payloadMapKey);
    }

    protected String[] convertToStringArray(final TestBucket bucket, final String payloadMapKey) throws IllegalArgumentException {
        checkBucketAndPayloadExist(bucket, payloadMapKey);
        final Object[] toConvert = ((ArrayList)bucket.getPayload().getMap().get(payloadMapKey)).toArray();
        return convertObjectArrToStringArr(toConvert);
    }

    protected Long[] convertToLongArray(final TestBucket bucket, final String payloadMapKey) throws IllegalArgumentException {
        checkBucketAndPayloadExist(bucket, payloadMapKey);
        final Object[] toConvert = ((ArrayList)bucket.getPayload().getMap().get(payloadMapKey)).toArray();
        return convertObjectArrToLongArr(toConvert);
    }

    protected Double[] convertToDoubleArray(final TestBucket bucket, final String payloadMapKey) throws IllegalArgumentException {
        checkBucketAndPayloadExist(bucket, payloadMapKey);
        final Object[] toConvert = ((ArrayList)bucket.getPayload().getMap().get(payloadMapKey)).toArray();
        return convertObjectArrToDoubleArr(toConvert);

    }

    private void checkBucketAndPayloadExist(final TestBucket bucket, final String payloadMapKey) {
        if(bucket != null && bucket.getPayload() != null && bucket.getPayload().getMap() != null && bucket.getPayload().getMap().containsKey(payloadMapKey)) {
            return;
        }
        throw new IllegalArgumentException("Missing bucket for payload constructor");
    }

    /*
     * Used because ObjectMapper does not always read in doubles as doubles
     * When it is a Map<String,Object>
     */
    private Double[] convertObjectArrToDoubleArr(final Object[] list) {
        final Double[] toReturn = new Double[list.length];
        for(int i = 0; i < list.length; i++) {
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
        for(int i = 0; i < list.length; i++) {
            toReturn[i] = ((Number)list[i]).longValue();
        }
        return toReturn;
    }

    private String[] convertObjectArrToStringArr(final Object[] list) {
        final String[] toReturn = new String[list.length];
        for(int i = 0; i < list.length; i++) {
            toReturn[i] = (String)list[i];
        }
        return toReturn;
    }
}
