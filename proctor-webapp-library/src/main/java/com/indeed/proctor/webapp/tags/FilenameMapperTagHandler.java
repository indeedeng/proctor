package com.indeed.proctor.webapp.tags;

import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;
import java.io.IOException;
import java.util.Map;

public class FilenameMapperTagHandler extends TagSupport {
    private static final Logger LOGGER = Logger.getLogger(FilenameMapperTagHandler.class);

    public static final String FILENAME_MAPPER = "FILENAME_MAPPER";

    private String filename;

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    public int doStartTag() {
        try {
            pageContext.getOut().print(getVersionizedFilename(pageContext, filename));
        } catch (IOException e) {
            LOGGER.error("Failed to write versionized filename to page context", e);
        }
        return SKIP_BODY;
    }

    public static String getVersionizedFilename(final PageContext pageContext, final String filename) {
        return getVersionizedFilename((HttpServletRequest) pageContext.getRequest(), filename);
    }

    public static String getVersionizedFilename(final HttpServletRequest httpServletRequest, final String filename) {
        final Object mapperObject = httpServletRequest.getAttribute(FILENAME_MAPPER);
        if (mapperObject instanceof Map) {
            final Map mapper = (Map) mapperObject;
            if (mapper.containsKey(filename)) {
                return httpServletRequest.getContextPath() + mapper.get(filename).toString();
            }
        }
        return httpServletRequest.getContextPath() + filename;
    }
}

