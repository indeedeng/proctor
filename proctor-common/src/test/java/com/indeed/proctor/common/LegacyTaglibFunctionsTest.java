package com.indeed.proctor.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.util.Arrays;

import static com.indeed.proctor.common.LegacyTaglibFunctions.contains;
import static com.indeed.proctor.common.LegacyTaglibFunctions.containsIgnoreCase;
import static com.indeed.proctor.common.LegacyTaglibFunctions.endsWith;
import static com.indeed.proctor.common.LegacyTaglibFunctions.indexOf;
import static com.indeed.proctor.common.LegacyTaglibFunctions.join;
import static com.indeed.proctor.common.LegacyTaglibFunctions.length;
import static com.indeed.proctor.common.LegacyTaglibFunctions.replace;
import static com.indeed.proctor.common.LegacyTaglibFunctions.split;
import static com.indeed.proctor.common.LegacyTaglibFunctions.startsWith;
import static com.indeed.proctor.common.LegacyTaglibFunctions.substring;
import static com.indeed.proctor.common.LegacyTaglibFunctions.substringAfter;
import static com.indeed.proctor.common.LegacyTaglibFunctions.substringBefore;
import static com.indeed.proctor.common.LegacyTaglibFunctions.toLowerCase;
import static com.indeed.proctor.common.LegacyTaglibFunctions.toUpperCase;
import static com.indeed.proctor.common.LegacyTaglibFunctions.trim;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

public class LegacyTaglibFunctionsTest {

    @Test
    public void testToUpperCase() {
        assertThat(toUpperCase(null)).isEqualTo("");
        assertThat(toUpperCase("Foo")).isEqualTo("FOO");
    }

    @Test
    public void testToLowerCase() {
        assertThat(toLowerCase(null)).isEqualTo("");
        assertThat(toLowerCase("Foo")).isEqualTo("foo");
    }

    @Test
    public void testIndexOf() {
        assertThat(indexOf(null, null)).isEqualTo(0);
        assertThat(indexOf("", null)).isEqualTo(0);
        assertThat(indexOf("foo", null)).isEqualTo(0);
        assertThat(indexOf(null, "")).isEqualTo(0);

        assertThat(indexOf("o", "o")).isEqualTo(0);
        assertThat(indexOf("o", "")).isEqualTo(0);
        assertThat(indexOf("", "o")).isEqualTo(-1);

        assertThat(indexOf("foooo", "o")).isEqualTo(1);
        assertThat(indexOf("the foooo", "foo")).isEqualTo(4);
    }

    @Test
    public void testContains() {
        assertThat(contains(null, null)).isTrue();
        assertThat(contains("", null)).isTrue();
        assertThat(contains("foo", null)).isTrue();
        assertThat(contains(null, "")).isTrue();
        assertThat(contains("foo", "")).isTrue();
        assertThat(contains("", "o")).isFalse();
        assertThat(contains("foo", "o")).isTrue();
        assertThat(contains("FOO", "o")).isFalse();
        assertThat(contains("bar", "o")).isFalse();

        assertThat(containsIgnoreCase(null, null)).isTrue();
        assertThat(containsIgnoreCase("", null)).isTrue();
        assertThat(containsIgnoreCase("foo", null)).isTrue();
        assertThat(containsIgnoreCase(null, "")).isTrue();
        assertThat(containsIgnoreCase("foo", "")).isTrue();
        assertThat(containsIgnoreCase("", "o")).isFalse();
        assertThat(containsIgnoreCase("foo", "o")).isTrue();
        assertThat(containsIgnoreCase("FOO", "o")).isTrue();
        assertThat(containsIgnoreCase("foo", "F")).isTrue();
        assertThat(containsIgnoreCase("bar", "o")).isFalse();

    }

    @Test
    public void testStartsWith() {
        assertThat(startsWith(null, null)).isTrue();
        assertThat(startsWith("", null)).isTrue();
        assertThat(startsWith("foo", null)).isTrue();
        assertThat(startsWith(null, "")).isTrue();
        assertThat(startsWith("foobar", "")).isTrue();
        assertThat(startsWith("", "foo")).isFalse();
        assertThat(startsWith("foobar", "foo")).isTrue();
        assertThat(startsWith("foobar", "bar")).isFalse();
    }

    @Test
    public void testEndsWith() {
        assertThat(endsWith(null, null)).isTrue();
        assertThat(endsWith("", null)).isTrue();
        assertThat(endsWith("foo", null)).isTrue();
        assertThat(endsWith(null, "")).isTrue();
        assertThat(endsWith("foobar", "")).isTrue();
        assertThat(endsWith("", "foo")).isFalse();
        assertThat(endsWith("foobar", "foo")).isFalse();
        assertThat(endsWith("foobar", "bar")).isTrue();
    }

    @Test
    public void testSubstring() {
        assertThat(substring(null, -1, 100)).isEqualTo("");
        assertThat(substring("", -1, 100)).isEqualTo("");
        assertThat(substring("foo", -1, 100)).isEqualTo("foo");
        assertThat(substring("foo", 100, 4)).isEqualTo("");
        assertThat(substring("foo", 0, -1)).isEqualTo("foo");
        assertThat(substring("foobarbaz", 4, 2)).isEqualTo("");
        assertThat(substring("foobarbaz", 3, 6)).isEqualTo("bar");
    }

