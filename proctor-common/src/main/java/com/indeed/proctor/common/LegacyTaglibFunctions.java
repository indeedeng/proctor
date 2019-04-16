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

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

// import javax.servlet.jsp.JspTagException;

// import org.apache.taglibs.standard.resources.Resources;
// import org.apache.taglibs.standard.tag.common.core.Util;

/**
 * <p>JSTL Functions</p>
 *
 *  copied from org.apache.taglibs.standard.Functions, keep the above license statement to remain legal
 */
public class LegacyTaglibFunctions {

    //*********************************************************************
    // String capitalization

    /**
     * Converts all of the characters of the input string to upper case.
     */
    public static String toUpperCase(String input) {
        return input.toUpperCase();
    }

    /**
     * Converts all of the characters of the input string to lower case.
     */
    public static String toLowerCase(String input) {
        return input.toLowerCase();
    }

    //*********************************************************************
    // Substring processing

    public static int indexOf(String input, String substring) {
        if (input == null) input = "";
        if (substring == null) substring = "";
        return input.indexOf(substring);
    }

    public static boolean contains(String input, String substring) {
        return indexOf(input, substring) != -1;
    }

    public static boolean containsIgnoreCase(String input, String substring) {
        if (input == null) input = "";
        if (substring == null) substring = "";
        String inputUC = input.toUpperCase();
        String substringUC = substring.toUpperCase();
        return indexOf(inputUC, substringUC) != -1;
    }

    public static boolean startsWith(String input, String substring) {
        if (input == null) input = "";
        if (substring == null) substring = "";
        return input.startsWith(substring);
    }

    public static boolean endsWith(String input, String substring) {
        if (input == null) input = "";
        if (substring == null) substring = "";
        return input.endsWith(substring);
    }

    public static String substring(String input, int beginIndex, int endIndex) {
        if (input == null) input = "";
        if (beginIndex >= input.length()) return "";
        if (beginIndex < 0) beginIndex = 0;
        if (endIndex < 0 || endIndex > input.length()) endIndex = input.length();
        if (endIndex < beginIndex) return "";
        return input.substring(beginIndex, endIndex);
    }

    public static String substringAfter(String input, String substring) {
        if (input == null) input = "";
        if (input.length() == 0) return "";
        if (substring == null) substring = "";
        if (substring.length() == 0) return input;

        int index = input.indexOf(substring);
        if (index == -1) {
            return "";
        } else {
            return input.substring(index+substring.length());
        }
    }

    public static String substringBefore(String input, String substring) {
        if (input == null) input = "";
        if (input.length() == 0) return "";
        if (substring == null) substring = "";
        if (substring.length() == 0) return "";

        int index = input.indexOf(substring);
        if (index == -1) {
            return "";
        } else {
            return input.substring(0, index);
        }
    }

    //*********************************************************************
    // Character replacement

//    public static String escapeXml(String input) {
//        if (input == null) return "";
//        return Util.escapeXml(input);
//    }

    public static String trim(String input) {
        if (input == null) return "";
        return input.trim();
    }

    public static String replace(
            String input,
            String substringBefore,
            String substringAfter)
    {
        if (input == null) input = "";
        if (input.length() == 0) return "";
        if (substringBefore == null) substringBefore = "";
        if (substringBefore.length() == 0) return input;

        StringBuffer buf = new StringBuffer(input.length());
        int startIndex = 0;
        int index;
        while ((index = input.indexOf(substringBefore, startIndex)) != -1) {
            buf.append(input.substring(startIndex, index)).append(substringAfter);
            startIndex = index + substringBefore.length();
        }
        return buf.append(input.substring(startIndex)).toString();
    }

    public static String[] split(
            String input,
            String delimiters)
    {
        String[] array;
        if (input == null) input = "";
        if (input.length() == 0) {
            array = new String[1];
            array[0] = "";
            return array;
        }

        if (delimiters == null) delimiters = "";

        StringTokenizer tok = new StringTokenizer(input, delimiters);
        int count = tok.countTokens();
        array = new String[count];
        int i = 0;
        while (tok.hasMoreTokens()) {
            array[i++] = tok.nextToken();
        }
        return array;
    }

    //*********************************************************************
    // Collections processing

    public static int length(Object obj) {
        if (obj == null) return 0;

        if (obj instanceof String) return ((String)obj).length();
        if (obj instanceof Collection) return ((Collection)obj).size();
        if (obj instanceof Map) return ((Map)obj).size();

        int count = 0;
        if (obj instanceof Iterator) {
            Iterator iter = (Iterator)obj;
            count = 0;
            while (iter.hasNext()) {
                count++;
                iter.next();
            }
            return count;
        }
        if (obj instanceof Enumeration) {
            Enumeration enum_ = (Enumeration)obj;
            count = 0;
            while (enum_.hasMoreElements()) {
                count++;
                enum_.nextElement();
            }
            return count;
        }
        try {
            count = Array.getLength(obj);
            return count;
        } catch (IllegalArgumentException ex) {}
        throw new IllegalStateException("Don't know how to iterate over supplied \"items\" in &lt;forEach&gt;");
    }

    public static String join(String[] array, String separator) {
        if (array == null) return "";
        if (separator == null) separator = "";

        StringBuffer buf = new StringBuffer();
        for (int i=0; i<array.length; i++) {
            buf.append(array[i]);
            if (i < array.length-1) buf.append(separator);
        }

        return buf.toString();
    }
}
