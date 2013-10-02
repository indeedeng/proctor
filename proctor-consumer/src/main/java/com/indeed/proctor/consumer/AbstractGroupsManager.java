package com.indeed.proctor.consumer;

import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.Proctor;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestType;

import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.collect.Maps;

/**
 * Doesn't really do much
 * @author ketan
 *
 */
public abstract class AbstractGroupsManager implements ProctorContextDescriptor {
    private final Supplier<Proctor> proctorSource;
    protected AbstractGroupsManager(final Supplier<Proctor> proctorSource) {
        this.proctorSource = proctorSource;
    }

    /**
     * I don't see any value in using this in an application; you probably should use
     * {@link #determineBucketsInternal(HttpServletRequest, HttpServletResponse, String, Map, boolean)}
     * TODO: should the identifier argument be a Map from TestType to ID?
     * @deprecated use {@link AbstractGroupsManager#determineBucketsInternal(Map, Map)}
     */
    @VisibleForTesting
    protected ProctorResult determineBucketsInternal(final TestType testType, final String identifier, final Map<String, Object> context) {
        final Map<String, Integer> forcedGroups = Collections.emptyMap();
        final Identifiers identifiers = new Identifiers(testType, identifier);
        return determineBucketsInternal(identifiers, context, forcedGroups);
    }

    @VisibleForTesting
    protected ProctorResult determineBucketsInternal(final Identifiers identifiers, final Map<String, Object> context) {
        return this.determineBucketsInternal(identifiers, context, Collections.<String, Integer>emptyMap());
    }

    /**
     * I don't see any value in using this in an application; you probably should use
     * {@link #determineBucketsInternal(HttpServletRequest, HttpServletResponse, Identifiers, Map, boolean)}
     */
    @VisibleForTesting
    protected ProctorResult determineBucketsInternal(final Identifiers identifiers, final Map<String, Object> context, final Map<String, Integer> forcedGroups) {
        final Proctor proctor = proctorSource.get();
        if (proctor == null) {
            final Map<String, TestBucket> buckets = getDefaultBucketValues();
            final Map<String, Integer> versions = Maps.newHashMap();
            for (final String testName : buckets.keySet()) {
                versions.put(testName, Integer.valueOf(-1));
            }
            return new ProctorResult(-1, buckets, Collections.<String, ConsumableTestDefinition>emptyMap());
        }
        final ProctorResult result = proctor.determineTestGroups(identifiers, context, forcedGroups);
        return result;
    }

    protected abstract Map<String, TestBucket> getDefaultBucketValues();

    protected ProctorResult determineBucketsInternal(final HttpServletRequest request, final HttpServletResponse response, final Identifiers identifiers,
            final Map<String, Object> context, final boolean allowForcedGroups) {
        final Map<String, Integer> forcedGroups;
        if (allowForcedGroups) {
            forcedGroups = ProctorConsumerUtils.parseForcedGroups(request);
            ProctorConsumerUtils.setForcedGroupsCookie(request, response, forcedGroups);
        } else {
            forcedGroups = Collections.emptyMap();
        }
        return determineBucketsInternal(identifiers, context, forcedGroups);
    }
}
