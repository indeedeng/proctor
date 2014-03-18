package com.indeed.proctor.webapp.extensions.renderer;

import com.indeed.proctor.webapp.db.Environment;

/**
 */
public interface BasePageRenderer {
    enum BasePagePosition {
        NAVBAR_BUTTON,
    }

    BasePagePosition getBasePagePosition();

    public String getRenderedHtml(final Environment branch);
}
