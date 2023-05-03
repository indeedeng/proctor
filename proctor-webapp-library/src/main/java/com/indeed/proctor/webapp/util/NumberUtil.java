package com.indeed.proctor.webapp.util;

public class NumberUtil {

    private static final double TOLERANCE = 1E-6;

    private NumberUtil() {
    }

    public static boolean equalsWithinTolerance(final double x, final double y) {
        return Math.abs(x - y) <= TOLERANCE;
    }
}
