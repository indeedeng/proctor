package com.indeed.proctor.webapp.views;

import com.indeed.proctor.common.Serializers;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.springframework.web.servlet.View;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author parker
 */
public class JsonView implements View {

    private static final String CONTENT_TYPE = "application/json;charset=utf-8";

    private static final ObjectWriter JSON_WRITER = Serializers.strict().writerWithDefaultPrettyPrinter();
    private final Object data;

    public JsonView(Object data) {
        this.data = data;
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    @Override
    public void render(Map<String, ?> model,
                       HttpServletRequest request,
                       HttpServletResponse response) throws Exception {
        response.setHeader("Content-Type", CONTENT_TYPE);
        JSON_WRITER.writeValue(response.getWriter(), data);
    }
}
