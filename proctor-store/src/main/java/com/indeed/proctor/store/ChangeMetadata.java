package com.indeed.proctor.store;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Objects;

public class ChangeMetadata {
    private final String username;
    private final String password;
    private final String author;
    private final Instant timestamp;
    private final String comment;

    ChangeMetadata(final String username, final String password, final String author, final String comment) {
        this(username, password, author, Instant.now(), comment);
    }

    public ChangeMetadata(
            @Nullable final String username,
            @Nullable final String password,
            final String author,
            final Instant timestamp,
            final String comment
    ) {
        this.username = username;
        this.password = password;
        this.author = Objects.requireNonNull(author);
        this.timestamp = Objects.requireNonNull(timestamp);
        this.comment = Objects.requireNonNull(comment);
    }

    @Nullable
    public String getUsername() {
        return username;
    }

    @Nullable
    public String getPassword() {
        return password;
    }

    public String getAuthor() {
        return author;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getComment() {
        return comment;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("username", username)
                .append("password", password)
                .append("author", author)
                .append("timestamp", timestamp)
                .append("comment", comment)
                .toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ChangeMetadata that = (ChangeMetadata) o;
        return Objects.equals(username, that.username) &&
                Objects.equals(password, that.password) &&
                Objects.equals(author, that.author) &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(comment, that.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password, author, timestamp, comment);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String username;
        private String password;
        private String author;
        private Instant timestamp;
        private String comment;

        public Builder setUsername(final String username) {
            this.username = username;
            return this;
        }

        public Builder setPassword(final String password) {
            this.password = password;
            return this;
        }

        public Builder setAuthor(final String author) {
            this.author = author;
            return this;
        }

        public Builder setUsernameAndAuthor(final String username) {
            setUsername(username);
            setAuthor(username);
            return this;
        }

        public Builder setTimestamp(final Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder setComment(final String comment) {
            this.comment = comment;
            return this;
        }

        public ChangeMetadata build() {
            return new ChangeMetadata(
                    username,
                    password,
                    author,
                    timestamp == null ? Instant.now() : timestamp,
                    comment);
        }
    }
}
