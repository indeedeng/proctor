package com.indeed.proctor.webapp.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestType;
import org.junit.Test;

import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestTestSearchUtil {
    private static final Allocation ALLOCATION_100 = new Allocation(null, ImmutableList.of(
            new Range(1, 1),
            new Range(-1, 0),
            new Range(0, 0)));
    private static final Allocation ALLOCATION_0 = new Allocation(null, ImmutableList.of(
            new Range(1, 0),
            new Range(-1, 1),
            new Range(0, 0)));
    private static final Allocation ALLOCATION_50_50 = new Allocation(null, ImmutableList.of(
            new Range(1, 0.5),
            new Range(-1, 0),
            new Range(0, 0.5)));
    private static final Allocation ALLOCATION_50_50_WITH_RULE = ALLOCATION_50_50;

    static {
        ALLOCATION_50_50_WITH_RULE.setRule("country == 'US'");
    }

    private static final Allocation ALLOCATION_TEN_10S = new Allocation(null, IntStream.range(-1, 9)
            .mapToObj(i -> new Range(i, 0.1))
            .collect(Collectors.toList()));

    private static final TestBucket INACTIVE_BUCKET = new TestBucket("inactive", -1, null);
    private static final TestBucket CONTROL_BUCKET = new TestBucket("control", 0, null);
    private static final TestBucket CONTROL_BUCKET_WITH_DESCRIPTION = new TestBucket("control", 0, "This is CONTROL");
    private static final TestBucket ACTIVE_BUCKET = new TestBucket("active", 1, null);
    private static final TestBucket ACTIVE_BUCKET_WITH_DESCRIPTION = new TestBucket("active", 1, "This is the test " +
            "treatment 1");

    private static final TestDefinition DEFAULT_DEFINITION = new TestDefinition(
            "",
            null,
            TestType.ANONYMOUS_USER,
            "&salt",
            ImmutableList.of(INACTIVE_BUCKET, CONTROL_BUCKET, ACTIVE_BUCKET),
            ImmutableList.of(ALLOCATION_50_50),
            false,
            Collections.emptyMap(),
            Collections.emptyMap(),
            null);
    private static final TestDefinition DEFINITION_WITH_DESCRIPTION = new TestDefinition(DEFAULT_DEFINITION);
    private static final TestDefinition DEFINITION_WITH_RULE = new TestDefinition(DEFAULT_DEFINITION);
    private static final TestDefinition DEFINITION_WITH_BUCKET_RULE = new TestDefinition(DEFAULT_DEFINITION);
    private static final TestDefinition DEFINITION_WITH_BUCKET_DESCRIPTION = new TestDefinition(DEFAULT_DEFINITION);

    static {
        DEFINITION_WITH_DESCRIPTION.setDescription("Great a/b test!");
        DEFINITION_WITH_RULE.setRule("country == 'MX'");
        DEFINITION_WITH_BUCKET_RULE.setAllocations(ImmutableList.of(ALLOCATION_50_50_WITH_RULE, ALLOCATION_0));
        DEFINITION_WITH_BUCKET_DESCRIPTION.setBuckets(ImmutableList.of(INACTIVE_BUCKET,
                CONTROL_BUCKET_WITH_DESCRIPTION, ACTIVE_BUCKET_WITH_DESCRIPTION));
    }

    @Test
    public void testMatchTestName() {
        assertTrue(TestSearchUtil.matchTestName("GREAT_test_1", "great"));
        assertFalse(TestSearchUtil.matchTestName("GREAT_test_1", "great test"));
        assertTrue(TestSearchUtil.matchTestName("GREAT_test_1", "t_1"));
        assertTrue(TestSearchUtil.matchTestName("GREAT_test_1", ""));
    }

    @Test
    public void testMatchDescription() {
        assertFalse(TestSearchUtil.matchDescription(DEFAULT_DEFINITION, "great"));
        assertTrue(TestSearchUtil.matchDescription(DEFAULT_DEFINITION, ""));
        assertFalse(TestSearchUtil.matchDescription(DEFINITION_WITH_DESCRIPTION, "great test"));
        assertTrue(TestSearchUtil.matchDescription(DEFINITION_WITH_DESCRIPTION, "great"));
        assertTrue(TestSearchUtil.matchDescription(DEFINITION_WITH_DESCRIPTION, "t a"));
    }

    @Test
    public void testMatchRule() {
        assertFalse(TestSearchUtil.matchRule(DEFAULT_DEFINITION, "rule"));
        assertTrue(TestSearchUtil.matchRule(DEFAULT_DEFINITION, ""));
        assertFalse(TestSearchUtil.matchRule(DEFINITION_WITH_RULE, "== mx"));
        assertTrue(TestSearchUtil.matchRule(DEFINITION_WITH_RULE, "'mx'"));
        assertFalse(TestSearchUtil.matchRule(DEFINITION_WITH_BUCKET_RULE, "'mx'"));
        assertTrue(TestSearchUtil.matchRule(DEFINITION_WITH_BUCKET_RULE, "'us'"));
    }

    @Test
    public void testMatchBucket() {
        assertTrue(TestSearchUtil.matchBucket(DEFAULT_DEFINITION, ""));
        assertTrue(TestSearchUtil.matchBucket(DEFAULT_DEFINITION, "inactive"));
        assertFalse(TestSearchUtil.matchBucket(DEFAULT_DEFINITION, "grp2"));
    }

    @Test
    public void testMatchBucketDescription() {
        assertTrue(TestSearchUtil.matchBucketDescription(DEFAULT_DEFINITION, ""));
        assertFalse(TestSearchUtil.matchBucketDescription(DEFAULT_DEFINITION, "this is"));
        assertTrue(TestSearchUtil.matchBucketDescription(DEFINITION_WITH_BUCKET_DESCRIPTION, "this is the test " +
                "treatment 1"));
        assertFalse(TestSearchUtil.matchBucketDescription(DEFINITION_WITH_BUCKET_DESCRIPTION, "treatment 2"));
    }

    @Test
    public void testMatchTestType() {
        assertTrue(TestSearchUtil.matchTestType(DEFAULT_DEFINITION, ""));
        assertTrue(TestSearchUtil.matchTestType(DEFAULT_DEFINITION, "user"));
        assertFalse(TestSearchUtil.matchTestType(DEFAULT_DEFINITION, "random"));
    }

    @Test
    public void testMatchSalt() {
        assertTrue(TestSearchUtil.matchSalt(DEFAULT_DEFINITION, ""));
        assertTrue(TestSearchUtil.matchSalt(DEFAULT_DEFINITION, "salt"));
        assertFalse(TestSearchUtil.matchSalt(DEFAULT_DEFINITION, "sugar"));
    }

    @Test
    public void testMatchActiveAllocation() {
        assertFalse(TestSearchUtil.matchActiveAllocation(ImmutableList.of()));
        assertFalse(TestSearchUtil.matchActiveAllocation(ImmutableList.of(ALLOCATION_0)));
        assertFalse(TestSearchUtil.matchActiveAllocation(ImmutableList.of(ALLOCATION_100)));
        assertFalse(TestSearchUtil.matchActiveAllocation(ImmutableList.of(ALLOCATION_0, ALLOCATION_100)));
        assertTrue(TestSearchUtil.matchActiveAllocation(ImmutableList.of(ALLOCATION_50_50)));
        assertTrue(TestSearchUtil.matchActiveAllocation(ImmutableList.of(ALLOCATION_TEN_10S)));
        assertTrue(TestSearchUtil.matchActiveAllocation(ImmutableList.of(ALLOCATION_0, ALLOCATION_50_50)));
        assertTrue(TestSearchUtil.matchActiveAllocation(ImmutableList.of(ALLOCATION_0, ALLOCATION_50_50,
                ALLOCATION_100)));
    }

    @Test
    public void testGivenSetFirstComparator() {
        final ImmutableSet<String> favorites = ImmutableSet.of("WEIRD_test", "example_test", "nonexistent_test",
                "GOOD_TEST" /*case sensitive check*/);
        final Comparator<String> comparator = TestSearchUtil.givenSetFirstComparator(
                favorites);

        String[] sorted = Stream.of("WEIRD_test", "good_test", "example_test", "super_test", "A_test")
                .sorted(comparator).toArray(String[]::new);
        assertTrue(favorites.contains(sorted[0]));
        assertTrue(favorites.contains(sorted[1]));
        assertFalse(favorites.contains(sorted[2]));
        assertFalse(favorites.contains(sorted[3]));
        assertFalse(favorites.contains(sorted[4]));

        sorted = Stream.of("WEIRD_test", "good_test", "example_test", "super_test", "A_test")
                .sorted(comparator.thenComparing(String::compareToIgnoreCase)).toArray(String[]::new);
        assertArrayEquals(new String[]{"example_test", "WEIRD_test", "A_test", "good_test", "super_test"}, sorted);
    }
}
