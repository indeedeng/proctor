package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.common.model.TestDefinition;

import java.util.Map;

public abstract class AbstractDefinitionDeleteChange
        implements PreDefinitionDeleteChange, PostDefinitionDeleteChange {
    @Override
    public void postDelete(
            final Map<String, String[]> extensionFields, final DefinitionChangeLogger logger) {}

    @Override
    public void preDelete(
            final TestDefinition testDefinition,
            final Map<String, String[]> extensionFields,
            final DefinitionChangeLogger logger) {}
}
