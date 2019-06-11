package com.indeed.proctor.store;

import java.util.Objects;
import java.util.Set;

/**
 * Details of a single revision
 */
public class RevisionDetails {
    private final Revision revision;
    private final Set<String> modifiedTests;

    public RevisionDetails(
            final Revision revision,
            final Set<String> modifiedTests
    ) {
        this.revision = revision;
        this.modifiedTests = modifiedTests;
    }

    public Revision getRevision() {
        return revision;
    }

    public Set<String> getModifiedTests() {
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
        return Objects.equals(revision, that.revision) &&
                Objects.equals(modifiedTests, that.modifiedTests);
    }

    @Override
    public int hashCode() {
        return Objects.hash(revision, modifiedTests);
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("revision", revision)
                .add("modifiedTests", modifiedTests)
                .toString();
    }
}
