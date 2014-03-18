package com.indeed.proctor.webapp.extensions.renderer;

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

    public String getRenderedHtml(final String testName, final String testDefinitionJson, final boolean isCreate);
}
