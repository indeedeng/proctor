package com.indeed.proctor.consumer;

public interface Bucket<T extends Enum<T>> {
    T getTest();
    int getValue();
    String getName();
    String getFullName();
}