package com.indeed.proctor.webapp.model.api;

import com.indeed.proctor.store.RevisionDetails;
import io.swagger.annotations.ApiModelProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * API Model class for {@link RevisionDetails}
 */
public class RevisionDetailsResponseModel {
    @Nonnull
    private final RevisionResponseModel revision;
    @Nonnull
    private final List<String> modifiedTests;

    RevisionDetailsResponseModel(
            @Nonnull final RevisionResponseModel revision,
            @Nonnull final List<String> modifiedTests
    ) {
        this.revision = revision;
        this.modifiedTests = modifiedTests;
    }

    @Nonnull
    @ApiModelProperty("revision model")
    public RevisionResponseModel getRevision() {
        return revision;
    }

    @Nonnull
    @ApiModelProperty("a list of test names that is modified in the revision")
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
        final RevisionDetailsResponseModel that = (RevisionDetailsResponseModel) o;
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

    public static RevisionDetailsResponseModel fromRevisionDetails(final RevisionDetails revisionDetails) {
        return new RevisionDetailsResponseModel(
                RevisionResponseModel.fromRevision(revisionDetails.getRevision()),
                revisionDetails.getModifiedTests().stream()
                        .sorted()
                        .collect(Collectors.toList())
        );
    }
}
