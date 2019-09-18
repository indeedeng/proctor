package com.indeed.proctor.webapp.model;

import com.indeed.proctor.common.ProctorSpecification;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;

/**
 * Model to represents a result of attempt
 * to fetch proctor specifications from running instance.
 */
public class SpecificationResult {
    @Nonnull
    private final ProctorSpecifications specifications;
    @Nullable
    private final String error;

    private SpecificationResult(
            @Nonnull final ProctorSpecifications specifications,
            @Nullable final String error
    ) {
        this.specifications = specifications;
        this.error = error;
    }

    @Nonnull
    public ProctorSpecifications getSpecifications() {
        return specifications;
    }

    @Nullable
    public String getError() {
        return error;
    }

    public boolean isFailed() {
        return error != null;
    }

    public static SpecificationResult success(
            @Nonnull final Iterable<ProctorSpecification> specifications
    ) {
        return new SpecificationResult(
                new ProctorSpecifications(specifications),
                null
        );
    }

    public static SpecificationResult failed(
            @Nonnull final String error
    ) {
        return new SpecificationResult(
                new ProctorSpecifications(Collections.emptySet()),
                error
        );
    }
}
