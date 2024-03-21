package com.indeed.proctor.consumer.spring;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.indeed.proctor.common.ForceGroupsOptions;
import com.indeed.proctor.common.Proctor;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.consumer.ProctorConsumerUtils;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import static com.indeed.proctor.consumer.ProctorConsumerUtils.FORCE_GROUPS_PARAMETER;
import static com.indeed.proctor.consumer.ProctorConsumerUtils.createForcedGroupsCookie;
import static com.indeed.proctor.consumer.ProctorConsumerUtils.getForceGroupsStringFromRequest;
import static com.indeed.proctor.consumer.ProctorConsumerUtils.parseForceGroupsList;
import static com.indeed.proctor.consumer.ProctorConsumerUtils.setForcedGroupsCookie;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/** @author gaurav */
public class TestProctorConsumerUtils {
    private static final HashSet<String> EMPTY_SET = new HashSet<>();

    @Test
    public void testDetermineBuckets() {
        final HttpServletRequest httpRequestMock = mock(HttpServletRequest.class);
        final HttpServletResponse httpResponseMock = mock(HttpServletResponse.class);
        final Proctor proctorMock = mock(Proctor.class);
        {
            // no force groups
            ProctorConsumerUtils.determineBuckets(
                    httpRequestMock,
                    httpResponseMock,
                    proctorMock,
                    "foo",
                    TestType.ANONYMOUS_USER,
                    emptyMap(),
                    true,
                    EMPTY_SET);
            verify(httpRequestMock, times(1)).getContextPath();
            verify(httpRequestMock, times(1)).getHeader(anyString());
            verify(httpRequestMock, times(1)).getCookies();
            verify(httpRequestMock, times(1)).getParameter(anyString());
            verifyNoMoreInteractions(httpRequestMock, httpResponseMock);
            clearInvocations(httpRequestMock, httpResponseMock);
        }
        {
            // allow force groups = false
            when(httpRequestMock.getParameter(FORCE_GROUPS_PARAMETER)).thenReturn("foo1");
            ProctorConsumerUtils.determineBuckets(
                    httpRequestMock,
                    httpResponseMock,
                    proctorMock,
                    "foo",
                    TestType.ANONYMOUS_USER,
                    emptyMap(),
                    false,
                    EMPTY_SET);
            verifyNoMoreInteractions(httpRequestMock, httpResponseMock);
            clearInvocations(httpRequestMock, httpResponseMock);
        }
        {
            // allow force groups = true
            ProctorConsumerUtils.determineBuckets(
                    httpRequestMock,
                    httpResponseMock,
                    proctorMock,
                    "foo",
                    TestType.ANONYMOUS_USER,
                    emptyMap(),
                    true,
                    EMPTY_SET);
            verify(httpRequestMock, times(1)).getContextPath();
            verify(httpRequestMock, times(1)).getParameter(anyString());
            verify(httpResponseMock, times(1)).addCookie(isA(Cookie.class));
            verifyNoMoreInteractions(httpRequestMock, httpResponseMock);
            clearInvocations(httpRequestMock, httpResponseMock);
        }
    }

