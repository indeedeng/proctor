package com.indeed.proctor.webapp.extensions.renderer;

import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;

/**
 */
public interface MatrixListPageRenderer {
    enum MatrixListPagePosition {
        TOP,
        LINK,
        BOTTOM
    }

    MatrixListPagePosition getMatrixListPagePosition();

    public String getRenderedHtml(final String testName, final TestMatrixVersion testMatrixVersion, final TestDefinition testDefinition);
}
