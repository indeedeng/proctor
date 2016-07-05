package com.indeed.proctor.store.cache;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

/**
 *
 */
public class ProctorStoreCachingTest {


    final List<Integer> testee = Lists.newArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

    @Test
    public void testSelectHistorySet() {

        List<Integer> result;
        result = ProctorStoreCaching.selectHistorySet(testee, 0, 1);
        assertListEqual(Lists.newArrayList(1), result);

        result = ProctorStoreCaching.selectHistorySet(testee, -1, 1);
        assertListEqual(Lists.newArrayList(1), result);

        result = ProctorStoreCaching.selectHistorySet(testee, 2, -1);
        assertListEqual(Collections.<Integer>emptyList(), result);

        result = ProctorStoreCaching.selectHistorySet(testee, 0, Integer.MAX_VALUE);
        assertListEqual(testee, result);

        result = ProctorStoreCaching.selectHistorySet(testee, 0, Integer.MIN_VALUE);
        assertListEqual(Collections.<Integer>emptyList(), result);

        result = ProctorStoreCaching.selectHistorySet(testee, Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertListEqual(testee, result);
    }

    private static void assertListEqual(final List<Integer> a, final List<Integer> b) {
        Assert.assertArrayEquals(Iterables.toArray(a, Integer.class), Iterables.toArray(b, Integer.class));
    }
}