package com.indeed.proctor.webapp.extensions.renderer;

import javax.servlet.jsp.PageContext;

/**
 */
public interface EditPageRenderer {
    enum EditPagePosition {
        TOP_FORM,
        MIDDLE_FORM,
        BOTTOM_FORM,
        SCRIPT,
        SPECIAL_CONSTANTS
    }

    EditPagePosition getEditPagePosition();

    @Deprecated
    default String getRenderedHtml(final String testName, final String testDefinitionJson, final boolean isCreate){
        return "";
    }

    default String getRenderedHtml(final PageContext pageContext, final String testName, final String testDefinitionJson, final boolean isCreate){
        return "";
    }
}
