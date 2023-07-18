package com.indeed.proctor.common;

import com.indeed.proctor.common.model.TestType;

import javax.annotation.Nonnull;

/**
 * An interface to validate an identifier before determining groups for tests. {@link Proctor} won't
 * assign any bucket for the identifier and test type when it's classified as invalid. An example
 * case to valid against is a dummy account id that indicates the user is logged out.
 */
public interface IdentifierValidator {
    /** @return true iff the identifier is valid as a value for the test type. */
    boolean validate(@Nonnull TestType testType, @Nonnull String identifier);

    /** A validator that accepts any identifiers. */
    class Noop implements IdentifierValidator {
        @Override
        public boolean validate(
                @Nonnull final TestType testType, @Nonnull final String identifier) {
            return true;
        }
    }
}
