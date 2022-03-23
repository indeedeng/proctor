package com.indeed.proctor.consumer.spring;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.Proctor;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.consumer.ProctorConsumerUtils;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Map;

import static com.indeed.proctor.consumer.ProctorConsumerUtils.FORCE_GROUPS_PARAMETER;
import static com.indeed.proctor.consumer.ProctorConsumerUtils.createForcedGroupsCookie;
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

/**
 * @author gaurav
 */
public class TestProctorConsumerUtils {

    @Test
    public void testDetermineBuckets() {
        final HttpServletRequest httpRequestMock = mock(HttpServletRequest.class);
        final HttpServletResponse httpResponseMock = mock(HttpServletResponse.class);
        final Proctor proctorMock = mock(Proctor.class);
        {
            // no force groups
            ProctorConsumerUtils.determineBuckets(
                    httpRequestMock, httpResponseMock, proctorMock, "foo", TestType.ANONYMOUS_USER, emptyMap(), true);
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
                    httpRequestMock, httpResponseMock, proctorMock, "foo", TestType.ANONYMOUS_USER, emptyMap(), false);
            verifyNoMoreInteractions(httpRequestMock, httpResponseMock);
            clearInvocations(httpRequestMock, httpResponseMock);
        }
        {
            // allow force groups = true
            ProctorConsumerUtils.determineBuckets(
                    httpRequestMock, httpResponseMock, proctorMock, "foo", TestType.ANONYMOUS_USER, emptyMap(), true);
            verify(httpRequestMock, times(1)).getContextPath();
            verify(httpRequestMock, times(1)).getParameter(anyString());
            verify(httpResponseMock, times(1)).addCookie(isA(Cookie.class));
            verifyNoMoreInteractions(httpRequestMock, httpResponseMock);
            clearInvocations(httpRequestMock, httpResponseMock);
        }

    }

    @Test
    public void testParseForcedGroups() {
        //empty request
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            final Map<String, Integer> forcedGroups = ProctorConsumerUtils.parseForcedGroups(mockRequest);
            assertThat(forcedGroups).isEmpty();
        }
        //Test that parameter works
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addParameter(FORCE_GROUPS_PARAMETER, "testing2");
            final Map<String, Integer> forcedGroups = ProctorConsumerUtils.parseForcedGroups(mockRequest);
            assertThat(forcedGroups)
                    .hasSize(1)
                    .containsEntry("testing", 2);
        }
        //Test that cookie works
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            final Cookie cookie = new Cookie(ProctorConsumerUtils.FORCE_GROUPS_COOKIE_NAME, "testing3");
            mockRequest.setCookies(cookie);
            final Map<String, Integer> forcedGroups = ProctorConsumerUtils.parseForcedGroups(mockRequest);
            assertThat(forcedGroups)
                    .hasSize(1)
                    .containsEntry("testing", 3);
        }
        //Test that header works
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addHeader(ProctorConsumerUtils.FORCE_GROUPS_HEADER, "testing4");
            final Map<String, Integer> forcedGroups = ProctorConsumerUtils.parseForcedGroups(mockRequest);
            assertThat(forcedGroups)
                    .hasSize(1)
                    .containsEntry("testing", 4);
        }
        //empty parameter
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addParameter(FORCE_GROUPS_PARAMETER, "");
            final Map<String, Integer> forcedGroups = ProctorConsumerUtils.parseForcedGroups(mockRequest);
            assertThat(forcedGroups).isEmpty();
        }
    }


    @Test
    public void testGetForceGroupsStringFromRequest() {
        //Test that parameter works
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addParameter(FORCE_GROUPS_PARAMETER, "testing1");
            final String forceGroups = ProctorConsumerUtils.getForceGroupsStringFromRequest(mockRequest);
            assertThat(forceGroups).isEqualTo("testing1");
        }
        //Test that header works
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addHeader(ProctorConsumerUtils.FORCE_GROUPS_HEADER, "testing1");
            final String forceGroups = ProctorConsumerUtils.getForceGroupsStringFromRequest(mockRequest);
            assertThat(forceGroups).isEqualTo("testing1");
        }
        //Test that cookie works
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            final Cookie cookie = new Cookie(ProctorConsumerUtils.FORCE_GROUPS_COOKIE_NAME, "testing1");
            mockRequest.setCookies(cookie);
            final String forceGroups = ProctorConsumerUtils.getForceGroupsStringFromRequest(mockRequest);
            assertThat(forceGroups).isEqualTo("testing1");
        }
        //Test that parameter beats cookie
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addParameter(FORCE_GROUPS_PARAMETER, "testing1");
            final Cookie cookie = new Cookie(ProctorConsumerUtils.FORCE_GROUPS_COOKIE_NAME, "testing2");
            mockRequest.setCookies(cookie);
            final String forceGroups = ProctorConsumerUtils.getForceGroupsStringFromRequest(mockRequest);
            assertThat(forceGroups).isEqualTo("testing1");
        }
        //Test that parameter beats header
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addParameter(FORCE_GROUPS_PARAMETER, "testing1");
            mockRequest.addHeader(ProctorConsumerUtils.FORCE_GROUPS_HEADER, "testing2");
            final String forceGroups = ProctorConsumerUtils.getForceGroupsStringFromRequest(mockRequest);
            assertThat(forceGroups).isEqualTo("testing1");
        }
        //Test that parameter beats header and cookie
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addParameter(FORCE_GROUPS_PARAMETER, "testing1");
            mockRequest.addHeader(ProctorConsumerUtils.FORCE_GROUPS_HEADER, "testing2");
            final Cookie cookie = new Cookie(ProctorConsumerUtils.FORCE_GROUPS_COOKIE_NAME, "testing3");
            mockRequest.setCookies(cookie);
            final String forceGroups = ProctorConsumerUtils.getForceGroupsStringFromRequest(mockRequest);
            assertThat(forceGroups).isEqualTo("testing1");
        }
        //Test that header beats cookie
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            mockRequest.addHeader(ProctorConsumerUtils.FORCE_GROUPS_HEADER, "testing1");
            final Cookie cookie = new Cookie(ProctorConsumerUtils.FORCE_GROUPS_COOKIE_NAME, "testing2");
            mockRequest.setCookies(cookie);
            final String forceGroups = ProctorConsumerUtils.getForceGroupsStringFromRequest(mockRequest);
            assertThat(forceGroups).isEqualTo("testing1");
        }
        //Test missing param, missing header, missing cookie
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            final String forceGroups = ProctorConsumerUtils.getForceGroupsStringFromRequest(mockRequest);
            assertThat(forceGroups).isEqualTo("");
        }
        //Test empty param, some cookies
        {
            final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
            final Cookie junkCookie = new Cookie("random", "totally random");
            final Cookie anotherJunkCookie = new Cookie("what", "yeah");
            mockRequest.setCookies(junkCookie, anotherJunkCookie);
            final String forceGroups = ProctorConsumerUtils.getForceGroupsStringFromRequest(mockRequest);
            assertThat(forceGroups).isEqualTo("");
        }
    }


    @Test
    public void testSetForcedGroupsCookie() {
        final HttpServletRequest httpRequestMock = mock(HttpServletRequest.class);
        final HttpServletResponse httpResponseMock = mock(HttpServletResponse.class);
        setForcedGroupsCookie(httpRequestMock, httpResponseMock, emptyMap());
        verify(httpRequestMock, times(1)).getContextPath();
        verifyNoMoreInteractions(httpRequestMock, httpResponseMock);
        clearInvocations(httpRequestMock);

        setForcedGroupsCookie(httpRequestMock, httpResponseMock, Collections.singletonMap("foo", 2));
        verify(httpResponseMock, times(1)).addCookie(isA(Cookie.class));
        verify(httpRequestMock, times(1)).getContextPath();
        verifyNoMoreInteractions(httpRequestMock, httpResponseMock);
    }

    @Test
    public void testCreateForcedGroupsCookie() {
        Cookie cookie = createForcedGroupsCookie("myapp", emptyMap());
        assertThat(cookie.getName()).isEqualTo("prforceGroups");
        assertThat(cookie.getValue()).isEqualTo("\"\"");
        assertThat(cookie.getPath()).isEqualTo("myapp");
        assertThat(cookie.getVersion()).isEqualTo(0);


        cookie = createForcedGroupsCookie("myapp", Collections.singletonMap("foo", 2));
        assertThat(cookie.getName()).isEqualTo("prforceGroups");
        assertThat(cookie.getValue()).isEqualTo("\"foo2\"");
        assertThat(cookie.getPath()).isEqualTo("myapp");
        assertThat(cookie.getVersion()).isEqualTo(0);
    }

}
