package com.indeed.proctor.common.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Contains basic metadata about a test matrix for auditing purposes
 * @author ketan
 */
public class Audit {
    public static final String EMPTY_VERSION = "";

    private String version;
    private long updated;
    @Nullable
    private String updatedBy;

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public long getUpdated() {
        return updated;
    }

    public void setUpdated(final long updated) {
        this.updated = updated;
    }

    @Nullable
    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(@Nullable final String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Nonnull
    @Override
    public String toString() {
        return "Audit{" +
               "version=" + version +
               ", updated=" + updated +
               ", updatedBy='" + updatedBy + '\'' +
               '}';
    }
}
