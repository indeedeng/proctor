package com.indeed.proctor.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import java.util.Date;

public class Revision {
    private final String revision;
    private final String author;
    private final Date date;
    private final String message;

    @JsonCreator
    public Revision(
            @JsonProperty("revision") final String revision,
            @JsonProperty("author") final String author,
            @JsonProperty("date") final Date date,
            @JsonProperty("message") final String message
    ) {
        this.revision = revision;
        this.author = author;
        this.date = date;
        this.message = message;
    }

    public String getRevision() {
        return revision;
    }

    public String getAuthor() {
        return author;
    }

    public Date getDate() {
        return date;
    }

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
        final Revision revision1 = (Revision) o;
        return Objects.equal(revision, revision1.revision) &&
                Objects.equal(author, revision1.author) &&
                Objects.equal(date, revision1.date) &&
                Objects.equal(message, revision1.message);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(revision, author, date, message);
    }

    @Override
    public String toString() {
        return "Revision{" +
                "revision='" + revision + '\'' +
                ", author='" + author + '\'' +
                ", date=" + date +
                ", message='" + message + '\'' +
                '}';
    }
}
