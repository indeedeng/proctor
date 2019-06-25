package com.indeed.proctor.webapp.tags;

import com.indeed.proctor.webapp.util.EncodingUtil;

public class UtilityFunctions {

    /**
     * url-encode in UTF-8
     */
    public static String urlEncode(final String input) {
        return EncodingUtil.urlEncodeUtf8(input);
    }
}
