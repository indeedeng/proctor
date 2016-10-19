package com.indeed.proctor.store.cache;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class ProctorStoreCachingTest {


    final List<Integer> testee = Lists.newArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

    @Test
    public void testSelectHistorySet() {

        List<Integer> result;
        result = ProctorStoreCaching.selectHistorySet(testee, 0, 1);
        assertEquals(Lists.newArrayList(1), result);

        result = ProctorStoreCaching.selectHistorySet(testee, -1, 1);
        assertEquals(Lists.newArrayList(1), result);

        result = ProctorStoreCaching.selectHistorySet(testee, 2, -1);
        assertEquals(Collections.<Integer>emptyList(), result);

        result = ProctorStoreCaching.selectHistorySet(testee, 0, Integer.MAX_VALUE);
        assertEquals(testee, result);

        result = ProctorStoreCaching.selectHistorySet(testee, 0, Integer.MIN_VALUE);
        assertEquals(Collections.<Integer>emptyList(), result);

        result = ProctorStoreCaching.selectHistorySet(testee, Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertEquals(testee, result);
    }
}