package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.webapp.db.Environment;

import java.util.Map;

public interface PreDefinitionPromoteChange {
    void prePromote(
            final TestDefinition testDefinition,
            final Map<String, String[]> extensionFields,
            final Environment src,
            final Environment destination,
            final boolean isAutopromote,
            final DefinitionChangeLog changeLog
    );
}
