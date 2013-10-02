package com.indeed.proctor.common;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.model.TestType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

public class Identifiers {
    @Nonnull
    private final Map<TestType, String> identifierMap;
    private final boolean randomEnabled;

    public Identifiers(@Nonnull final Map<TestType, String> identifierMap) {
        this(identifierMap, false);
    }

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


    public boolean isRandomEnabled() {
        return randomEnabled;
    }

    @Nullable
    public String getIdentifier(final TestType testType) {
        return identifierMap.get(testType);
    }

    @Nullable
    public String getUserId() {
        return getIdentifier(TestType.USER);
    }

    @SuppressWarnings("UnusedDeclaration")
    @Nullable
    public String getPageId() {
        return getIdentifier(TestType.PAGE);
    }

    @Nullable
    public String getCompanyId() {
        return getIdentifier(TestType.COMPANY);
    }

    @Nullable
    public String getEmail() {
        return getIdentifier(TestType.EMAIL);
    }

    @Nullable
    public String getAccountId() {
        return getIdentifier(TestType.ACCOUNT);
    }
}
