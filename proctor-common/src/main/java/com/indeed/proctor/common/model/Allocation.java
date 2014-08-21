package com.indeed.proctor.common.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
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

    public Allocation(final Allocation other) {
        this.rule = other.rule;
        this.ranges = new ArrayList<Range>();
        for (final Range range : other.getRanges()) {
            this.ranges.add(new Range(range));
        }
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