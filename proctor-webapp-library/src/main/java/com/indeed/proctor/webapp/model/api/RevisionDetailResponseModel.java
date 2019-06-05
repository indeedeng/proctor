package com.indeed.proctor.webapp.model.api;

import com.indeed.proctor.store.RevisionDetails;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Objects;

/**
 * API Model class for {@link RevisionDetails}
 */
public class RevisionDetailResponseModel {
    private final RevisionDetails revisionDetails;

    public RevisionDetailResponseModel(final RevisionDetails revisionDetails) {
        this.revisionDetails = revisionDetails;
    }

    @ApiModelProperty("revision model")
    public RevisionResponseModel getRevision() {
        return new RevisionResponseModel(revisionDetails.getRevision());
    }

    @ApiModelProperty("a list of test names that is modified in the revision")
    public List<String> getModifiedTests() {
        return revisionDetails.getModifiedTests();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final RevisionDetailResponseModel that = (RevisionDetailResponseModel) o;
        return Objects.equals(revisionDetails, that.revisionDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(revisionDetails);
    }
}
