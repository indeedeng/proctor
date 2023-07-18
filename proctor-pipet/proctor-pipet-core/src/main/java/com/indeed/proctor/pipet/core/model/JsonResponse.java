package com.indeed.proctor.pipet.core.model;

/**
 * Standard envelope response to JSON web requests.
 *
 * <p>data should be a JSON-serializable object, like a JsonResult.
 */
public class JsonResponse<JsonData> {
    private final JsonData data;
    private final JsonMeta meta;

    public JsonResponse(final JsonData data, final JsonMeta meta) {
        this.data = data;
        this.meta = meta;
    }

    public JsonData getData() {
        return data;
    }

    public JsonMeta getMeta() {
        return meta;
    }
}
