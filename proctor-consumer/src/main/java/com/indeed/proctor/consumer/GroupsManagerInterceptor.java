package com.indeed.proctor.consumer;

import com.indeed.proctor.common.ForceGroupsOptions;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.ProctorResult;

import java.util.Map;

public interface GroupsManagerInterceptor {
    /**
     * Interceptor running at the beginning of
     * {@link AbstractGroupsManager#determineBucketsInternal(Identifiers, Map, ForceGroupsOptions)}
     * See also: method parameters of
     * {@link AbstractGroupsManager#determineBucketsInternal(Identifiers, Map, ForceGroupsOptions)}
     */
    void beforeDetermineGroups(Identifiers identifiers, Map<String, Object> context, ForceGroupsOptions forceGroupsOptions);

    /**
     * Interceptor running at the end of
     * {@link AbstractGroupsManager#determineBucketsInternal(Identifiers, Map, ForceGroupsOptions)}
     * See also: return value of
     * {@link AbstractGroupsManager#determineBucketsInternal(Identifiers, Map, ForceGroupsOptions)}
     */
    void afterDetermineGroups(ProctorResult proctorResult);

    static GroupsManagerInterceptor getDefault() {
        return new GroupsManagerInterceptor() {
            @Override
            public void beforeDetermineGroups(
                    final Identifiers identifiers,
                    final Map<String, Object> context,
                    final ForceGroupsOptions forceGroupsOptions
            ) {
            }

            @Override
            public void afterDetermineGroups(final ProctorResult proctorResult) {
            }
        };
    }
}
