package com.indeed.proctor.common;

public interface GroupsManagerInterceptor {
    void beforeDetermineBucket();
    void afterDetermineBucket();

    static GroupsManagerInterceptor getDefault() {
        return new GroupsManagerInterceptor() {
            @Override
            public void beforeDetermineBucket() {
            }

            @Override
            public void afterDetermineBucket() {
            }
        };
    }
}
