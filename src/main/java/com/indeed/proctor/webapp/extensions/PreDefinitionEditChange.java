package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.common.model.TestDefinition;

import java.util.Map;

public interface PreDefinitionEditChange {
    void preEdit(
            final TestDefinition existingTestDefinition,
            final TestDefinition testDefinitionToUpdate,
            final Map<String, String[]> extensionFields,
            final DefinitionChangeLogger logger
    );
}
