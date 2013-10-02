package com.indeed.proctor.store;

import java.util.Date;

public class Revision {
    private final long revision;
    private final String author;
    private final Date date;
    private final String message;

    public Revision(final long revision, final String author, final Date date, final String message) {
        this.revision = revision;
        this.author = author;
        this.date = date;
        this.message = message;
    }

    public long getRevision() {
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
}
