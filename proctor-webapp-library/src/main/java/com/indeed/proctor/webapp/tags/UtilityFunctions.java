package com.indeed.proctor.webapp.tags;

import com.google.common.base.Charsets;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 */
public class UtilityFunctions {
    public static String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, Charsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }
}
