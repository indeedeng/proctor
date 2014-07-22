package com.indeed.proctor.common;

import javax.annotation.Nullable;
import java.util.Map;

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
    private Map<String,String> schema;

    @Nullable
    public String getType() {
        return type;
    }

    public void setType(@Nullable final String type) {
        this.type = type;
    }

    public Map<String,String> getSchema() {
        return schema;
    }
    @Nullable
    public void setSchema(@Nullable final Map<String,String> schema) {
        this.schema = schema;
    }

    @Nullable
    public String getValidator() {
        return validator;
    }

    public void setValidator(@Nullable final String validator) {
        this.validator = validator;
    }
}
