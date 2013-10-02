package com.indeed.proctor.common;

import javax.annotation.Nullable;

/**
 * Models the payload specification part of a test specification.
 *
 * @author pwp
 */
public class PayloadSpecification {
    @Nullable
    private String type;
    @Nullable
    private String validator;

    @Nullable
    public String getType() {
        return type;
    }

    public void setType(@Nullable final String type) {
        this.type = type;
    }

    @Nullable
    public String getValidator() {
        return validator;
    }

    public void setValidator(@Nullable final String validator) {
        this.validator = validator;
    }
}
