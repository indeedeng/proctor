package com.indeed.proctor.consumer;

import com.google.common.base.Strings;
import com.indeed.proctor.common.ForceGroupsOptions;
import com.indeed.proctor.common.ForceGroupsOptionsStrings;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.Proctor;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.TestType;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class ProctorConsumerUtils {
    /**
     * plain old "forceGroups" is already in use by JASX for SERP groups
     *
     * @param allowForcedGroups if true, parses force group parameters from request / cookie, sets new cookie
     */
    public static final String FORCE_GROUPS_PARAMETER = "prforceGroups";
    public static final String FORCE_GROUPS_COOKIE_NAME = "prforceGroups";
    public static final String FORCE_GROUPS_HEADER = "X-PRFORCEGROUPS";

    public static ProctorResult determineBuckets(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Proctor proctor,
            final String identifier,
            final TestType testType,
            final Map<String, Object> context,
            final boolean allowForcedGroups
    ) {
        final Identifiers identifiers = new Identifiers(testType, identifier);
        return determineBuckets(request, response, proctor, identifiers, context, allowForcedGroups);
    }

    /**
     * calculates ProctorResult (determined groups) and also handles forcedGroups, setting cookie if necessary
     *
     * @param allowForcedGroups if true, parses force group parameters from request / cookie, sets new cookie
     */
    public static ProctorResult determineBuckets(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Proctor proctor,
            final Identifiers identifiers,
            final Map<String, Object> context,
            final boolean allowForcedGroups
    ) {
        final ForceGroupsOptions forcedGroups;
        if (allowForcedGroups) {
            forcedGroups = parseForcedGroupsOptions(request);
            createForcedGroupsCookieUnlessEmpty(request.getContextPath(), forcedGroups)
                    .ifPresent(response::addCookie);
        } else {
            forcedGroups = ForceGroupsOptions.empty();
        }
        return proctor.determineTestGroups(identifiers, context, forcedGroups, Collections.emptyList());
    }

    /**
     * Consumer is required to do any privilege checks before getting here
     *
     * @param request a {@link HttpServletRequest} which may contain forced groups parameters from URL, Header or Cookie.
     * @return a map of test names to bucket values specified by the request.  Returns an empty {@link Map} if nothing was specified
     */
    @Nonnull
    public static ForceGroupsOptions parseForcedGroupsOptions(@Nonnull final HttpServletRequest request) {
        final String forceGroupsList = getForceGroupsStringFromRequest(request);
        return ForceGroupsOptionsStrings.parseForceGroupsString(forceGroupsList);
    }

    @Nonnull
    public static Map<String, Integer> parseForceGroupsList(@Nullable final String payload) {
        return ForceGroupsOptionsStrings.parseForceGroupsString(payload)
                .getForceGroups();
    }

    /**
     * @return proctor force groups if set in request, returns first found of: parameter, header, cookie
     */
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

    /**
     * Create a cookie that will be parsed by {@link #parseForcedGroupsOptions(HttpServletRequest)}.
     * Cookie expires at end of browser session
     *
     * @param contextPath        request.contextPath
     * @param forceGroupsOptions parsed force groups
     */
    public static Optional<Cookie> createForcedGroupsCookieUnlessEmpty(
            final String contextPath,
            final ForceGroupsOptions forceGroupsOptions
    ) {
        //  don't overwrite other cookie with empty; this would be relevant in a race condition where
        //  there is a forceGroups request simultaneous with a non-forceGroups request
        if (forceGroupsOptions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(doCreateForcedGroupsCookie(contextPath, forceGroupsOptions));
    }

    private static Cookie doCreateForcedGroupsCookie(
            final String contextPath,
            final ForceGroupsOptions forceGroupsOptions
    ) {
        //  be sure to quote cookies because they have characters that are not allowed raw
        final String cookieValue = '"' + ForceGroupsOptionsStrings.generateForceGroupsString(forceGroupsOptions) + '"';

        final String cookiePath;
        if (StringUtils.isBlank(contextPath)) {
            cookiePath = "/";
        } else {
            cookiePath = contextPath;
        }

        final Cookie cookie = new Cookie(FORCE_GROUPS_COOKIE_NAME, cookieValue);
        cookie.setPath(cookiePath);

        return cookie;
    }

    /**
     * @deprecated Use {@link #parseForcedGroupsOptions(HttpServletRequest)}
     */
    @Deprecated
    @Nonnull
    public static Map<String, Integer> parseForcedGroups(@Nonnull final HttpServletRequest request) {
        return parseForcedGroupsOptions(request).getForceGroups();
    }

    /**
     * Unless forceGroups is empty, set a cookie that will be parsed by {@link #parseForcedGroups(HttpServletRequest)}.
     * Cookie expires at end of browser session
     *
     * @param forceGroups parsed force groups
     * @deprecated use {@link ProctorConsumerUtils#createForcedGroupsCookieUnlessEmpty}, modify as needed, then add to the response
     */
    @Deprecated
    public static void setForcedGroupsCookie(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Map<String, Integer> forceGroups
    ) {
        createForcedGroupsCookieUnlessEmpty(request.getContextPath(), forceGroups)
                .ifPresent(response::addCookie);
    }

    /**
     * @deprecated Use {@link #createForcedGroupsCookieUnlessEmpty(String, ForceGroupsOptions)}
     */
    @Deprecated
    public static Optional<Cookie> createForcedGroupsCookieUnlessEmpty(
            final String contextPath,
            final Map<String, Integer> forceGroups
    ) {
        return createForcedGroupsCookieUnlessEmpty(
                contextPath,
                ForceGroupsOptions.builder().putAllForceGroups(forceGroups).build()
        );
    }

    /**
     * @deprecated use {@link ProctorConsumerUtils#createForcedGroupsCookieUnlessEmpty(String, ForceGroupsOptions}
     */
    @Deprecated // not safe, see comment in createForcedGroupsCookieUnlessEmpty
    public static Cookie createForcedGroupsCookie(final String contextPath, final Map<String, Integer> forceGroups) {
        return doCreateForcedGroupsCookie(contextPath, forceGroups);
    }

    // TODO: can be merged into createForcedGroupsCookieUnlessEmpty once createForcedGroupsCookie() was deleted

    /**
     * @deprecated Use {@link #doCreateForcedGroupsCookie(String, ForceGroupsOptions)}
     */
    @Deprecated
    private static Cookie doCreateForcedGroupsCookie(final String contextPath, final Map<String, Integer> forceGroups) {
        return doCreateForcedGroupsCookie(
                contextPath,
                ForceGroupsOptions.builder().putAllForceGroups(forceGroups).build()
        );
    }
}
