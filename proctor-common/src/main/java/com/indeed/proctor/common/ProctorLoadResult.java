package com.indeed.proctor.common;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author matts
 */
public class ProctorLoadResult {
    @Nonnull
    private final Map<String, String> testErrorMap;
    @Nonnull
    private final Set<String> missingTests;

    private final boolean verifiedRules;

    public ProctorLoadResult(
            @Nonnull final Set<String> testsWithErrors,
            @Nonnull Set<String> missingTests
    ) {
        this.testErrorMap = makeTestErrorMap(testsWithErrors);
        this.missingTests = missingTests;
        this.verifiedRules = false;
    }

    public ProctorLoadResult(
            @Nonnull final Set<String> testsWithErrors,
            @Nonnull Set<String> missingTests,
            final boolean verifiedRules
    ) {
        this.testErrorMap = makeTestErrorMap(testsWithErrors);
        this.missingTests = missingTests;
        this.verifiedRules = verifiedRules;
    }

    public ProctorLoadResult(
        @Nonnull final Map<String, String> testErrorMap,
        @Nonnull Set<String> missingTests,
        final boolean verifiedRules
    ) {
        this.testErrorMap = testErrorMap;
        this.missingTests = missingTests;
        this.verifiedRules = verifiedRules;
    }

    @Nonnull
    public Set<String> getTestsWithErrors() {
        return testErrorMap.keySet();
    }

    @Nonnull
    public Map<String, String> getTestErrorMap() {
        return testErrorMap;
    }

    @Nonnull
    public Set<String> getMissingTests() {
        return missingTests;
    }

    public boolean getVerifiedRules() {
        return verifiedRules;
    }

    private static Map<String, String> makeTestErrorMap(final Set<String> testsWithErrors) {
        return Maps.asMap(testsWithErrors, new Function<String, String>() {
            @Override
            public String apply(final String testName) {
                return testName + " has an invalid specification";
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
        private ImmutableMap.Builder<String, String> testsWithErrors = ImmutableMap.builder();
        private ImmutableSet.Builder<String> missingTests = ImmutableSet.builder();
        private boolean verifiedRules = false;
        private Builder() { }

        @Nonnull
        public Builder recordError(final String testName, final String message) {
            testsWithErrors.put(testName, message);
            return this;
        }

        @Nonnull
        public Builder recordError(final String testName) {
            testsWithErrors.put(testName, testName + " has an invalid specification");
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
            return new ProctorLoadResult(testsWithErrors.build(), missingTests.build(), verifiedRules);
        }
    }
}
