package com.indeed.proctor.service;

/**
 * Standard envelope response to JSON web requests.
 *
 * data should be a JSON-serializable object, like a JsonResult.
 */
public class JsonResponse<JsonData> {
    private JsonData data;
    private JsonError error;

    public JsonResponse(final JsonData data, final JsonError error) {
        this.data = data;
        this.error = error;
    }

    public JsonData getData() {
        return data;
    }

    public JsonError getError() {
        return error;
    }
}
