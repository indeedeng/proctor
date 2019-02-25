package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.common.model.TestDefinition;

import java.util.Map;

public interface PreDefinitionDeleteChange {
    void preDelete(
            final TestDefinition testDefinition,
            final Map<String, String[]> extensionFields,
            final DefinitionChangeLogger logger
    );
}
