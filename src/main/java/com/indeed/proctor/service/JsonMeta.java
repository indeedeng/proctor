package com.indeed.proctor.service;

import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
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

    public String getError() {
        return error;
    }
}
