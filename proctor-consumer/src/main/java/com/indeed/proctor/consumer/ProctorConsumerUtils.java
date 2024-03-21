package com.indeed.proctor.consumer;

import com.google.common.annotations.VisibleForTesting;
import com.indeed.proctor.common.ForceGroupsOptions;
import com.indeed.proctor.common.ForceGroupsOptionsStrings;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.Proctor;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.TestType;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ProctorConsumerUtils {
    /**
     * plain old "forceGroups" is already in use by JASX for SERP groups
     *
     * @param allowForcedGroups if true, parses force group parameters from request / cookie, sets
     *     new cookie
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
            final boolean allowForcedGroups,
            final Set<String> forcePayloadTests) {
        final Identifiers identifiers = new Identifiers(testType, identifier);
        return determineBuckets(
                request,
                response,
                proctor,
                identifiers,
                context,
                allowForcedGroups,
                forcePayloadTests);
    }

    /**
     * calculates ProctorResult (determined groups) and also handles forcedGroups, setting cookie if
     * necessary
     *
     * @param allowForcedGroups if true, parses force group parameters from request / cookie, sets
     *     new cookie
     */
    public static ProctorResult determineBuckets(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Proctor proctor,
            final Identifiers identifiers,
            final Map<String, Object> context,
            final boolean allowForcedGroups,
            final Set<String> forcePayloadTests) {
        final ForceGroupsOptions forceGroupsOptions;
        if (allowForcedGroups) {
            forceGroupsOptions = parseForcedGroupsOptions(request, forcePayloadTests);
            createForcedGroupsCookieUnlessEmpty(request.getContextPath(), forceGroupsOptions)
                    .ifPresent(response::addCookie);
        } else {
            forceGroupsOptions = ForceGroupsOptions.empty();
        }
        return proctor.determineTestGroups(
                identifiers, context, forceGroupsOptions, Collections.emptyList());
    }

    /**
     * Consumer is required to do any privilege checks before getting here
     *
     * @param request a {@link HttpServletRequest} which may contain forced groups parameters from
     *     URL, Header or Cookie.
     * @return a map of test names to bucket values specified by the request. Returns an empty
     *     {@link Map} if nothing was specified
     */
    @Nonnull
    public static ForceGroupsOptions parseForcedGroupsOptions(
            @Nonnull final HttpServletRequest request) {
        final String forceGroupsList = getForceGroupsStringFromRequest(request);
        return ForceGroupsOptionsStrings.parseForceGroupsString(forceGroupsList, new HashSet<>());
    }

    /**
     * Consumer is required to do any privilege checks before getting here
     *
     * @param request a {@link HttpServletRequest} which may contain forced groups parameters from
     *     URL, Header or Cookie.
     * @param forcePayloadTests set of Test names that allow using force payloads
     * @return a map of test names to bucket values specified by the request. Returns an empty
     *     {@link Map} if nothing was specified
     */
    @Nonnull
    public static ForceGroupsOptions parseForcedGroupsOptions(
            @Nonnull final HttpServletRequest request, final Set<String> forcePayloadTests) {
        final String forceGroupsList = getForceGroupsStringFromRequest(request);
        return ForceGroupsOptionsStrings.parseForceGroupsString(forceGroupsList, forcePayloadTests);
    }

    @Nonnull
    public static Map<String, Integer> parseForceGroupsList(@Nullable final String payload) {
        return ForceGroupsOptionsStrings.parseForceGroupsString(payload, Collections.emptySet())
                .getForceGroups();
    }

    /**
     * Parse the force groups from the request. Returns the first found from the URL parameter,
     * header or cookies.
     *
     * <p>Cookies require a special note here. It is possible to have more than one force groups
     * cookie assign to the same request. If this happens, it is likely an accident from multiple
     * different scenarios overlapping. Our goal is to try and do whatever gets the user as close as
     * possible to their end result. This means attempting to give them as many of the right group
     * allocations as possible. We can easily take the union of allocations for different tests, but
     * we need to make a decision about which group to force when multiple cookies specify an group
     * assignment for the same test. Unfortunately, the only thing the browser tells us is the name
     * and value of the cookie. We don't know the age, domain or path of the cookie, all of which
     * would let us make a better decision. The relevant specification is <a
     * href="https://datatracker.ietf.org/doc/html/rfc6265#section-5.4">RFC 6265, Section 5.4</a>.
     * According to that, User Agents SHOULD sort the list first by path, longest-to-shortest, and
     * then by age, oldest-to-newest. What we probably want is longest-to-shortest path and then
     * newest-to-oldest age cookie. However, the paths are likely to all be "/"; it's the
     * <i>domain</i> that is likely to differ among the cookies. And we don't have access to that.
     * So when we are processing cookies, we union all the cookies with the right name in the order
     * provided by the browser. This means the group assignments in the last cookie will be at the
     * end of the force groups string, which will cause them to win.
     *
     * @return proctor force groups if set in request, returns first found of: parameter, header,
     *     cookie
     */
    @Nonnull
    public static String getForceGroupsStringFromRequest(
            @Nonnull final HttpServletRequest request) {

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

        return Arrays.stream(cookies)
                .filter(Objects::nonNull)
                .filter(x -> FORCE_GROUPS_COOKIE_NAME.equals(x.getName()))
                .map(Cookie::getValue)
                .filter(Objects::nonNull)
                .map(
                        cookie -> {
                            try {
                                return URLDecoder.decode(cookie, StandardCharsets.UTF_8.toString());
                            } catch (final UnsupportedEncodingException e) {
                                throw new RuntimeException(e);
                            }
                        })
                .collect(Collectors.joining(","));
    }

    /**
     * It's not enough to just combine all the force groups from all the available cookies. If
     * different force group cookies exist with different allocations for the same test, we could
     * have different results. We need to process the cookies in a stable order so that we always
     * produce the same result.
     */
    @VisibleForTesting
    private static final Comparator<Cookie> COOKIE_COMPARATOR =
            Comparator.comparing(Cookie::getPath)
                    .thenComparing(Cookie::isHttpOnly)
                    .thenComparing(Cookie::getValue);

    /**
     * Create a cookie that will be parsed by {@link #parseForcedGroupsOptions(HttpServletRequest)}.
     * Cookie expires at end of browser session
     *
     * @param contextPath request.contextPath
     * @param forceGroupsOptions parsed force groups
     */
    public static Optional<Cookie> createForcedGroupsCookieUnlessEmpty(
            final String contextPath, final ForceGroupsOptions forceGroupsOptions) {
        //  don't overwrite other cookie with empty; this would be relevant in a race condition
        // where
        //  there is a forceGroups request simultaneous with a non-forceGroups request
        if (forceGroupsOptions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(doCreateForcedGroupsCookie(contextPath, forceGroupsOptions));
    }

    private static Cookie doCreateForcedGroupsCookie(
            final String contextPath, final ForceGroupsOptions forceGroupsOptions) {
        //  be sure to quote cookies because they have characters that are not allowed raw
        final String cookieValue =
                ForceGroupsOptionsStrings.generateForceGroupsStringForCookies(forceGroupsOptions);

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

    /** @deprecated Use {@link #parseForcedGroupsOptions(HttpServletRequest)} */
    @Deprecated
    @Nonnull
    public static Map<String, Integer> parseForcedGroups(
            @Nonnull final HttpServletRequest request) {
        return parseForcedGroupsOptions(request, new HashSet<>()).getForceGroups();
    }

    /**
     * Unless forceGroups is empty, set a cookie that will be parsed by {@link
     * #parseForcedGroups(HttpServletRequest)}. Cookie expires at end of browser session
     *
     * @param forceGroups parsed force groups
     * @deprecated use {@link ProctorConsumerUtils#createForcedGroupsCookieUnlessEmpty}, modify as
     *     needed, then add to the response
     */
    @Deprecated
    public static void setForcedGroupsCookie(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Map<String, Integer> forceGroups) {
        createForcedGroupsCookieUnlessEmpty(request.getContextPath(), forceGroups)
                .ifPresent(response::addCookie);
    }

    /** @deprecated Use {@link #createForcedGroupsCookieUnlessEmpty(String, ForceGroupsOptions)} */
    @Deprecated
    public static Optional<Cookie> createForcedGroupsCookieUnlessEmpty(
            final String contextPath, final Map<String, Integer> forceGroups) {
        return createForcedGroupsCookieUnlessEmpty(
                contextPath, ForceGroupsOptions.builder().putAllForceGroups(forceGroups).build());
    }

    /**
     * @deprecated use {@link ProctorConsumerUtils#createForcedGroupsCookieUnlessEmpty(String,
     *     ForceGroupsOptions}
     */
    @Deprecated // not safe, see comment in createForcedGroupsCookieUnlessEmpty
    public static Cookie createForcedGroupsCookie(
            final String contextPath, final Map<String, Integer> forceGroups) {
        return doCreateForcedGroupsCookie(contextPath, forceGroups);
    }

    // TODO: can be merged into createForcedGroupsCookieUnlessEmpty once createForcedGroupsCookie()
    // was deleted

    /** @deprecated Use {@link #doCreateForcedGroupsCookie(String, ForceGroupsOptions)} */
    @Deprecated
    private static Cookie doCreateForcedGroupsCookie(
            final String contextPath, final Map<String, Integer> forceGroups) {
        return doCreateForcedGroupsCookie(
                contextPath, ForceGroupsOptions.builder().putAllForceGroups(forceGroups).build());
    }
}
