package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.common.model.TestDefinition;

import java.util.Map;

/**
 */
public abstract class AbstractDefinitionCreateChange implements PreDefinitionCreateChange, PostDefinitionCreateChange{
    public DefinitionChangeLog preCreate(final TestDefinition testDefinition, final Map<String, String[]> extensionFields) {
        return null;
    }


    public DefinitionChangeLog postCreate(final TestDefinition testDefinition, final Map<String, String[]> extensionFields) {
        return null;
    }
}