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

    public Allocation(@Nonnull final Allocation other) {
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Allocation that = (Allocation) o;

        if (rule != null ? !rule.equals(that.rule) : that.rule != null) {
            return false;
        }
        return ranges.equals(that.ranges);

    }

    @Override
    public int hashCode() {
        int result = rule != null ? rule.hashCode() : 0;
        result = 31 * result + ranges.hashCode();
        return result;
    }
}