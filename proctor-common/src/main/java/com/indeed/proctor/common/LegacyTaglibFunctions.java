package com.indeed.proctor.common;
/*
 * Copyright (c) 1997-2018 Oracle and/or its affiliates. All rights reserved.
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * functions to be used within Proctor rules. Rules are null-safe and return non-null on any input
 *
 * <p>was:
 *
 * <p>JSTL Functions copied from org.apache.taglibs.standard.Functions, keep the above license
 * statement to remain legal
 */
public class LegacyTaglibFunctions {

    // *********************************************************************
    // String capitalization

    /** Converts all of the characters of the input string to upper case. */
    public static String toUpperCase(@Nullable final String input) {
        if (input == null) {
            return "";
        }
        return input.toUpperCase(Locale.ENGLISH);
    }

    /** Converts all of the characters of the input string to lower case. */
    public static String toLowerCase(@Nullable final String input) {
        if (input == null) {
            return "";
        }
        return input.toLowerCase(Locale.ENGLISH);
    }

    // *********************************************************************
    // Substring processing

    /**
     * Returns the index withing a string of the first occurrence of a specified substring.
     *
     * @return 0 if any argument is null, else index of first occurence, or -1
     */
    public static int indexOf(@Nullable String input, @Nullable String substring) {
        if (input == null) {
            input = "";
        }
        if (substring == null) {
            substring = "";
        }
        return input.indexOf(substring);
    }

    /**
     * Tests if an input string contains the specified substring.
     *
     * @return true if any argument is null, else true if substring contained in input
     */
    public static boolean contains(@Nullable final String input, @Nullable final String substring) {
        return indexOf(input, substring) != -1;
    }

    /**
     * Tests if an input string contains the specified substring in a case insensitive way.
     *
     * @return true if any argument is null, else true if substring contained in input
     */
    public static boolean containsIgnoreCase(@Nullable String input, @Nullable String substring) {
        if (input == null) {
            input = "";
        }
        if (substring == null) {
            substring = "";
        }
        final String inputUC = input.toUpperCase(Locale.ENGLISH);
        final String substringUC = substring.toUpperCase(Locale.ENGLISH);
        return indexOf(inputUC, substringUC) != -1;
    }

    /** @return true if any argument is null, else true if input starts with substring */
    public static boolean startsWith(@Nullable String input, @Nullable String substring) {
        if (input == null) {
            input = "";
        }
        if (substring == null) {
            substring = "";
        }
        return input.startsWith(substring);
    }

    /** @return true if any argument is null, else true if input ends with substring */
    public static boolean endsWith(@Nullable String input, @Nullable String substring) {
        if (input == null) {
            input = "";
        }
        if (substring == null) {
            substring = "";
        }
        return input.endsWith(substring);
    }

    /**
     * substring("foobarbaz", 3, 6)).isEqualTo("bar")
     *
     * @param beginIndex inclusive
     * @param endIndex exclusive, negative index means infinite
     * @return substring after normalizing arguments, empty string if input is null
     */
    public static String substring(@Nullable String input, int beginIndex, int endIndex) {
        if (input == null) {
            input = "";
        }
        if (beginIndex >= input.length()) {
            return "";
        }
        if (beginIndex < 0) {
            beginIndex = 0;
        }
        if (endIndex < 0 || endIndex > input.length()) {
            endIndex = input.length();
        }
        if (endIndex < beginIndex) {
            return "";
        }
        return input.substring(beginIndex, endIndex);
    }

    /**
     * @param input if null, empty string is returned
     * @param substring if null, input string is returned
     * @return a subset of a string before a specific substring.
     */
    public static String substringAfter(@Nullable String input, @Nullable String substring) {
        if (input == null) {
            input = "";
        }
        if (input.isEmpty()) {
            return "";
        }
        if (substring == null) {
            substring = "";
        }
        if (substring.isEmpty()) {
            return input;
        }

        final int index = input.indexOf(substring);
        if (index == -1) {
            return "";
        } else {
            return input.substring(index + substring.length());
        }
    }

