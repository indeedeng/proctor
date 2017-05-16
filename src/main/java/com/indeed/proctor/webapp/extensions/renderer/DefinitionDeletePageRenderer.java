package com.indeed.proctor.webapp.extensions.renderer;

import javax.servlet.jsp.PageContext;

/**
 */
public interface DefinitionDeletePageRenderer {
    enum DefinitionDeletePagePosition {
        TOP_FORM,
        MIDDLE_FORM,
        BOTTOM_FORM,
        SCRIPT
    }

    DefinitionDeletePagePosition getDefinitionDeletePagePosition();

    @Deprecated
    default String getRenderedHtml(final String testName){
        return "";
    }

    default String getRenderedHtml(final PageContext pageContext, final String testName){
        return "";
    }
}
