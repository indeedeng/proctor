package com.indeed.proctor.webapp.extensions.renderer;

import com.indeed.proctor.store.Revision;

import javax.servlet.jsp.PageContext;

/** */
public interface DefinitionHistoryPageRenderer {
    enum DefinitionHistoryPagePosition {
        PROMOTE_FORM_BOTTOM,
    }

    DefinitionHistoryPagePosition getDefinitionHistoryPagePositionPosition();

    @Deprecated
    default String getRenderedHtml(final String testName, final Revision testDefinitionVersion) {
        return "";
    }

    default String getRenderedHtml(
            final PageContext pageContext,
            final String testName,
            final Revision testDefinitionVersion) {
        return "";
    }
}
