package com.indeed.proctor.store;

import com.indeed.proctor.common.model.TestDefinition;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * A class representing an edit of a test
 * definition is null when the edit was for deleting the test
 */
public class TestEdit {
    private final Revision revision;
    private final TestDefinition definition;

    public TestEdit(final Revision revision, @Nullable final TestDefinition definition) {
        this.revision = revision;
        this.definition = definition;
    }

    public Revision getRevision() {
        return revision;
    }

    @Nullable
    public TestDefinition getDefinition() {
        return definition;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TestEdit testEdit = (TestEdit) o;
        return Objects.equals(revision, testEdit.revision) &&
                Objects.equals(definition, testEdit.definition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(revision, definition);
    }
}
