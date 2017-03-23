package com.indeed.proctor.common.model;

import com.google.common.collect.Maps;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Represents the entirety of the test specification artifact as the consumers should consume it
 * @author ketan
 */
public class TestMatrixArtifact {
    @Nullable
    private Audit audit;
    @Nonnull
    private Map<String, ConsumableTestDefinition> tests = Collections.emptyMap();

    @Nullable
    public Audit getAudit() {
        return audit;
    }

    public void setAudit(@Nullable final Audit audit) {
        this.audit = audit;
    }

    /* TODO: move somewhere better */
    public static Map<String, Object> collectionsToSets(@Nonnull final Map<String, Object> inputMap) {
        final Map<String, Object> newMap = Maps.newHashMap();
        for (final Entry<String, Object> entry : inputMap.entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();
            if (value instanceof Collection) {
                newMap.put(key, new HashSet<Object>((Collection<?>) value));
            } else {
                newMap.put(key, value);
            }
        }
        return newMap;
    }

    @Nonnull
    public Map<String, ConsumableTestDefinition> getTests() {
        // Return the mutable copy of the map because verification-and-consolidation rewrites it.
        // That should probably change to returning a modified clone as long as this class needs to remain public.
        return tests;
    }

    public void setTests(@Nonnull final Map<String, ConsumableTestDefinition> tests) {
        this.tests = Maps.newHashMap(tests);
    }
}
