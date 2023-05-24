package com.indeed.proctor.common;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.model.TestType;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Convenience wrapper for Map<TestType, String>, used to select an identifier suitable to resolve
 * test groups for the given experiment.
 */
public class Identifiers {
    @Nonnull private final Map<TestType, String> identifierMap;
    private final boolean randomEnabled;

    public Identifiers(@Nonnull final Map<TestType, String> identifierMap) {
        this(identifierMap, false);
    }

    /**
     * This constructor can construct instances with randomEnabled == true, which only affects test
     * with TestType.RANDOM, and enables the random behavior. For A/B testing, randomized behavior
     * is typically harmful, because the same test units get different treatment every time,
     * preventing analysis of effects. TestType.RANDOM can be used for non-experimentation purposes,
     * such as gradual rollouts of migrations with e.g. 20% of traffic to be diverged to a different
     * server.
     *
     * <p>TestType.RANDOM will only work with apps that use this constructor setting randomEnabled
     * == true. Please refers to {@link Proctor#determineTestGroups(Identifiers, Map, Map,
     * Collection)} to know how randomEnabled value is used to enable/disable random behavior for
     * tests with TestType.RANDOM.
     *
     * @param identifierMap: A map from TestType to identifier to use (e.g. ctk, accountId). Note
     *     that it must not has an entry for TestType.RANDOM because proctor generates random values
     *     for this type.
     * @param randomEnabled: A flag whether random behavior for tests with TestType.RANDOM is
     *     enabled or not. Default is false.
     */
    public Identifiers(
            @Nonnull final Map<TestType, String> identifierMap, final boolean randomEnabled) {
        if (identifierMap.containsKey(TestType.RANDOM)) {
            throw new IllegalArgumentException(
                    "Cannot specify " + TestType.RANDOM + " in identifierMap");
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

    public static Identifiers of(
            final TestType typeA,
            final String identifierA,
            final TestType typeB,
            final String identifierB) {
        return new Identifiers(ImmutableMap.of(typeA, identifierA, typeB, identifierB));
    }

    public static Identifiers of(
            final TestType typeA,
            final String identifierA,
            final TestType typeB,
            final String identifierB,
            final TestType typeC,
            final String identifierC) {
        return new Identifiers(
                ImmutableMap.of(typeA, identifierA, typeB, identifierB, typeC, identifierC));
    }

    /** @return true if random test group assignment was enabled in constructor. */
    public boolean isRandomEnabled() {
        return randomEnabled;
    }

    /** @return a set of all the test types whose identifiers are stored in this object. */
    public Set<TestType> getAvailableTestTypes() {
        return identifierMap.keySet();
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
        return randomEnabled == that.randomEnabled
                && Objects.equals(identifierMap, that.identifierMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifierMap, randomEnabled);
    }
}
