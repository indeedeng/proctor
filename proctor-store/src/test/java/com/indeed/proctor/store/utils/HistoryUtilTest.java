package com.indeed.proctor.store.utils;

import com.google.common.collect.Lists;
import com.indeed.proctor.store.Revision;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.indeed.proctor.store.utils.HistoryUtil.selectHistorySet;
import static com.indeed.proctor.store.utils.HistoryUtil.selectRevisionHistorySetFrom;
import static org.junit.Assert.assertEquals;

public class HistoryUtilTest {
    private static final List<Integer> DUMMY_HISTORY = Lists.newArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    private static final List<Revision> REVISION_HISTORY = Lists.newArrayList(
            makeRevision("1"),
            makeRevision("2"),
            makeRevision("3"),
            makeRevision("4"),
            makeRevision("5")
    );

    @Test
    public void testSelectHistorySet() {
        final List<Integer> result = selectHistorySet(DUMMY_HISTORY, 0, 1);
        assertEquals(Lists.newArrayList(1), result);
    }

    @Test
    public void testSelectHistorySetWithNegativeStart() {
        final List<Integer> result = selectHistorySet(DUMMY_HISTORY, -1, 1);
        assertEquals(Lists.newArrayList(1), result);
    }

    @Test
    public void testSelectHistorySetWithNonPositiveLimit() {
        final List<Integer> result = selectHistorySet(DUMMY_HISTORY, 2, -1);
        assertEquals(Collections.<Integer>emptyList(), result);
    }

    @Test
    public void testSelectHistorySetNotOverflow() {
        List<Integer> result = selectHistorySet(DUMMY_HISTORY, 0, Integer.MAX_VALUE);
        assertEquals(DUMMY_HISTORY, result);

        result = selectHistorySet(DUMMY_HISTORY, 0, Integer.MIN_VALUE);
        assertEquals(Collections.<Integer>emptyList(), result);

        result = selectHistorySet(DUMMY_HISTORY, Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertEquals(DUMMY_HISTORY, result);
    }

    @Test
    public void testSelectRevisionHistorySetFrom() {
        final List<Revision> result = selectRevisionHistorySetFrom(REVISION_HISTORY, "2", 0, 3);
        assertEquals(REVISION_HISTORY.subList(1, 4), result);
    }

    @Test
    public void testSelectRevisionHistorySetFromReturnEmtpy() {
        final List<Revision> result = selectRevisionHistorySetFrom(REVISION_HISTORY, "5", 1, 3);
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void testSelectRevisionHistorySetFromNonexistence() {
        final List<Revision> result = selectRevisionHistorySetFrom(REVISION_HISTORY, "hello", 0, 3);
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void testSelectRevisionHistorySetFromNullHistories() {
        final List<Revision> result = selectRevisionHistorySetFrom(null, "1", 0, 3);
        assertEquals(Collections.emptyList(), result);
    }

    private static Revision makeRevision(final String revision) {
        return new Revision(revision, "author", new Date(), "revision message");
    }
}
