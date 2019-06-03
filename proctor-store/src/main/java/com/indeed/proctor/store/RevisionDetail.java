package com.indeed.proctor.store;

import java.util.List;
import java.util.Objects;

/**
 * Details of a result that are hard to compute in History method.
 */
public class RevisionDetail {
    private final List<String> modifiedTests;

    public RevisionDetail(final List<String> modifiedTests) {
        this.modifiedTests = modifiedTests;
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
        final RevisionDetail that = (RevisionDetail) o;
        return Objects.equals(modifiedTests, that.modifiedTests);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modifiedTests);
    }
}
