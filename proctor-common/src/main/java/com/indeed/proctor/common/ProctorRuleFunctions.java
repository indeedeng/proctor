package com.indeed.proctor.common;

import java.util.Collection;

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
}
