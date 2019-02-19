package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.common.model.TestDefinition;

import java.util.Map;

/**
 */
public interface PostDefinitionEditChange {
    void postEdit(
            final TestDefinition oldTestDefinition,
            final TestDefinition newTestDefinition,
            final Map<String, String[]> extensionFields,
            final DefinitionChangeLogger logger
    );
}
