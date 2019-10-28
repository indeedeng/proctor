package com.indeed.proctor.webapp.extensions.renderer;

import com.indeed.proctor.common.model.TestDefinition;

import javax.servlet.jsp.PageContext;

/**
 */
public interface DefinitionDetailsPageRenderer {
    enum DefinitionDetailsPagePosition {
        TOP,
        MIDDLE,
        BOTTOM
    }

    DefinitionDetailsPagePosition getDefinitionDetailsPagePosition();

    @Deprecated
    default String getRenderedHtml(final String testName, final TestDefinition testDefinition){
        return "";
    }

    default String getRenderedHtml(final PageContext pageContext, final String testName, final TestDefinition testDefinition){
        return "";
    }
}
