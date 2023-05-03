package com.indeed.proctor.pipet.core.model;

import java.util.Collections;
import java.util.Map;

/**
 * Response used for errors where there is no data to provide.
 *
 * In this case, data should be an empty object, not null.
 */
public class JsonEmptyDataResponse extends JsonResponse<Map> {
    public JsonEmptyDataResponse(final JsonMeta meta) {
        super(Collections.emptyMap(), meta);
    }
}
