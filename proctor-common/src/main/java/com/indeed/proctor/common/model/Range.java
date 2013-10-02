package com.indeed.proctor.common.model;

public class Range {
    private int bucketValue;
    private double length;

    public Range() { /* intentionally empty */ }

    public Range(final int bucketValue, final double length) {
        if (length < 0 || length > 1) {
            throw new IllegalArgumentException("Length must be >= 0 and <= 1");
        }
        this.bucketValue = bucketValue;
        this.length = length;
    }

    public int getBucketValue() {
        return bucketValue;
    }

    public void setBucketValue(final int bucketValue) {
        this.bucketValue = bucketValue;
    }

    public double getLength() {
        return length;
    }

    public void setLength(final double length) {
        this.length = length;
    }
}