    @Test
    public void testParseForcedGroups() {
        // empty request
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            final Map<String, Integer> forcedGroups =
                    ProctorConsumerUtils.parseForcedGroups(mockRequest);
            assertThat(forcedGroups).isEmpty();
        }
        // Test that parameter works
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addParameter(FORCE_GROUPS_PARAMETER, "testing2");
            final Map<String, Integer> forcedGroups =
                    ProctorConsumerUtils.parseForcedGroups(mockRequest);
            assertThat(forcedGroups).hasSize(1).containsEntry("testing", 2);
        }
        // Test that cookie works
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            final Cookie cookie =
                    new Cookie(ProctorConsumerUtils.FORCE_GROUPS_COOKIE_NAME, "testing3");
            mockRequest.setCookies(cookie);
            final Map<String, Integer> forcedGroups =
                    ProctorConsumerUtils.parseForcedGroups(mockRequest);
            assertThat(forcedGroups).hasSize(1).containsEntry("testing", 3);
        }
        // Test that header works
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addHeader(ProctorConsumerUtils.FORCE_GROUPS_HEADER, "testing4");
            final Map<String, Integer> forcedGroups =
                    ProctorConsumerUtils.parseForcedGroups(mockRequest);
            assertThat(forcedGroups).hasSize(1).containsEntry("testing", 4);
        }
        // empty parameter
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addParameter(FORCE_GROUPS_PARAMETER, "");
            final Map<String, Integer> forcedGroups =
                    ProctorConsumerUtils.parseForcedGroups(mockRequest);
            assertThat(forcedGroups).isEmpty();
        }
    }

    @Test
    public void testParseForcedGroups_WithValidPayload() {
        final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader(
                ProctorConsumerUtils.FORCE_GROUPS_HEADER, "testing5;stringValue:\"forcePayload\"");
        final ForceGroupsOptions forceGroupsOptions =
                ProctorConsumerUtils.parseForcedGroupsOptions(
                        mockRequest, ImmutableSet.of("testing"));
        assertThat(forceGroupsOptions.getForcePayloads()).hasSize(1);
        assertThat(forceGroupsOptions.getForcePayloads())
                .containsEntry("testing", new Payload("forcePayload"));
    }

    @Test
    public void testParseForcedGroups_WithInvalidPayload() {
        final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader(
                ProctorConsumerUtils.FORCE_GROUPS_HEADER,
                "wrongTestName5;stringValue:\"forcePayload\"");
        final ForceGroupsOptions forceGroupsOptions =
                ProctorConsumerUtils.parseForcedGroupsOptions(
                        mockRequest, ImmutableSet.of("testing"));
        assertThat(forceGroupsOptions.getForcePayloads()).hasSize(0);
    }

    @Test
    public void testGetForceGroupsStringFromRequest() {
        // Test that parameter works
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addParameter(FORCE_GROUPS_PARAMETER, "testing1");
            final String forceGroups = getForceGroupsStringFromRequest(mockRequest);
            assertThat(forceGroups).isEqualTo("testing1");
        }
        // Test that header works
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addHeader(ProctorConsumerUtils.FORCE_GROUPS_HEADER, "testing1");
            final String forceGroups = getForceGroupsStringFromRequest(mockRequest);
            assertThat(forceGroups).isEqualTo("testing1");
        }
        // Test that cookie works
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            final Cookie cookie =
                    new Cookie(ProctorConsumerUtils.FORCE_GROUPS_COOKIE_NAME, "testing1");
            mockRequest.setCookies(cookie);
            final String forceGroups = getForceGroupsStringFromRequest(mockRequest);
            assertThat(forceGroups).isEqualTo("testing1");
        }
        // Test that parameter beats cookie
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addParameter(FORCE_GROUPS_PARAMETER, "testing1");
            final Cookie cookie =
                    new Cookie(ProctorConsumerUtils.FORCE_GROUPS_COOKIE_NAME, "testing2");
            mockRequest.setCookies(cookie);
            final String forceGroups = getForceGroupsStringFromRequest(mockRequest);
            assertThat(forceGroups).isEqualTo("testing1");
        }
        // Test that parameter beats header
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addParameter(FORCE_GROUPS_PARAMETER, "testing1");
            mockRequest.addHeader(ProctorConsumerUtils.FORCE_GROUPS_HEADER, "testing2");
            final String forceGroups = getForceGroupsStringFromRequest(mockRequest);
            assertThat(forceGroups).isEqualTo("testing1");
        }
        // Test that parameter beats header and cookie
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addParameter(FORCE_GROUPS_PARAMETER, "testing1");
            mockRequest.addHeader(ProctorConsumerUtils.FORCE_GROUPS_HEADER, "testing2");
            final Cookie cookie =
                    new Cookie(ProctorConsumerUtils.FORCE_GROUPS_COOKIE_NAME, "testing3");
            mockRequest.setCookies(cookie);
            final String forceGroups = getForceGroupsStringFromRequest(mockRequest);
            assertThat(forceGroups).isEqualTo("testing1");
        }
        // Test that header beats cookie
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addHeader(ProctorConsumerUtils.FORCE_GROUPS_HEADER, "testing1");
            final Cookie cookie =
                    new Cookie(ProctorConsumerUtils.FORCE_GROUPS_COOKIE_NAME, "testing2");
            mockRequest.setCookies(cookie);
            final String forceGroups = getForceGroupsStringFromRequest(mockRequest);
            assertThat(forceGroups).isEqualTo("testing1");
        }
        // Test missing param, missing header, missing cookie
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            final String forceGroups = getForceGroupsStringFromRequest(mockRequest);
            assertThat(forceGroups).isEqualTo("");
        }
        // Test empty param, some cookies
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            final Cookie junkCookie = new Cookie("random", "totally random");
            final Cookie anotherJunkCookie = new Cookie("what", "yeah");
            mockRequest.setCookies(junkCookie, anotherJunkCookie);
            final String forceGroups = getForceGroupsStringFromRequest(mockRequest);
            assertThat(forceGroups).isEqualTo("");
        }
    }

    @Test
    public void testParseForceGroupsList() {
        // Test null string
        assertThat(parseForceGroupsList(null)).isEmpty();
        // Test empty string
        assertThat(parseForceGroupsList("")).isEmpty();
        // Test invalid string
        assertThat(parseForceGroupsList("fasdfasdf;zxcvwasdf")).isEmpty();
        // Test invalid numbers
        assertThat(parseForceGroupsList("fasdfasdf")).isEmpty();
        assertThat(parseForceGroupsList("test" + Integer.MAX_VALUE + "0")).isEmpty();
        assertThat(parseForceGroupsList("test-")).isEmpty();
        assertThat(parseForceGroupsList("test0-")).isEmpty();
        // Test single group
        assertThat(parseForceGroupsList("somerandomtst1"))
                .hasSize(1)
                .containsEntry("somerandomtst", 1);
        // not sure if this case needs to be supported...
        assertThat(parseForceGroupsList("somerandomtst" + Integer.MAX_VALUE))
                .hasSize(1)
                .containsEntry("somerandomtst", Integer.MAX_VALUE);
        assertThat(parseForceGroupsList("somerandomtst" + Integer.MIN_VALUE))
                .hasSize(1)
                .containsEntry("somerandomtst", Integer.MIN_VALUE);
        // Test multiple groups, multiple commas
        assertThat(parseForceGroupsList(",,somerandomtst1, \n,, someothertst0, notanothertst2,,"))
                .isEqualTo(
                        ImmutableMap.builder()
                                .put("somerandomtst", 1)
                                .put("someothertst", 0)
                                .put("notanothertst", 2)
                                .build());
        // Test multiple, duplicate groups, last one wins
        assertThat(parseForceGroupsList("testA1, testA2, testB2"))
                .isEqualTo(ImmutableMap.builder().put("testA", 2).put("testB", 2).build());

        // Test multiple groups with some invalid stuff
        assertThat(
                        parseForceGroupsList(
                                "somerandomtst1, someothertst0, notanothertst2,asdf;alksdfjzvc"))
                .isEqualTo(
                        ImmutableMap.builder()
                                .put("somerandomtst", 1)
                                .put("someothertst", 0)
                                .put("notanothertst", 2)
                                .build());
    }

    @Test
    public void testSetForcedGroupsCookie() {
        final HttpServletRequest httpRequestMock = mock(HttpServletRequest.class);
        final HttpServletResponse httpResponseMock = mock(HttpServletResponse.class);
        setForcedGroupsCookie(httpRequestMock, httpResponseMock, emptyMap());
        verify(httpRequestMock, times(1)).getContextPath();
        verifyNoMoreInteractions(httpRequestMock, httpResponseMock);
        clearInvocations(httpRequestMock);

        setForcedGroupsCookie(
                httpRequestMock, httpResponseMock, Collections.singletonMap("foo", 2));
        verify(httpResponseMock, times(1)).addCookie(isA(Cookie.class));
        verify(httpRequestMock, times(1)).getContextPath();
        verifyNoMoreInteractions(httpRequestMock, httpResponseMock);
    }

    @Test
    public void testCreateForcedGroupsCookie() {
        Cookie cookie = createForcedGroupsCookie("myapp", emptyMap());
        assertThat(cookie.getName()).isEqualTo("prforceGroups");
        assertThat(cookie.getValue()).isEqualTo("");
        assertThat(cookie.getPath()).isEqualTo("myapp");
        assertThat(cookie.getVersion()).isEqualTo(0);

        cookie = createForcedGroupsCookie("myapp", Collections.singletonMap("foo", 2));
        assertThat(cookie.getName()).isEqualTo("prforceGroups");
        assertThat(cookie.getValue()).isEqualTo("foo2");
        assertThat(cookie.getPath()).isEqualTo("myapp");
        assertThat(cookie.getVersion()).isEqualTo(0);

        final Map<String, Integer> forceGroups = new java.util.HashMap<>();
        forceGroups.put("foo", 2);
        forceGroups.put("bar", 3);
        cookie = createForcedGroupsCookie("myapp", forceGroups);
        assertThat(cookie.getName()).isEqualTo("prforceGroups");
        assertThat(cookie.getValue()).isEqualTo("bar3%2Cfoo2");
        assertThat(cookie.getPath()).isEqualTo("myapp");
        assertThat(cookie.getVersion()).isEqualTo(0);

        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(cookie);

        final String forceGroup = getForceGroupsStringFromRequest(request);

        assertThat(forceGroup).isEqualTo("bar3,foo2");
    }

    @Test
    public void testCreateForcedGroupsUnion() {
        final String forcedGroupsA = "first_test1,second_test0";
        final Cookie a = new Cookie(ProctorConsumerUtils.FORCE_GROUPS_COOKIE_NAME, forcedGroupsA);

        final String forcedGroupsB = "second_test-1,first_test2";
        final Cookie b = new Cookie(ProctorConsumerUtils.FORCE_GROUPS_COOKIE_NAME, forcedGroupsB);

        final String forcedGroupsC = "second_test0,third_test2";
        final Cookie c = new Cookie(ProctorConsumerUtils.FORCE_GROUPS_COOKIE_NAME, forcedGroupsC);

        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.setCookies(a, b, c);

            final String expectedForceGroupsString =
                    forcedGroupsA + "," + forcedGroupsB + "," + forcedGroupsC;
            final ForceGroupsOptions expectedForceGroupsOptions =
                    new ForceGroupsOptions.Builder()
                            .putForceGroup("first_test", 2)
                            .putForceGroup("second_test", 0)
                            .putForceGroup("third_test", 2)
                            .build();

            final String forceGroupsString = getForceGroupsStringFromRequest(mockRequest);
            final ForceGroupsOptions forceGroupsOptions =
                    ProctorConsumerUtils.parseForcedGroupsOptions(mockRequest);
            assertThat(forceGroupsString).isEqualTo(expectedForceGroupsString);
            assertThat(forceGroupsOptions).isEqualTo(expectedForceGroupsOptions);
        }
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.setCookies(b, c, a);

            final String expectedForceGroupsString =
                    forcedGroupsB + "," + forcedGroupsC + "," + forcedGroupsA;
            final ForceGroupsOptions expectedForceGroupsOptions =
                    new ForceGroupsOptions.Builder()
                            .putForceGroup("first_test", 1)
                            .putForceGroup("second_test", 0)
                            .putForceGroup("third_test", 2)
                            .build();

            final String forceGroupsString = getForceGroupsStringFromRequest(mockRequest);
            final ForceGroupsOptions forceGroupsOptions =
                    ProctorConsumerUtils.parseForcedGroupsOptions(mockRequest);
            assertThat(forceGroupsString).isEqualTo(expectedForceGroupsString);
            assertThat(forceGroupsOptions).isEqualTo(expectedForceGroupsOptions);
        }

        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.setCookies(c, a, b);

            final String expectedForceGroupsString =
                    forcedGroupsC + "," + forcedGroupsA + "," + forcedGroupsB;
            final ForceGroupsOptions expectedForceGroupsOptions =
                    new ForceGroupsOptions.Builder()
                            .putForceGroup("first_test", 2)
                            .putForceGroup("second_test", -1)
                            .putForceGroup("third_test", 2)
                            .build();

            final String forceGroupsString = getForceGroupsStringFromRequest(mockRequest);
            final ForceGroupsOptions forceGroupsOptions =
                    ProctorConsumerUtils.parseForcedGroupsOptions(mockRequest);
            assertThat(forceGroupsString).isEqualTo(expectedForceGroupsString);
            assertThat(forceGroupsOptions).isEqualTo(expectedForceGroupsOptions);
        }

        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.setCookies(
                    new Cookie(
                            ProctorConsumerUtils.FORCE_GROUPS_COOKIE_NAME,
                            "first_test2;longValue:1"));

            final String expectedForceGroupsString = "first_test2;longValue:1";
            final ForceGroupsOptions expectedForceGroupsOptions =
                    new ForceGroupsOptions.Builder()
                            .putForceGroup("first_test", 2)
                            .putForcePayload("first_test", new Payload(1L))
                            .build();

            final String forceGroupsString = getForceGroupsStringFromRequest(mockRequest);
            final ForceGroupsOptions forceGroupsOptions =
                    ProctorConsumerUtils.parseForcedGroupsOptions(
                            mockRequest, ImmutableSet.of("first_test"));
            assertThat(forceGroupsString).isEqualTo(expectedForceGroupsString);
            assertThat(forceGroupsOptions).isEqualTo(expectedForceGroupsOptions);
        }
    }
}
