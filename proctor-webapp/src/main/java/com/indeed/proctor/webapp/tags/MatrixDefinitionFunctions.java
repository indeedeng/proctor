package com.indeed.proctor.webapp.tags;

import com.indeed.proctor.webapp.controllers.ProctorController;

import java.util.Collection;

/**
 * @author parker
 */
public final class MatrixDefinitionFunctions {
    public static boolean hasDevInstances(final Collection<ProctorController.CompatibilityRow> rows) {
        for (final ProctorController.CompatibilityRow row : rows) {
            if (!row.getDev().isEmpty()) {
                return true;
            }
        }
        return false;
    }

}
