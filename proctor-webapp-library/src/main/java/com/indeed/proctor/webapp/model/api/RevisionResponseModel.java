package com.indeed.proctor.webapp.model.api;

import com.indeed.proctor.store.Revision;
import io.swagger.annotations.ApiModelProperty;

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.Objects;

/**
 * API Model class for {@link Revision}
 */
public class RevisionResponseModel {
    @Nonnull
    private final String revision;
    @Nonnull
    private final String author;
    @Nonnull
    private final Date date;
    @Nonnull
    private final String message;

    RevisionResponseModel(
            @Nonnull final String revision,
            @Nonnull final String author,
            @Nonnull final Date date,
            @Nonnull final String message
    ) {
        this.revision = revision;
        this.author = author;
        this.date = date;
        this.message = message;
    }

    @Nonnull
    @ApiModelProperty("revision id")
    public String getRevision() {
        return revision;
    }

    @Nonnull
    @ApiModelProperty("author name")
    public String getAuthor() {
        return author;
    }

    @Nonnull
    @ApiModelProperty("modified date")
    public Date getDate() {
        return date;
    }

    @Nonnull
    @ApiModelProperty("description of the change by the author and proctor webapp")
    public String getMessage() {
        return message;
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
        return Objects.equals(revision, that.revision) &&
                Objects.equals(author, that.author) &&
                Objects.equals(date, that.date) &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(revision, author, date, message);
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("revision", revision)
                .add("author", author)
                .add("date", date)
                .add("message", message)
                .toString();
    }

    public static RevisionResponseModel fromRevision(final Revision revision) {
        return new RevisionResponseModel(
                revision.getRevision(),
                revision.getAuthor(),
                revision.getDate(),
                revision.getMessage()
        );
    }
}
