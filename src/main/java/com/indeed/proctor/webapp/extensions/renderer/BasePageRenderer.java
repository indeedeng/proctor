package com.indeed.proctor.webapp.extensions.renderer;

import com.indeed.proctor.webapp.db.Environment;

/**
 */
public interface BasePageRenderer {
    enum BasePagePosition {
        NAVBAR_BUTTON,
        FOOTER,
    }

    BasePagePosition getBasePagePosition();

    public String getRenderedHtml(final Environment branch);
}
