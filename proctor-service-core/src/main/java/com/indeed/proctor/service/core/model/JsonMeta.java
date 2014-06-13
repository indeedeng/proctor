package com.indeed.proctor.service.core.model;

import org.codehaus.jackson.map.annotate.JsonSerialize;

public class JsonMeta {
    // HTTP Response Code
    private final int status;

    private final String error;

    public JsonMeta(final int status) {
        this(status, null);
    }

    public JsonMeta(final int status, final String error) {
        this.status = status;
        this.error = error;
    }

    public int getStatus() {
        return status;
    }

    // No point in including an error message field if it's null.
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    public String getError() {
        return error;
    }
}
