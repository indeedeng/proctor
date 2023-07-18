package com.indeed.proctor.webapp.util;

import com.google.common.base.Charsets;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/** */
public class EncodingUtil {
    public static String urlEncodeUtf8(String s) {
        try {
            return URLEncoder.encode(s, Charsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }
}
