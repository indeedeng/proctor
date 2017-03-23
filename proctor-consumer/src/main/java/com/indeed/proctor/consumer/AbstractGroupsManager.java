package com.indeed.proctor.consumer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.collect.Maps;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.Proctor;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Map;

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
     * TODO: should the identifier argument be a Map from TestType to ID?
     *
     * @param testType   test type
     * @param identifier identifier
     * @param context    a {@link Map} containing variables describing the context in which the request is executing. These will be supplied to any rules that execute to determine test eligibility.
     * @return a {@link ProctorResult} to describe buckets allocations of all tests.
     * @deprecated use {@link AbstractGroupsManager#determineBucketsInternal(Identifiers, Map)}
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
     *
     * @param identifiers  a {@link Map} of unique-ish {@link String}s describing the request in the context of different {@link TestType}s.For example,
     *                     {@link TestType#USER} has a CTK associated, {@link TestType#EMAIL} is an email address, {@link TestType#PAGE} might be a url-encoded String
     *                     containing the normalized relevant page parameters
     * @param context      a {@link Map} containing variables describing the context in which the request is executing. These will be supplied to any rules that
     *                     execute to determine test eligibility.
     * @param forcedGroups a {@link Map} from a String test name to an Integer bucket value. For the specified test allocate the specified bucket (if valid) regardless
     *                     of the standard logic
     * @return a {@link ProctorResult} to describe buckets allocations of all tests.
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
            return new ProctorResult(Audit.EMPTY_VERSION, buckets, Collections.<String, ConsumableTestDefinition>emptyMap());
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
