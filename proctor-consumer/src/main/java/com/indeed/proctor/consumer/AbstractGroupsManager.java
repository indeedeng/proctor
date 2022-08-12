package com.indeed.proctor.consumer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.indeed.proctor.common.ForceGroupsOptions;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.Proctor;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySortedMap;

/**
 * Provides ability to determine buckets using a supplier to fetch a recent test definition.
 * Subclasses should only pass context Maps to determineBucketsInternal that are compatible to the schema in getProvidedContext();
 */
public abstract class AbstractGroupsManager implements ProctorContextDescriptor {
    private final Supplier<Proctor> proctorSource;
    private final Supplier<GroupsManagerInterceptor> interceptorSupplier;

    protected AbstractGroupsManager(final Supplier<Proctor> proctorSource) {
        this.proctorSource = proctorSource;
        this.interceptorSupplier = GroupsManagerInterceptor::getDefault;
    }

    protected AbstractGroupsManager(
            final Supplier<Proctor> proctorSource,
            final Supplier<GroupsManagerInterceptor> interceptorSupplier
    ) {
        this.proctorSource = proctorSource;
        this.interceptorSupplier = interceptorSupplier;
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
    protected ProctorResult determineBucketsInternal(
            final TestType testType,
            final String identifier,
            final Map<String, Object> context
    ) {
        final Map<String, Integer> forcedGroups = emptyMap();
        final Identifiers identifiers = new Identifiers(testType, identifier);
        return determineBucketsInternal(identifiers, context, forcedGroups);
    }

    @VisibleForTesting
    protected ProctorResult determineBucketsInternal(final Identifiers identifiers, final Map<String, Object> context) {
        return this.determineBucketsInternal(identifiers, context, emptyMap());
    }

    /**
     * I don't see any value in using this in an application; you probably should use
     * {@link #determineBucketsInternal(HttpServletRequest, HttpServletResponse, Identifiers, Map, boolean)}
     *
     * @param identifiers        a {@link Map} of unique-ish {@link String}s describing the request in the context of different {@link TestType}s.
     *                           For example, {@link TestType#USER} has a CTK associated, {@link TestType#EMAIL} is an email address,
     *                           {@link TestType#PAGE} might be a url-encoded String containing the normalized relevant page parameters
     * @param context            a {@link Map} containing variables describing the context in which the request is executing.
     *                           These will be supplied to any rules that execute to determine test eligibility.
     * @param forceGroupsOptions a options to specify forced groups which includes a {@link Map} from a String test name to an Integer bucket value.
     *                           For the specified test allocate the specified bucket (if valid) regardless of the standard logic
     * @return a {@link ProctorResult} to describe buckets allocations of all tests.
     */
    @VisibleForTesting
    protected ProctorResult determineBucketsInternal(
            final Identifiers identifiers,
            final Map<String, Object> context,
            final ForceGroupsOptions forceGroupsOptions
    ) {
        final GroupsManagerInterceptor interceptor = interceptorSupplier.get();
        interceptor.beforeDetermineGroups(identifiers, context, forceGroupsOptions.getForceGroups());

        final Proctor proctor = proctorSource.get();
        if (proctor == null) {
            final Map<String, TestBucket> buckets = getDefaultBucketValues();
            return new ProctorResult(Audit.EMPTY_VERSION,
                    new TreeMap<>(buckets), // legacy behavior uses mutable Map
                    emptySortedMap(),
                    emptyMap()
            );
        }
        final ProctorResult proctorResult = proctor.determineTestGroups(
                identifiers,
                context,
                forceGroupsOptions,
                emptyList()
        );

        interceptor.afterDetermineGroups(proctorResult);
        return proctorResult;
    }

    /**
     * @deprecated {@link #determineBucketsInternal(Identifiers, Map, ForceGroupsOptions)} is the replacement.
     * If it's used in servlet-based-application where HttpServletRequest and HttpServletResponse are available
     * consider use {@link #determineBucketsInternal(HttpServletRequest, HttpServletResponse, Identifiers, Map, boolean)}
     * for better functionality.
     */
    @Deprecated
    @VisibleForTesting
    protected ProctorResult determineBucketsInternal(
            final Identifiers identifiers,
            final Map<String, Object> context,
            final Map<String, Integer> forcedGroups
    ) {
        return determineBucketsInternal(
                identifiers,
                context,
                ForceGroupsOptions.builder()
                        .putAllForceGroups(forcedGroups)
                        .build()
        );
    }


    protected abstract Map<String, TestBucket> getDefaultBucketValues();

    /**
     * servlet-based-application friendly version of determineBucketsInternal, also enabling forcing groups via request headers and cookies
     */
    protected ProctorResult determineBucketsInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Identifiers identifiers,
            final Map<String, Object> context,
            final boolean allowForcedGroups
    ) {
        final ForceGroupsOptions forceGroupsOptions;
        if (allowForcedGroups) {
            forceGroupsOptions = ProctorConsumerUtils.parseForcedGroupsOptions(request, new HashSet<>());
            ProctorConsumerUtils.createForcedGroupsCookieUnlessEmpty(request.getContextPath(), forceGroupsOptions)
                    .ifPresent(response::addCookie);
        } else {
            forceGroupsOptions = ForceGroupsOptions.empty();
        }
        return determineBucketsInternal(identifiers, context, forceGroupsOptions);
    }

    /**
     * servlet-based-application friendly version of determineBucketsInternal, also enabling forcing groups and payloads via request headers and cookies
     */
    protected ProctorResult determineBucketsInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Identifiers identifiers,
            final Map<String, Object> context,
            final boolean allowForcedGroups,
            final Set<String> forcePayloadTests
    ) {
        final ForceGroupsOptions forceGroupsOptions;
        if (allowForcedGroups) {
            forceGroupsOptions = ProctorConsumerUtils.parseForcedGroupsOptions(request, forcePayloadTests);
            ProctorConsumerUtils.createForcedGroupsCookieUnlessEmpty(request.getContextPath(), forceGroupsOptions)
                    .ifPresent(response::addCookie);
        } else {
            forceGroupsOptions = ForceGroupsOptions.empty();
        }
        return determineBucketsInternal(identifiers, context, forceGroupsOptions);
    }
}
