package com.indeed.proctor.common;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.model.TestType;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * Convenience wrapper for Map<TestType, String>, used to select an identifier suitable to resolve test groups
 * for the given experiment.
 */
public class Identifiers {
    @Nonnull
    private final Map<TestType, String> identifierMap;
    private final boolean randomEnabled;

    public Identifiers(@Nonnull final Map<TestType, String> identifierMap) {
        this(identifierMap, false);
    }

    /**
     * Only this constructor can construct instances with randomEnabled == true.
     * randomEnabled == true is rarely used because test groups should be usually assigned by an identifier.
     *
     * @param identifierMap: A map from TestType to identifier to use (e.g. ctk, accountId). Note that it must not has
     *                       an entry for TestType.RANDOM because random tests don't need identifiers.
     * @param randomEnabled: A flag whether buckets for RANDOM tests should be assigned or not.
     */
    public Identifiers(@Nonnull final Map<TestType, String> identifierMap, final boolean randomEnabled) {
        if (identifierMap.containsKey(TestType.RANDOM)) {
            throw new IllegalArgumentException("Cannot specify " + TestType.RANDOM + " in identifierMap");
        }
        this.identifierMap = identifierMap;
        this.randomEnabled = randomEnabled;
    }

    public Identifiers(final TestType type, final String identifier) {
        this(Collections.singletonMap(type, identifier));
    }

    public static Identifiers of(final TestType type, final String identifier) {
        return new Identifiers(type, identifier);
    }

    public static Identifiers of(final TestType typeA, final String identifierA,
                                 final TestType typeB, final String identifierB) {
        return new Identifiers(ImmutableMap.of(typeA, identifierA, typeB, identifierB));
    }

    public static Identifiers of(final TestType typeA, final String identifierA,
                                 final TestType typeB, final String identifierB,
                                 final TestType typeC, final String identifierC) {
        return new Identifiers(ImmutableMap.of(typeA, identifierA, typeB, identifierB, typeC, identifierC));
    }

    /**
     * Only affects test with TestType.RANDOM. If this method returns false, proctor will not assign buckets for
     * RANDOM tests, which means default buckets defined in an application will be assigned.
     * This method is called only in {@link Proctor#determineTestGroups(Identifiers, Map, Map, Collection)}.
     *
     * @return a flag whether proctor assigns test groups for RANDOM tests or not
     */
    public boolean isRandomEnabled() {
        return randomEnabled;
    }

    @CheckForNull
    public String getIdentifier(final TestType testType) {
        return identifierMap.get(testType);
    }

    @CheckForNull
    public String getUserId() {
        return getIdentifier(TestType.USER);
    }

    @SuppressWarnings("UnusedDeclaration")
    @CheckForNull
    public String getPageId() {
        return getIdentifier(TestType.PAGE);
    }

    @CheckForNull
    public String getCompanyId() {
        return getIdentifier(TestType.COMPANY);
    }

    @CheckForNull
    public String getEmail() {
        return getIdentifier(TestType.EMAIL);
    }

    @CheckForNull
    public String getAccountId() {
        return getIdentifier(TestType.ACCOUNT);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Identifiers that = (Identifiers) o;
        return randomEnabled == that.randomEnabled &&
                Objects.equals(identifierMap, that.identifierMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifierMap, randomEnabled);
    }
}
