package com.indeed.proctor.common;

import com.indeed.util.core.ReleaseVersion;

import java.util.Collection;

/**
 * Library of functions to make available to EL rules
 *
 * @author ketan
 */
@SuppressWarnings("UnusedDeclaration")
public class ProctorRuleFunctions {
    public static boolean contains(final Collection c, final Object element) {
        if (isIntegerNumber(element)) {
            // This special treatment is required because the type of constant variable
            // determined implicitly in deserialization from JSON definition file.
            // Without this treatment, it may cause an unintended behavior
            // when collection comes from constant integer array (ex. [1, 2, 3]: List<Integer>)
            // and element comes from context variable defined as Long.
            // This doesn't handle float number since it's not usual to do strict equal comparison.
            final long elementValue = toLong(element);
            for (final Object x : c) {
                if (isIntegerNumber(x)) {
                    final long value = toLong(x);
                    if (elementValue == value) {
                        return true;
                    }
                }
            }
            return false;
        }

        return c.contains(element);
    }

    private static boolean isFloatNumber(final Object object) {
        return (object instanceof Float) || (object instanceof Double);
    }

    private static boolean isIntegerNumber(final Object object) {
        return (object instanceof Number) && !isFloatNumber(object);
    }

    /**
     * Casts to long. Argument variable must be instance of Number class
     *
     * @param number Number object
     * @return casted long value
     * @throws ClassCastException if given object is not instance of Number class
     */
    private static long toLong(final Object number) {
        return ((Number) number).longValue();
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

    public static <T extends Comparable<T>> boolean inRange(
            final T value, final T closedLowerBound, final T openUpperBound) {
        return value.compareTo(closedLowerBound) >= 0 && openUpperBound.compareTo(value) > 0;
    }

    public static boolean versionInRange(
            final ReleaseVersion version, final String startInclusive, final String endExclusive) {
        final ReleaseVersion start = ReleaseVersion.fromString(startInclusive);
        final ReleaseVersion end = ReleaseVersion.fromString(endExclusive);
        if (end.getMatchPrecision() != ReleaseVersion.MatchPrecision.BUILD) {
            throw new IllegalStateException(
                    "Cannot use wildcard as open upper bound of range: " + endExclusive);
        }
        return inRange(version, start, end);
    }

    public static MaybeBool maybeAnd(final MaybeBool op1, final MaybeBool op2) {
        if (MaybeBool.FALSE == op1 || MaybeBool.FALSE == op2) {
            return MaybeBool.FALSE;
        }
        if (MaybeBool.TRUE == op1 && MaybeBool.TRUE == op2) {
            return MaybeBool.TRUE;
        }
        return MaybeBool.UNKNOWN;
    }

    public static MaybeBool maybeOr(final MaybeBool op1, final MaybeBool op2) {
        if (MaybeBool.TRUE == op1 || MaybeBool.TRUE == op2) {
            return MaybeBool.TRUE;
        }
        if (MaybeBool.FALSE == op1 && MaybeBool.FALSE == op2) {
            return MaybeBool.FALSE;
        }
        return MaybeBool.UNKNOWN;
    }

    public static MaybeBool maybeNot(final MaybeBool maybeBool) {
        if (MaybeBool.TRUE == maybeBool) {
            return MaybeBool.FALSE;
        }
        if (MaybeBool.FALSE == maybeBool) {
            return MaybeBool.TRUE;
        }
        return MaybeBool.UNKNOWN;
    }

    public static MaybeBool toMaybeBool(final boolean b) {
        return b ? MaybeBool.TRUE : MaybeBool.FALSE;
    }

    public enum MaybeBool {
        TRUE,
        FALSE,
        UNKNOWN;
    }
}
