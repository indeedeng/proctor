package com.indeed.proctor.store.utils;

import com.indeed.proctor.store.Revision;

import java.util.Collections;
import java.util.List;

public class HistoryUtil {
    public static <T> List<T> selectHistorySet(final List<T> histories, final int start, final int limit) {
        if ((histories == null) || (start >= histories.size()) || (limit < 1)) {
            return Collections.emptyList();
        }
        final int s = Math.max(start, 0);
        final int l = Math.min(limit, histories.size() - s); /** to avoid overflow**/
        return histories.subList(s, s + l);
    }

    public static List<Revision> selectRevisionHistorySetFrom(final List<Revision> history, final String from, final int start, final int limit) {
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
