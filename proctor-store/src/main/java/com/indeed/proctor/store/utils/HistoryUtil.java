package com.indeed.proctor.store.utils;

import com.indeed.proctor.store.Revision;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class HistoryUtil {
    public static <T> List<T> selectHistorySet(
            @Nullable final List<T> histories,
            final int start,
            final int limit
    ) {
        if ((histories == null) || (start >= histories.size()) || (limit < 1)) {
            return Collections.emptyList();
        }
        final int s = Math.max(start, 0);
        final int l = Math.min(limit, histories.size() - s); /* to avoid overflow */
        return histories.subList(s, s + l);
    }

    public static List<Revision> selectRevisionHistorySetFrom(
            @Nullable final List<Revision> histories,
            final String from,
            final int start,
            final int limit
    ) {
        if (histories == null) {
            return Collections.emptyList();
        }

        int i = 0;
        for (final Revision rev : histories) {
            if (rev.getRevision().equals(from)) {
                break;
            }
            i++;
        }
        return selectHistorySet(histories, start + i, limit);
    }
}
