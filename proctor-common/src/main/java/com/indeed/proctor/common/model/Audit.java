package com.indeed.proctor.common.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Contains basic metadata about a test matrix for auditing purposes
 *
 * @author ketan
 */
public class Audit {
    public static final String EMPTY_VERSION = "";
    public static final TimeZone DEFAULT_TIMEZONE = TimeZone.getTimeZone("US/Central");

    private String version;
    private long updated;
    @Nullable private String updatedDate;
    @Nullable private String updatedBy;

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
        if (updated > 0) {
            final DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
            formatter.setTimeZone(DEFAULT_TIMEZONE);
            updatedDate = formatter.format(new Date(updated));
        } else {
            updatedDate = "";
        }
    }

    @Nullable
    public String getUpdatedDate() {
        return updatedDate;
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
        return "Audit{"
                + "version="
                + version
                + ", updated="
                + updated
                + ", updatedDate="
                + updatedDate
                + ", updatedBy='"
                + updatedBy
                + '\''
                + '}';
    }
}
