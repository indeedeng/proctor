package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.common.model.TestDefinition;

import java.util.Map;

/**
 */
public abstract class AbstractDefinitionEditChange implements PreDefinitionEditChange, PostDefinitionEditChange{
    public DefinitionChangeLog preEdit(final TestDefinition existingTestDefinition, final TestDefinition testDefinitionToUpdate, final Map<String, String[]> extensionFields) {
        return null;
    }


    public DefinitionChangeLog postEdit(final TestDefinition testDefinition, final Map<String, String[]> extensionFields) {
        return null;
    }
}