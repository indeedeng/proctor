package com.indeed.proctor.common;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import com.indeed.proctor.common.IncompatibleTestMatrixException;
import com.indeed.proctor.common.model.TestMatrixArtifact;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author matts
 */
public class ProctorLoadResult {
    @Nonnull
    private final Map<String, IncompatibleTestMatrixException> testErrorMap;
    @Nonnull
    private final Set<String> missingTests;
    @Nonnull
    private final Map<String, IncompatibleTestMatrixException> dynamicTestErrorMap;

    private final boolean verifiedRules;

    /**
     * @deprecated Use {@link ProctorLoadResult#newBuilder()}
     */
    @Deprecated
    public ProctorLoadResult(
            @Nonnull final Set<String> testsWithErrors,
            @Nonnull Set<String> missingTests
    ) {
        this(testsWithErrors, missingTests, false);
    }

    /**
     * @deprecated Use {@link ProctorLoadResult#newBuilder()}
     */
    @Deprecated
    public ProctorLoadResult(
            @Nonnull final Set<String> testsWithErrors,
            @Nonnull Set<String> missingTests,
            final boolean verifiedRules
    ) {
        this(makeTestErrorMap(testsWithErrors), missingTests, verifiedRules);
    }

    /**
     * @deprecated Use {@link ProctorLoadResult#newBuilder()}
     */
    @Deprecated
    public ProctorLoadResult(
        @Nonnull final Map<String, IncompatibleTestMatrixException> testErrorMap,
        @Nonnull Set<String> missingTests,
        final boolean verifiedRules
    ) {
        this(
                testErrorMap,
                Collections.<String, IncompatibleTestMatrixException>emptyMap(),
                missingTests,
                verifiedRules
        );
    }

    /**
     * @deprecated Use {@link ProctorLoadResult#newBuilder()}
     */
    @Deprecated
    public ProctorLoadResult(
            @Nonnull final Map<String, IncompatibleTestMatrixException> testErrorMap,
            @Nonnull final Map<String, IncompatibleTestMatrixException> dynamicTestErrorMap,
            @Nonnull Set<String> missingTests,
            final boolean verifiedRules
    ) {
        this.testErrorMap = testErrorMap;
        this.dynamicTestErrorMap = dynamicTestErrorMap;
        this.missingTests = missingTests;
        this.verifiedRules = verifiedRules;
    }

    @Nonnull
    public Set<String> getTestsWithErrors() {
        return testErrorMap.keySet();
    }

    @Nonnull
    public Set<String> getDynamicTestWithErrors() {
        return dynamicTestErrorMap.keySet();
    }

    @Nonnull
    public Map<String, IncompatibleTestMatrixException> getTestErrorMap() {
        return testErrorMap;
    }

    /**
     * Returns map from test name to incompatible test matrix exception for tests resolved by dynamic filter.
     */
    @Nonnull
    public Map<String, IncompatibleTestMatrixException> getDynamicTestErrorMap() {
        return dynamicTestErrorMap;
    }

    @Nonnull
    public Set<String> getMissingTests() {
        return missingTests;
    }

    public boolean getVerifiedRules() {
        return verifiedRules;
    }

    private static Map<String, IncompatibleTestMatrixException> makeTestErrorMap(final Set<String> testsWithErrors) {
        return Maps.asMap(testsWithErrors, new Function<String, IncompatibleTestMatrixException>() {
            @Override
            public IncompatibleTestMatrixException apply(final String testName) {
                return new IncompatibleTestMatrixException(testName + " has an invalid specification");
            }
        });
    }

    @SuppressWarnings("UnusedDeclaration")
    public boolean hasInvalidTests() {
        return !(testErrorMap.isEmpty() && missingTests.isEmpty());
    }

    private static final ProctorLoadResult EMPTY = newBuilder().build();
    @Nonnull
    public static ProctorLoadResult emptyResult() {
        return EMPTY;
    }

    @Nonnull
    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private ImmutableMap.Builder<String, IncompatibleTestMatrixException> testsWithErrors = ImmutableMap.builder();
        private ImmutableMap.Builder<String, IncompatibleTestMatrixException> dynamicTestsWithErrors = ImmutableMap.builder();
        private ImmutableSet.Builder<String> missingTests = ImmutableSet.builder();
        private boolean verifiedRules = false;
        private Builder() { }

        @Nonnull
        public Builder recordError(final String testName, final IncompatibleTestMatrixException exception) {
            testsWithErrors.put(testName, exception);
            return this;
        }

        /**
         * Record incompatible test matrix exception thrown by {@link ProctorUtils#verify} for dynamic tests
         */
        @Nonnull
        public Builder recordIncompatibleDynamicTest(final String testName, final IncompatibleTestMatrixException exception) {
            dynamicTestsWithErrors.put(testName, exception);
            return this;
        }

        @Nonnull
        public Builder recordError(final String testName) {
            testsWithErrors.put(testName, new IncompatibleTestMatrixException(testName + " has an invalid specification"));
            return this;
        }

        @Nonnull
        public Builder recordMissing(final String testName) {
            missingTests.add(testName);
            return this;
        }

        @Nonnull
        public Builder recordAllMissing(final Collection<String> testNames) {
            missingTests.addAll(testNames);
            return this;
        }

        public Builder recordVerifiedRules(final boolean verifiedRulesInput){
            verifiedRules = verifiedRulesInput;
            return this;
        }

        @Nonnull
        public ProctorLoadResult build() {
            return new ProctorLoadResult(
                    testsWithErrors.build(),
                    dynamicTestsWithErrors.build(),
                    missingTests.build(),
                    verifiedRules
            );
        }
    }
}
