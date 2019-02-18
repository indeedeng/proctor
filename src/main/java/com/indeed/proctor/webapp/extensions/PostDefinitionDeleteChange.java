package com.indeed.proctor.webapp.extensions;

import java.util.Map;

public interface PostDefinitionDeleteChange {
    void postDelete(final Map<String, String[]> extensionFields, final DefinitionChangeLog changeLog);
}
