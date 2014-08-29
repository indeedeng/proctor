package com.indeed.proctor.common;

import com.google.common.collect.ImmutableSet;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Set;

/**
 * @author matts
 */
public class ProctorLoadResult {
    @Nonnull
    private final Set<String> testsWithErrors;
    @Nonnull
    private final Set<String> missingTests;

    private final boolean verifiedRules;
    public ProctorLoadResult(
            @Nonnull final Set<String> testsWithErrors,
            @Nonnull Set<String> missingTests
    ) {
        this.testsWithErrors = testsWithErrors;
        this.missingTests = missingTests;
        this.verifiedRules = false;
    }
    public ProctorLoadResult(
            @Nonnull final Set<String> testsWithErrors,
            @Nonnull Set<String> missingTests,
            final boolean verifiedRules
    ) {
        this.testsWithErrors = testsWithErrors;
        this.missingTests = missingTests;
        this.verifiedRules = verifiedRules;
    }

    @Nonnull
    public Set<String> getTestsWithErrors() {
        return testsWithErrors;
    }

    @Nonnull
    public Set<String> getMissingTests() {
        return missingTests;
    }


    public boolean getVerifiedRules() {
        return verifiedRules;
    }
    @SuppressWarnings("UnusedDeclaration")
    public boolean hasInvalidTests() {
        return !(testsWithErrors.isEmpty() && missingTests.isEmpty());
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
        private ImmutableSet.Builder<String> testsWithErrors = ImmutableSet.builder();
        private ImmutableSet.Builder<String> missingTests = ImmutableSet.builder();
        private boolean verifiedRules = false;
        private Builder() { }

        @Nonnull
        public Builder recordError(final String testName) {
            testsWithErrors.add(testName);
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
