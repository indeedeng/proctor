package com.indeed.proctor.consumer;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.Proctor;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.TestType;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class ProctorConsumerUtils {
    private static final Logger LOGGER = Logger.getLogger(ProctorConsumerUtils.class);
    /**
     * plain old "forceGroups" is already in use by JASX for SERP groups
     */
    public static final String FORCE_GROUPS_PARAMETER = "prforceGroups";
    public static final String FORCE_GROUPS_COOKIE_NAME = "prforceGroups";
    public static final String FORCE_GROUPS_HEADER = "X-PRFORCEGROUPS";

    public static ProctorResult determineBuckets(final HttpServletRequest request, final HttpServletResponse response, final Proctor proctor,
                                                    final String identifier, final TestType testType, final Map<String, Object> context, final boolean allowForcedGroups) {
        final Identifiers identifiers = new Identifiers(testType, identifier);
        return determineBuckets(request, response, proctor, identifiers, context, allowForcedGroups);
    }

    public static ProctorResult determineBuckets(final HttpServletRequest request, final HttpServletResponse response, final Proctor proctor,
                                                    final Identifiers identifiers, final Map<String, Object> context, final boolean allowForcedGroups) {
        final Map<String, Integer> forcedGroups;
        if (allowForcedGroups) {
            forcedGroups = parseForcedGroups(request);
            setForcedGroupsCookie(request, response, forcedGroups);
        } else {
            forcedGroups = Collections.emptyMap();
        }
        final ProctorResult result = proctor.determineTestGroups(identifiers, context, forcedGroups);
        return result;
    }

    /**
     * Consumer is required to do any privilege checks before getting here
     *
     * @param request a {@link HttpServletRequest} which may contain forced groups parameters from URL, Header or Cookie.
     * @return a map of test names to bucket values specified by the request.  Returns an empty {@link Map} if nothing was specified
     */
    @Nonnull
    public static Map<String, Integer> parseForcedGroups(@Nonnull final HttpServletRequest request) {
        final String forceGroupsList = getForceGroupsStringFromRequest(request);
        return parseForceGroupsList(forceGroupsList);
    }

    @Nonnull
    public static String getForceGroupsStringFromRequest(@Nonnull final HttpServletRequest request) {

        final String param = request.getParameter(FORCE_GROUPS_PARAMETER);
        if (param != null) {
            return param;
        }

        final String header = request.getHeader(FORCE_GROUPS_HEADER);
        if (header != null) {
            return header;
        }

        final Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return "";
        }

        for (int i = 0; i < cookies.length; i++) {
            if (FORCE_GROUPS_COOKIE_NAME.equals(cookies[i].getName())) {
                final String cookieValue = cookies[i].getValue();
                return Strings.nullToEmpty(cookieValue);
            }
        }

        return "";
    }

    @Nonnull
    public static Map<String, Integer> parseForceGroupsList(@Nullable final String payload) {
        if (payload == null) {
            return Collections.emptyMap();
        }
        final String[] pieces = payload.split(",+");
        final Map<String, Integer> forcedGroups = Maps.newHashMap();
        for (int i = 0; i < pieces.length; i++) {
            final String piece = pieces[i].trim();
            if (piece.length() == 0) {
                continue;
            }
            int bucketValueStart = piece.length() - 1;
            for (; bucketValueStart >= 0; bucketValueStart--) {
                if (! Character.isDigit(piece.charAt(bucketValueStart))) {
                    break;
                }
            }
            //  if no name or no value was found, it's not a valid proctor test bucket name
            if ((bucketValueStart == piece.length() - 1) || (bucketValueStart < 1)) {
                continue;
            }
            //  minus sign can only be at the beginning of a run
            if (piece.charAt(bucketValueStart) != '-') {
                bucketValueStart++;
            }
            //  bucketValueStart should now be the index of the minus sign or the first digit in a run of digits going to the end of the word
            final String testName = piece.substring(0, bucketValueStart).trim();
            final String bucketValueStr = piece.substring(bucketValueStart, piece.length());
            try {
                final Integer bucketValue = Integer.valueOf(bucketValueStr);
                forcedGroups.put(testName, bucketValue);
            } catch (final NumberFormatException e) {
                LOGGER.error("Unable to parse bucket value " + bucketValueStr + " as integer", e);
            }
        }
        return forcedGroups;
    }

    /**
     * Set a cookie that will be parsed by {@link #parseForcedGroups(HttpServletRequest)}.  Cookie expires at end of browser session
     * @param request request
     * @param response response
     * @param forceGroups parsed force groups
     */
    public static void setForcedGroupsCookie(final HttpServletRequest request, final HttpServletResponse response, final Map<String, Integer> forceGroups) {
        //  don't overwrite with empty; this would be relevant in a race condition where there is a forceGroups request simultaneous with a non-forceGroups request
        if (forceGroups.isEmpty()) {
            return;
        }

        //  be sure to quote cookies because they have characters that are not allowed raw
        final StringBuilder sb = new StringBuilder(10*forceGroups.size());
        sb.append('"');
        for (final Iterator<Entry<String, Integer>> iterator = forceGroups.entrySet().iterator(); iterator.hasNext(); ) {
            final Entry<String, Integer> next = iterator.next();
            sb.append(next.getKey()).append(next.getValue());
            if (iterator.hasNext()) {
                sb.append(',');
            }
        }
        sb.append('"');

        final String contextPath = request.getContextPath();
        final String cookiePath;
        if (CharMatcher.WHITESPACE.matchesAllOf(Strings.nullToEmpty(contextPath))) {
            cookiePath = "/";
        } else {
            cookiePath = contextPath;
        }

        final Cookie cookie = new Cookie(FORCE_GROUPS_COOKIE_NAME, sb.toString());
        cookie.setPath(cookiePath);
        response.addCookie(cookie);
    }
}
