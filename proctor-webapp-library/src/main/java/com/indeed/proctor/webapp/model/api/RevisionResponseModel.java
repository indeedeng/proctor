package com.indeed.proctor.webapp.model.api;

import com.indeed.proctor.store.Revision;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;
import java.util.Objects;

/**
 * API Model class for {@link Revision}
 */
public class RevisionResponseModel {
    private final Revision revision;

    public RevisionResponseModel(final Revision revision) {
        this.revision = revision;
    }

    @ApiModelProperty("revision id")
    public String getRevision() {
        return revision.getRevision();
    }

    @ApiModelProperty("author name")
    public String getAuthor() {
        return revision.getAuthor();
    }

    @ApiModelProperty("modified date")
    public Date getDate() {
        return revision.getDate();
    }

    @ApiModelProperty("description of the change by the author and proctor webapp")
    public String getMessage() {
        return revision.getMessage();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final RevisionResponseModel that = (RevisionResponseModel) o;
        return Objects.equals(revision, that.revision);
    }

    @Override
    public int hashCode() {
        return Objects.hash(revision);
    }
}
