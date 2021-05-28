package com.indeed.proctor.common;

import java.util.Map;

public interface GroupsManagerInterceptor {
    void beforeDetermineBucket(Identifiers identifiers, Map<String, Object> context, Map<String, Integer> forcedGroups);
    void afterDetermineBucket(ProctorResult proctorResult);

    static GroupsManagerInterceptor getDefault() {
        return new GroupsManagerInterceptor() {
            @Override
            public void beforeDetermineBucket(
                    final Identifiers identifiers,
                    final Map<String, Object> context,
                    final Map<String, Integer> forcedGroups
            ) {
            }

            @Override
            public void afterDetermineBucket(final ProctorResult proctorResult) {
            }
        };
    }
}
