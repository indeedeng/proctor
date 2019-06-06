package com.indeed.proctor.store;

import java.util.List;
import java.util.Objects;

/**
 * Details of a single revision
 */
public class RevisionDetails {
    private final Revision revision;
    private final List<String> modifiedTests;

    public RevisionDetails(
            final Revision revision,
            final List<String> modifiedTests
    ) {
        this.revision = revision;
        this.modifiedTests = modifiedTests;
    }

    public Revision getRevision() {
        return revision;
    }

    public List<String> getModifiedTests() {
        return modifiedTests;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final RevisionDetails that = (RevisionDetails) o;
        return Objects.equals(modifiedTests, that.modifiedTests);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modifiedTests);
    }
}
