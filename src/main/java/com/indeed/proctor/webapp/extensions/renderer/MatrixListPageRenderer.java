package com.indeed.proctor.webapp.extensions.renderer;

import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;

import javax.servlet.jsp.PageContext;

/**
 */
public interface MatrixListPageRenderer {
    enum MatrixListPagePosition {
        TOP,
        LINK,
        BOTTOM
    }

    MatrixListPagePosition getMatrixListPagePosition();

    @Deprecated
    default String getRenderedHtml(final String testName, final TestMatrixVersion testMatrixVersion, final TestDefinition testDefinition){
        return "";
    }

    default String getRenderedHtml(final PageContext pageContext, final String testName, final TestMatrixVersion testMatrixVersion, final TestDefinition testDefinition){
        return "";
    }
}
