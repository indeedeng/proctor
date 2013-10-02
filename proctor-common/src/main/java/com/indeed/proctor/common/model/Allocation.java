package com.indeed.proctor.common.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class Allocation {
    @Nullable
    private String rule;

    /**
     * Map from a bucket name
     */
    @Nonnull
    private List<Range> ranges = Collections.emptyList();

    public Allocation() { /* intentionally empty */ }

    public Allocation(@Nullable final String rule, @Nonnull final List<Range> ranges) {
        this.rule = rule;
        this.ranges = ranges;
    }

    @Nullable
    public String getRule() {
        return rule;
    }

    @SuppressWarnings("UnusedDeclaration")
    public void setRule(@Nullable final String rule) {
        this.rule = rule;
    }

    @Nonnull
    public List<Range> getRanges() {
        return ranges;
    }

    public void setRanges(@Nonnull final List<Range> ranges) {
        this.ranges = ranges;
    }
}