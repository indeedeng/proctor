package com.indeed.proctor.pipet.core.web;

import org.springframework.web.servlet.view.json.MappingJacksonJsonView;

/**
 * View for converting to JSON with Jackson 1.
 *
 * Use this View for all API responses. That guarantees that responses are consistently formatted.
 *
 * The JsonResponse should be the only object in the Model.
 *
 * This is guaranteed to use Jackson 1, which fixes a few problems related to JSON annotations.
 */
public class JsonResponseView extends MappingJacksonJsonView {
    public JsonResponseView() {
        super();
        setExtractValueFromSingleKeyModel(true);
        setPrettyPrint(true);
    }
}
