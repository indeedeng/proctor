package com.indeed.proctor.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.StringJoiner;

public class TestDependency {
    private final String testName;
    private final int bucketValue;

    @JsonCreator
    public TestDependency(
            @JsonProperty("testName") final String testName,
            @JsonProperty("bucketValue") final int bucketValue
    ) {
        this.testName = testName;
        this.bucketValue = bucketValue;
    }

    public String getTestName() {
        return testName;
    }

    public int getBucketValue() {
        return bucketValue;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TestDependency.class.getSimpleName() + "[", "]")
                .add("testName='" + testName + "'")
                .add("bucketValue=" + bucketValue)
                .toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        final TestDependency that = (TestDependency) o;
        return (bucketValue == that.bucketValue) && Objects.equals(testName, that.testName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testName, bucketValue);
    }
}
