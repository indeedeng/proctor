package com.indeed.proctor.service.useragents;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for web browser stuff.
 */
public class Browser {
    public enum BrowserType {
        MSIE, FIREFOX, SAFARI, CHROME, OPERA
    }

    private final BrowserType browserType;
    private final int majorVersion;
    private final int minorVersion;

    public Browser(BrowserType browserType, int majorVersion, int minorVersion) {
        this.browserType = browserType;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
    }

    public BrowserType getBrowserType() {
        return browserType;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    /**
     * @return null if user agent was unrecognized
     */
    @Nullable
    public static Browser parseUserAgent(@Nullable final String userAgent) {
        if (userAgent == null) return null;
        try {
            return innerParseUserAgent(userAgent);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Browser browser = (Browser)o;

        if (majorVersion != browser.majorVersion) {
            return false;
        }
        if (minorVersion != browser.minorVersion) {
            return false;
        }
        if (browserType != browser.browserType) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (browserType != null ? browserType.hashCode() : 0);
        result = 31 * result + majorVersion;
        result = 31 * result + minorVersion;
        return result;
    }

    private static Browser innerParseUserAgent(String userAgent) {
        final Browser[] ret = new Browser[1]; // used for passing a pointer by reference
        // ordered by probability of match
        if (tryPattern(userAgent, BrowserType.MSIE, MSIE_SIGNATURE, MSIE_PATTERN, ret)) return ret[0];
        if (tryPattern(userAgent, BrowserType.FIREFOX, FIREFOX_SIGNATURE, FIREFOX_PATTERN, ret)) return ret[0];
        if (tryPattern(userAgent, BrowserType.CHROME, CHROME_SIGNATURE, CHROME_PATTERN, ret)) return ret[0];    //  Chrome BEFORE Safari because Chrome matches Safari
        if (tryPattern(userAgent, BrowserType.SAFARI, SAFARI_SIGNATURE, SAFARI_NEW_PATTERN, ret)) return ret[0];
        if (tryPattern(userAgent, BrowserType.SAFARI, SAFARI_SIGNATURE, SAFARI_PATTERN, ret)) return ret[0];
        if (tryPattern(userAgent, BrowserType.OPERA, OPERA_SIGNATURE, OPERA_PATTERN, ret)) return ret[0];
        return null;
    }

    private static boolean tryPattern(String userAgent, BrowserType type, String signature, Pattern pattern, Browser[] ret) {
        if (!userAgent.contains(signature)) return false;
        final Matcher m = pattern.matcher(userAgent);
        if (m.find()) {
            final int majorVersion = Integer.parseInt(m.group(1));
            final int minorVersion = zeroIfNull(m.group(3));
            ret[0] = new Browser(type, majorVersion, minorVersion);
            return true;
        }
        return false;
    }

    private static int zeroIfNull(String s) {
        return s==null?0:Integer.parseInt(s);
    }

    public static boolean isFirefox(String ua) {
        return ua != null && ua.contains(FIREFOX_SIGNATURE);
    }

    public static boolean isMSIE(String ua) {
        return ua != null && ua.contains(MSIE_SIGNATURE);
    }

    // this overmatches to include chrome currently
    public static boolean isSafari(String ua) {
        return ua != null && ua.contains(SAFARI_SIGNATURE);
    }

    public static boolean isChrome(String ua) {
        return ua != null && ua.contains(CHROME_SIGNATURE);
    }

    public static boolean isIPhone(String ua) {
        return ua != null && (ua.contains(IPHONE_SIGNATURE) || ua.contains(IPOD_SIGNATURE));
    }

    public static boolean isIPad(String ua) {
        return ua != null && ua.contains(IPAD_SIGNATURE);
    }

    public static boolean isMobileSafari(String ua) {
        return ua != null && ua.contains(MOBILE_SAFARI_SIGNATURE);
    }

    public static boolean isAndroid(String ua) {
        return ua != null && ua.contains(ANDROID_SIGNATURE);
    }

    /**
     * Attempts to determine whether this user-agent is a phone or tablet. Please note that many
     *  early-version android devices incorrectly (per industry recommendations) include 'mobile'
     *  in their user-agent strings. The Galaxy Tab at least back to 7.7 does NOT, so this should
     *  be fine for a rough cut.
     *
     * @param ua
     * @return
     */
    public static boolean isAndroidTablet(String ua) {
        if (!isAndroid(ua)) {
            return false;
        }

        if (indexOfIgnoreCase(ua, 0, "mobile") >= 0) {
            return false;
        }

        return true;
    }

    public static boolean isWindowsPhone(String ua) {
        return ua != null && ua.contains(WINDOWS_PHONE_SIGNATURE);
    }

    public static String getAndroidVersion(String ua) {
        final Matcher m = ANDROID_PATTERN.matcher(ua);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    @Nullable
    public static String getIOSVersion(@Nullable final String ua) {
        if (ua == null) return null;
        StringBuilder versionBuilder = new StringBuilder(6);
        int index = ua.indexOf("OS ");
        if (index == -1) return null;
        index += 3; // skip over "OS ".
        if (!Character.isDigit(ua.charAt(index))) {
            return null;
        }
        while (index < ua.length() && ua.charAt(index) != ' ') {
            versionBuilder.append(ua.charAt(index));
            index++;
        }
        return versionBuilder.toString().replace('_', '.');
    }
    
    public static String getWindowsPhoneVersion(String ua) {
        if (ua == null) return null;
        StringBuilder versionBuilder = new StringBuilder(6);
        int index = ua.indexOf("OS ");
        if (index == -1) return null;
        index += 3; // skip over "OS ".
        if (!Character.isDigit(ua.charAt(index))) {
            return null;
        }
        while (index < ua.length() && ua.charAt(index) != ';') {
            versionBuilder.append(ua.charAt(index));
            index++;
        }
        return versionBuilder.toString();
    }

    public static boolean isSmartPhone(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
	    return isSmartPhone(ua);
    }

    /**
     * @param ua user agent string
     * @return true if user agent is a supported smart phone
     */
    public static boolean isSmartPhone(String ua) {
        return ua == null || isIPhone(ua) || isAndroid(ua) || isMobileSafari(ua) || isWindowsPhone(ua);
    }

    /**
     * @param ua
     * @return true if user agent is a supported mobile phone (smart phone or feature phone)
     *
     * TODO Exclude tablets, specifically android tablets
     */
    public static boolean isMobilePhone(final String ua) {
        if (ua != null) {
            if (isSmartPhone(ua)){
                return true;
            }

            for (final String uaSub : FEATURE_PHONE_SIGNATURES) {
                if (indexOfIgnoreCase(ua, 0, uaSub) > 0) {
                    return true;
                }
            }
        }

        return false;
    }
    public static boolean isMobilePhone(@Nonnull final HttpServletRequest request) {
        final String ua = request.getHeader("User-Agent");
	    return isMobilePhone(ua);
    }

    /**
     * @param ua
     * @return true if user agent is a supported mobile phone (smart phone or feature phone)
     */
    public static boolean isTablet(final String ua) {
        return ua != null && (isIPad(ua) || isAndroidTablet(ua));
    }
    public static boolean isTablet(@Nonnull final HttpServletRequest request) {
        final String ua = request.getHeader("User-Agent");
	    return isTablet(ua);
    }

    public static boolean isMobileDevice(final String ua) {
        return ua != null && isMobilePhone(ua) || isTablet(ua);
    }
    public static boolean isMobileDevice(@Nonnull final HttpServletRequest request) {
        final String ua = request.getHeader("User-Agent");
	    return isMobileDevice(ua);
    }

    // faster to search for substrings to avoid using a more heavyweight Matcher etc
    private static final String MSIE_SIGNATURE = "MSIE";
    private static final String FIREFOX_SIGNATURE = "Firefox/";
    private static final String SAFARI_SIGNATURE = "Safari/";
    private static final String CHROME_SIGNATURE = "Chrome/";   //  will capture chromium as well
    private static final String OPERA_SIGNATURE = "Opera/";
    private static final String IPHONE_SIGNATURE = "iPhone";
    private static final String IPOD_SIGNATURE = "iPod";
    private static final String MOBILE_SAFARI_SIGNATURE = "Mobile Safari";
    private static final String ANDROID_SIGNATURE = "Android";
    private static final String WINDOWS_PHONE_SIGNATURE = "Windows Phone";

    private static final String IPAD_SIGNATURE = "iPad";

    // Ported from JASX, PASSPORT, REX, minus the duplicated smartphone signatures above
    private static final String[] FEATURE_PHONE_SIGNATURES = new String[] {
            "blackberry",
            "blazer",
            "opera mini",
            "up.browser",
            "netfront",
            "symbianos",
            "polaris",
            // todo: Does this entirely duplicate 'windows phone' above?
            "iemobile",
            "teleca",
            "docomo",
            "googlebot-mobile",
            "brew",
            "kddi-",  // JASX-3854 All KDDI-* user agents
            "hiptop", // JASX-3699 Danger hiptop
    };

    private static final Pattern MSIE_PATTERN = Pattern.compile("MSIE (\\d+)(\\.(\\d+))?");
    private static final Pattern FIREFOX_PATTERN = Pattern.compile("Firefox/(\\d+)(\\.(\\d+))?");
    private static final Pattern SAFARI_PATTERN = Pattern.compile("Safari/(\\d+)(\\.(\\d+))?");
    private static final Pattern SAFARI_NEW_PATTERN = Pattern.compile("Version/(\\d+)(\\.(\\d+))?(\\.(\\d+))? Safari/");
    private static final Pattern CHROME_PATTERN = Pattern.compile("Chrome/(\\d+)(\\.(\\d+))?");
    private static final Pattern OPERA_PATTERN = Pattern.compile("Opera/(\\d+)(\\.(\\d+))?");
    private static final Pattern ANDROID_PATTERN = Pattern.compile("Android (\\d+(?:\\.\\d+)+)");

    /**
     * Test for whether browser is AJAX capable via User-Agent string.
     * @param request HttpServletRequest
     */
    public static boolean isAjaxCapable(HttpServletRequest request) {
    	return Browser.isAjaxCapable(request.getHeader("User-Agent"));
    }

    /**
     * Test for whether browser is AJAX capable via User-Agent string.
     * @param agent the user-agent
     */
    public static boolean isAjaxCapable(String agent) {
    	if (agent == null) return true;
    	// check for IE for Mac 5.17, 5.23 (http://en.wikipedia.org/wiki/User_agent)
    	if (agent.contains("MSIE") && agent.contains(" Mac")) return false;

		// Known problem version is 8.5.1, last version in Jaguar (10.2). All Jaguar
		// versions are "85.<something>".
		// Panther (10.3) starts at version 100, we're just guessing it's ok.
    	// see http://developer.apple.com/internet/safari/uamatrix.html
    	if (agent.contains(" Safari/85.")) return false;

    	return true;
    }

    /**
     * Returns true if css sprites are available for a given user-agent.
     */
    public static boolean isSpriteAvailable(String ua) {
        try {
            if (isMSIE(ua)) {
                Matcher m = MSIE_PATTERN.matcher(ua);
                if (m.find()) {
                    String h = m.group(1);
                    if (Integer.parseInt(h) >= 6) return true;
                }
                return false;
            } else if (isFirefox(ua)) {
                Matcher m = FIREFOX_PATTERN.matcher(ua);
                if (m.find()) {
                    String h = m.group(1);
                    // >= 2.0 is good
                    if (Integer.parseInt(h) >= 2) return true;
                    String l = m.group(3);
                    // >= 1.5 is good
                    if (Integer.parseInt(h) == 1 && Integer.parseInt(l) >= 5) return true;
                }
                return false;
            } else if (isSafari(ua)) {
                final Matcher newSafariMatcher = SAFARI_NEW_PATTERN.matcher(ua);
                if (newSafariMatcher.find()) {
                    return true;    //  all safaris that have Version/ match
               }
                Matcher m = SAFARI_PATTERN.matcher(ua);
                if (m.find()) {
                    String h = m.group(1);
                    if (Integer.parseInt(h) >= 419) return true;
                }
                return false;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Returns true iff the browser has sent an Accept-Encoding string containing "gzip".
     */
    public static boolean isAcceptsGzip(HttpServletRequest request) {
        String acceptEncoding = request.getHeader("Accept-Encoding");
        if (acceptEncoding == null) return false;
        return acceptEncoding.toLowerCase().contains("gzip");
    }

    public String toString() {
        return browserType + "/" + majorVersion + "." + minorVersion;
    };

    /**
     * Returns the first index of the given sub-string ("needle") in the given string after the given index, ignoring case; returns -1 if
     * sub-string not found after that index in string.
     *
     * @param str string to search
     * @param fromIndex index from which to search in str
     * @param needle sub-string to search for in str
     * @return first index of sub-string in str after fromIndex, -1 if not found
     * @see String#indexOf(String, int)
     */
    protected static int indexOfIgnoreCase(final String str, final int fromIndex, final String needle) {
        final int needleLength = needle.length();
        final int len = str.length();
        for (int i = fromIndex; i + needleLength <= len; i++) {
            if (str.regionMatches(true, i, needle, 0, needleLength)) {
                return i;
            }
        }
        return -1;
    }

}
