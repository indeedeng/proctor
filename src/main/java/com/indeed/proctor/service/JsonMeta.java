package com.indeed.proctor.service;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