    @Test
    public void testSubstringBeforeAfter() {
        assertThat(substringBefore(null, null)).isEqualTo("");
        assertThat(substringBefore("", null)).isEqualTo("");
        assertThat(substringBefore("foo", null)).isEqualTo("");
        assertThat(substringBefore(null, "")).isEqualTo("");
        assertThat(substringBefore("foo", "")).isEqualTo("");
        assertThat(substringBefore("", "o")).isEqualTo("");
        assertThat(substringBefore("foo", "o")).isEqualTo("f");
        assertThat(substringBefore("FOO", "o")).isEqualTo("");
        assertThat(substringBefore("bar", "o")).isEqualTo("");

        assertThat(substringAfter(null, null)).isEqualTo("");
        assertThat(substringAfter("", null)).isEqualTo("");
        assertThat(substringAfter("foo", null)).isEqualTo("foo");
        assertThat(substringAfter(null, "")).isEqualTo("");
        assertThat(substringAfter("foo", "")).isEqualTo("foo");
        assertThat(substringAfter("", "o")).isEqualTo("");
        assertThat(substringAfter("foo", "o")).isEqualTo("o");
        assertThat(substringAfter("FOO", "o")).isEqualTo("");
        assertThat(substringAfter("bar", "o")).isEqualTo("");
    }

    @Test
    public void testTrim() {
        assertThat(trim(null)).isEqualTo("");
        assertThat(trim("")).isEqualTo("");
        assertThat(trim("foo")).isEqualTo("foo");
        assertThat(trim(" foo ")).isEqualTo("foo");
        assertThat(trim("\tfoo\t")).isEqualTo("foo");
        assertThat(trim("\nfoo\n")).isEqualTo("foo");
        assertThat(trim("\n\t foo \t\n")).isEqualTo("foo");
        assertThat(trim(" foo bar ")).isEqualTo("foo bar");
    }

    @Test
    public void testReplace() {
        assertThat(replace(null, null, null)).isEqualTo("");
        assertThat(replace("", null, null)).isEqualTo("");
        assertThat(replace("foo", null, null)).isEqualTo("foo");
        assertThat(replace(null, "", null)).isEqualTo("");
        assertThat(replace(null, null, "")).isEqualTo("");
        assertThat(replace("", null, "")).isEqualTo("");
        assertThat(replace("foo", null, "")).isEqualTo("foo");
        assertThat(replace(null, "", "")).isEqualTo("");
        assertThat(replace("", "", "")).isEqualTo("");
        assertThat(replace("foo", "", "")).isEqualTo("foo");
        assertThat(replace("", "foo", "")).isEqualTo("");
        assertThat(replace("", "", "foo")).isEqualTo("");
        assertThat(replace("bar", "", "foo")).isEqualTo("bar");
        assertThat(replace("foo", "foo", "")).isEqualTo("");
        assertThat(replace("foobarfoo", "foo", "bum")).isEqualTo("bumbarbum");
    }

    @Test
    public void testSplit() {
        assertThat(split(null, null)).containsExactly("");
        assertThat(split("", null)).containsExactly("");
        assertThat(split("foo", null)).containsExactly("foo");
        assertThat(split(null, "")).containsExactly("");
        assertThat(split("foo", "")).containsExactly("foo");
        assertThat(split("", "")).containsExactly("");
        assertThat(split("", "x")).containsExactly("");

        assertThat(split("ooo", "o")).isEmpty();
        assertThat(split("foo, bar", ",")).containsExactly("foo", " bar");
        assertThat(split(",foo,", ",")).containsExactly("foo");
    }

    @Test
    public void testLength() {
        assertThat(length(null)).isEqualTo(0);
        assertThat(length("")).isEqualTo(0);
        assertThat(length("foo")).isEqualTo(3);

        assertThat(length(emptyList())).isEqualTo(0);
        assertThat(length(ImmutableList.of(1, 2, 3))).isEqualTo(3);

        assertThat(length(emptySet())).isEqualTo(0);
        assertThat(length(ImmutableSet.of(1, 2, 3))).isEqualTo(3);

        assertThat(length(emptyMap())).isEqualTo(0);
        assertThat(length(ImmutableMap.of(1, 2))).isEqualTo(1);

        assertThat(length(new String[0])).isEqualTo(0);
        assertThat(length(new String[3])).isEqualTo(3);
    }

    @Test
    public void testJoin() {
        assertThat(join(null, null)).isEqualTo("");
        assertThat(join(new String[0], null)).isEqualTo("");
        assertThat(join(new String[]{""}, null)).isEqualTo("");
        assertThat(join(new String[]{"foo"}, null)).isEqualTo("foo");
        assertThat(join(new String[]{"foo", "bar"}, null)).isEqualTo("foobar");
        assertThat(join(new String[]{"foo", "bar"}, "")).isEqualTo("foobar");
        assertThat(join(new String[]{"foo", "bar"}, "x")).isEqualTo("fooxbar");

    }
}