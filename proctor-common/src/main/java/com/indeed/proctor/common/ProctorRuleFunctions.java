package com.indeed.proctor.common;

import java.util.Collection;

import com.indeed.util.core.ReleaseVersion;

/**
 * Library of functions to make available to EL rules
 * @author ketan
 *
 */
@SuppressWarnings("UnusedDeclaration")
public class ProctorRuleFunctions {
    public static boolean contains(final Collection c, final Object element) {
        return c.contains(element);
    }

    public static boolean matches(final String value, final String regex) {
        return value.matches(regex);
    }

    public static long now() {
        return System.currentTimeMillis();
    }

    public static ReleaseVersion version(final String versionString) {
        return ReleaseVersion.fromString(versionString);
    }

    public static <T extends Comparable<T>> boolean inRange(final T value, final T closedLowerBound, final T openUpperBound) {
        return value.compareTo(closedLowerBound) >= 0 && openUpperBound.compareTo(value) > 0;
    }

    public static boolean versionInRange(final ReleaseVersion version, final String startInclusive, final String endExclusive) {
        final ReleaseVersion start = ReleaseVersion.fromString(startInclusive);
        final ReleaseVersion end = ReleaseVersion.fromString(endExclusive);
        if (end.getMatchPrecision() != ReleaseVersion.MatchPrecision.BUILD) {
            throw new IllegalStateException("Cannot use wildcard as open upper bound of range: " + endExclusive);
        }
        return inRange(version, start, end);
    }
}
