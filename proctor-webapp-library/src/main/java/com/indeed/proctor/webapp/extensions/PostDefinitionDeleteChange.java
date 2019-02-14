package com.indeed.proctor.webapp.extensions;

import java.util.Map;

/**
 */
public interface PostDefinitionDeleteChange {
    public DefinitionChangeLog postDelete(final Map<String, String[]> extensionFields);
}
