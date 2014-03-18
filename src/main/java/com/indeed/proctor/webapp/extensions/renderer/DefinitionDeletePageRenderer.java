package com.indeed.proctor.webapp.extensions.renderer;

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

    public String getRenderedHtml(final String testName);
}
