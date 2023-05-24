package com.indeed.proctor.store.utils;

import com.indeed.proctor.store.Revision;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class HistoryUtil {
    /** @return sublist avoiding any overflow from bad parameters */
    public static <T> List<T> selectHistorySet(
            @Nullable final List<T> history, final int start, final int limit) {
        if ((history == null) || (start >= history.size()) || (limit < 1)) {
            return Collections.emptyList();
        }
        final int s = Math.max(start, 0);
        final int l = Math.min(limit, history.size() - s); /* to avoid overflow */
        return history.subList(s, s + l);
    }

    /** @return sublist starting at from avoiding any overflow from bad parameters */
    public static List<Revision> selectRevisionHistorySetFrom(
            @Nullable final List<Revision> history,
            final String from,
            final int start,
            final int limit) {
        if (history == null) {
            return Collections.emptyList();
        }

        int i = 0;
        for (final Revision rev : history) {
            if (rev.getRevision().equals(from)) {
                break;
            }
            i++;
        }
        return selectHistorySet(history, start + i, limit);
    }
}
