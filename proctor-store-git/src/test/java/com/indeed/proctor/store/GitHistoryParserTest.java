package com.indeed.proctor.store;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GitHistoryParserTest {

    private static int revisionCount = 1;

    @Test
    public void testSortByDate() {
        final Map<String, List<Revision>> testee = Maps.newHashMap();
        testee.put(
                "test1",
                Lists.newArrayList(
                        makeRandomRevision(5), makeRandomRevision(8), makeRandomRevision(1)));
        testee.put("test2", Lists.newArrayList(makeRandomRevision(10)));

        GitHistoryParser.sortByDate(testee);

        /** assert order is correct * */
        for (final List<Revision> revisions : testee.values()) {
            Date prevDate = revisions.get(0).getDate();
            for (final Revision revision : revisions) {
                assertTrue(prevDate.compareTo(revision.getDate()) >= 0);
                prevDate = revision.getDate();
            }
        }
    }

    @Test
    public void testTestNamePattern() {
        final Pattern pattern =
                GitHistoryParser.compileTestNamePattern("matrices/test-definitions");
        Matcher matcher = pattern.matcher("matrices/test-definitions/testname/definition.json");
        assertTrue(matcher.matches());
        assertEquals("testname", matcher.group(1));

        matcher = pattern.matcher("matrices/test-definitions/testname/definition");
        assertFalse(matcher.matches());
    }

    private static Revision makeRandomRevision(final long date) {
        final Revision result =
                new Revision(
                        UUID.randomUUID().toString(),
                        "author",
                        new Date(date),
                        String.valueOf(revisionCount));
        revisionCount++;
        return result;
    }
}
