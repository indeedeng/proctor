package com.indeed.proctor.common;

import javax.annotation.Nullable;

public class SpecificationResult {
    @Nullable
    private ProctorSpecification specification;
    @Nullable
    private String error;
    @Nullable
    private String exception;

    @Nullable
    public ProctorSpecification getSpecification() {
        return specification;
    }

    public void setSpecification(@Nullable final ProctorSpecification specification) {
        this.specification = specification;
    }

    @Nullable
    public String getError() {
        return error;
    }

    public void setError(@Nullable final String error) {
        this.error = error;
    }

    @SuppressWarnings("UnusedDeclaration") // For json
    @Nullable
    public String getException() {
        return exception;
    }

    public void setException(@Nullable final String exception) {
        this.exception = exception;
    }
}