package com.indeed.proctor.store;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Set;

/**
 * Details of a single revision
 */
public class RevisionDetails {
    @Nonnull
    private final Revision revision;
    @Nonnull
    private final Set<String> modifiedTests;

    public RevisionDetails(
            @Nonnull final Revision revision,
            @Nonnull final Set<String> modifiedTests
    ) {
        this.revision = revision;
        this.modifiedTests = modifiedTests;
    }

    @Nonnull
    public Revision getRevision() {
        return revision;
    }

    @Nonnull
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
