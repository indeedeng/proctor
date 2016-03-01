package com.indeed.proctor.common.model;

import javax.annotation.Nonnull;

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

    public Range(@Nonnull final Range other) {
        this.bucketValue = other.bucketValue;
        this.length = other.length;
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Range range = (Range) o;

        if (bucketValue != range.bucketValue) {
            return false;
        }
        return Double.compare(range.length, length) == 0;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = bucketValue;
        temp = Double.doubleToLongBits(length);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}