package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.common.model.TestDefinition;

import java.util.Map;

public abstract class AbstractDefinitionEditChange implements PreDefinitionEditChange, PostDefinitionEditChange{
    @Override
    public void postEdit(
            final TestDefinition oldTestDefinition,
            final TestDefinition newTestDefinition,
            final Map<String, String[]> extensionFields,
            final DefinitionChangeLog changeLog
    ) {
    }

    @Override
    public void preEdit(
            final TestDefinition existingTestDefinition,
            final TestDefinition testDefinitionToUpdate,
            final Map<String, String[]> extensionFields,
            final DefinitionChangeLog changeLog
    ) {
    }
}