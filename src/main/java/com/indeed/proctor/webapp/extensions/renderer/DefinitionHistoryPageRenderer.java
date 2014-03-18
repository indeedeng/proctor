package com.indeed.proctor.webapp.extensions.renderer;

import com.indeed.proctor.store.Revision;

/**
 */
public interface DefinitionHistoryPageRenderer {
    enum DefinitionHistoryPagePosition {
        PROMOTE_FORM_BOTTOM,
    }

    DefinitionHistoryPagePosition getDefinitionHistoryPagePositionPosition();

    public String getRenderedHtml(final String testName, final Revision testDefinitionVersion);
}
