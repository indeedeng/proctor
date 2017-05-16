package com.indeed.proctor.webapp.extensions.renderer;

import com.indeed.proctor.webapp.db.Environment;

import javax.servlet.jsp.PageContext;

/**
 */
public interface BasePageRenderer {
    enum BasePagePosition {
        NAVBAR_BUTTON,
        FOOTER,
    }

    BasePagePosition getBasePagePosition();

    @Deprecated
    default String getRenderedHtml(final Environment branch) {
        return "";
    }

    default String getRenderedHtml(final PageContext pageContext, final Environment branch) {
        return "";
    }
}
