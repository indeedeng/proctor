package com.indeed.proctor.webapp.extensions.renderer;

import com.indeed.proctor.common.model.TestDefinition;

/**
 */
public interface DefinitionDetailsPageRenderer {
    enum DefinitionDetailsPagePosition {
        TOP,
        MIDDLE,
        BOTTOM
    }

    DefinitionDetailsPagePosition getDefinitionDetailsPagePosition();

    public String getRenderedHtml(final String testName, final TestDefinition testDefinition);
}
