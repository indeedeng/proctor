package com.indeed.proctor.common;

public interface GroupsManagerCallbacks {
    void beforeDetermineBucket();
    void afterDetermineBucket();

    static GroupsManagerCallbacks getDefault() {
        return new GroupsManagerCallbacks() {
            @Override
            public void beforeDetermineBucket() {
            }

            @Override
            public void afterDetermineBucket() {
            }
        };
    }
}