    /**
     * @param input if null, empty string is returned
     * @param substring if null, input string is returned
     * @return arguments is null a subset of a string following a specific substring.
     */
    public static String substringBefore(@Nullable String input, @Nullable String substring) {
        if (input == null) {
            input = "";
        }
        if (input.isEmpty()) {
            return "";
        }
        if (substring == null) {
            substring = "";
        }
        if (substring.isEmpty()) {
            return "";
        }

        final int index = input.indexOf(substring);
        if (index == -1) {
            return "";
        } else {
            return input.substring(0, index);
        }
    }

    // *********************************************************************
    // Character replacement

    /**
     * Removes white spaces from both ends of a string.
     *
     * @return empty string if input is null, trimmed else
     */
    public static String trim(@Nullable final String input) {
        if (input == null) {
            return "";
        }
        return input.trim();
    }

    /**
     * @param substringBefore if empty, input string is returned unchanged
     * @return a string resulting from replacing in an input string all occurrences of a non-empty
     *     "before" string into an "after" substring.
     */
    public static String replace(
            @Nullable String input, @Nullable String substringBefore, final String substringAfter) {
        if (input == null) {
            input = "";
        }
        if (input.isEmpty()) {
            return "";
        }
        if (substringBefore == null) {
            substringBefore = "";
        }
        if (substringBefore.isEmpty()) {
            return input;
        }

        final StringBuilder buf = new StringBuilder(input.length());
        int startIndex = 0;
        int index;
        while ((index = input.indexOf(substringBefore, startIndex)) != -1) {
            buf.append(input.substring(startIndex, index)).append(substringAfter);
            startIndex = index + substringBefore.length();
        }
        return buf.append(input.substring(startIndex)).toString();
    }

    /**
     * Splits a string into an array of substrings.
     *
     * @param delimiters "" if null
     * @return [""] when input is null
     */
    public static String[] split(@Nullable String input, @Nullable String delimiters) {
        final String[] array;
        if (input == null || input.isEmpty()) {
            return new String[] {""};
        }

        if (delimiters == null) {
            return new String[] {input};
        }

        final StringTokenizer tok = new StringTokenizer(input, delimiters);
        final int count = tok.countTokens();
        array = new String[count];
        int i = 0;
        while (tok.hasMoreTokens()) {
            array[i] = tok.nextToken();
            i++;
        }
        return array;
    }

    // *********************************************************************
    // Collections processing

    /** @return the number of items in a collection, or the number of characters in a string. */
    public static int length(@Nullable final Object obj) {
        if (obj == null) {
            return 0;
        }

        if (obj instanceof String) {
            return ((String) obj).length();
        }
        if (obj instanceof Collection) {
            return ((Collection) obj).size();
        }
        if (obj instanceof Map) {
            return ((Map) obj).size();
        }

        int count = 0;
        if (obj instanceof Iterator) {
            final Iterator iter = (Iterator) obj;
            count = 0;
            while (iter.hasNext()) {
                count++;
                iter.next();
            }
            return count;
        }
        if (obj instanceof Enumeration) {
            final Enumeration inputEnum = (Enumeration) obj;
            count = 0;
            while (inputEnum.hasMoreElements()) {
                count++;
                inputEnum.nextElement();
            }
            return count;
        }
        try {
            count = Array.getLength(obj);
            return count;
        } catch (final IllegalArgumentException ex) {
            // ignore
        }
        throw new IllegalStateException(
                "Don't know how to iterate over supplied \"items\" in &lt;forEach&gt;");
    }

    /**
     * Joins all elements of an array into a string.
     *
     * @return empty string if input is null
     */
    public static String join(@Nullable final String[] array, @Nullable String separator) {
        if (array == null) {
            return "";
        }
        if (separator == null) {
            separator = "";
        }

        final StringBuilder buf = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            buf.append(array[i]);
            if (i < array.length - 1) {
                buf.append(separator);
            }
        }

        return buf.toString();
    }
}
