package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.common.model.TestDefinition;

import java.util.Map;

/**
 */
public abstract class AbstractDefinitionDeleteChange implements PreDefinitionDeleteChange, PostDefinitionDeleteChange{
    public DefinitionChangeLog preDelete(final TestDefinition testDefinition, final Map<String, String[]> extensionFields) {
        return null;
    }


    public DefinitionChangeLog postDelete(final Map<String, String[]> extensionFields) {
        return null;
    }
}