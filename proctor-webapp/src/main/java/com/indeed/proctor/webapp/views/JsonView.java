package com.indeed.proctor.webapp.views;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.indeed.proctor.common.Serializers;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * @author parker
 */
public class JsonView implements View {

    private static final String CONTENT_TYPE = "application/json;charset=utf-8";
    private static final Logger LOGGER = LogManager.getLogger(JsonView.class);

    private static final ObjectWriter JSON_WRITER = Serializers.strict()
            .setDateFormat(new StdDateFormat().withColonInTimeZone(false))
            .writerWithDefaultPrettyPrinter();
    private final Object data;

    public JsonView(final Object data) {
        this.data = data;
    }

    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

    @Override
    public void render(final Map<String, ?> model,
                       final HttpServletRequest request,
                       final HttpServletResponse response) throws Exception {
        response.setHeader("Content-Type", CONTENT_TYPE);

        try {
            JSON_WRITER.writeValue(response.getWriter(), data);
        } catch (final IOException e) {
            if (isDisconnectedClientError(e)) {
                LOGGER.warn("Client disconnected. " + e.getMessage());
            } else {
                throw e;
            }
        }
    }

    /**
     * True if the throwable is caused because client disconnected during process
     */
    private static boolean isDisconnectedClientError(final IOException e) {
        final Throwable cause = ExceptionUtils.getRootCause(e);
        return cause instanceof IOException && "broken pipe".equalsIgnoreCase(cause.getMessage());
    }
}
