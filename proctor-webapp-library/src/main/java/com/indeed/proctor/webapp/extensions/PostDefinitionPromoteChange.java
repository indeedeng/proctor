package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.webapp.db.Environment;

import java.util.Map;

public interface PostDefinitionPromoteChange {
    void postPromote(
            final Map<String, String[]> extensionFields,
            final Environment src,
            final Environment destination,
            final boolean isAutopromote,
            final DefinitionChangeLogger logger
    );
}
