package com.indeed.proctor.consumer;

import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.ProctorResult;

import java.util.Map;

public interface GroupsManagerInterceptor {
    /**
     * Interceptor running at the beginning of {@link AbstractGroupsManager#determineBucketsInternal(Identifiers, Map, Map)}
     * See also: method parameters of {@link AbstractGroupsManager#determineBucketsInternal(Identifiers, Map, Map)}
     */
    void beforeDetermineGroups(Identifiers identifiers, Map<String, Object> context, Map<String, Integer> forcedGroups);

    /**
     * Interceptor running at the end of {@link AbstractGroupsManager#determineBucketsInternal(Identifiers, Map, Map)}
     * See also: return value of {@link AbstractGroupsManager#determineBucketsInternal(Identifiers, Map, Map)}
     */
    void afterDetermineGroups(ProctorResult proctorResult);

    static GroupsManagerInterceptor getDefault() {
        return new GroupsManagerInterceptor() {
            @Override
            public void beforeDetermineGroups(
                    final Identifiers identifiers,
                    final Map<String, Object> context,
                    final Map<String, Integer> forcedGroups
            ) {
            }

            @Override
            public void afterDetermineGroups(final ProctorResult proctorResult) {
            }
        };
    }
}
