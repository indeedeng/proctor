package com.indeed.proctor.consumer;

import com.google.common.collect.ImmutableSet;
import com.indeed.proctor.common.ForceGroupsOptions;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.Proctor;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestType;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.Map;

import static com.indeed.proctor.consumer.ProctorConsumerUtils.FORCE_GROUPS_PARAMETER;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AbstractGroupsManagerTest {

    @Test
    public void testDetermineBuckets() {

        final Proctor proctorMock = mock(Proctor.class);
        final Identifiers identifiers = Identifiers.of(TestType.ANONYMOUS_USER, "fooUser");
        final AbstractGroupsManager manager = new AbstractGroupsManager(() -> proctorMock) {
            @Override
            public Map<String, String> getProvidedContext() {
                return null;
            }

            @Override
            protected Map<String, TestBucket> getDefaultBucketValues() {
                return null;
            }
        };

        final HttpServletRequest httpRequestMock = mock(HttpServletRequest.class);
        final HttpServletResponse httpResponseMock = mock(HttpServletResponse.class);
        {
            // no force groups
            manager.determineBucketsInternal(
                    httpRequestMock, httpResponseMock, identifiers, emptyMap(), true, new HashSet<String>());
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
            manager.determineBucketsInternal(
                    httpRequestMock, httpResponseMock, identifiers, emptyMap(), false, new HashSet<String>());
            verifyNoMoreInteractions(httpRequestMock, httpResponseMock);
            clearInvocations(httpRequestMock, httpResponseMock);
        }
        {
            // allow force groups = true
            manager.determineBucketsInternal(
                    httpRequestMock, httpResponseMock, identifiers, emptyMap(), true, new HashSet<String>());
            verify(httpRequestMock, times(1)).getContextPath();
            verify(httpRequestMock, times(1)).getParameter(anyString());
            verify(httpResponseMock, times(1)).addCookie(isA(Cookie.class));
            verifyNoMoreInteractions(httpRequestMock, httpResponseMock);
            clearInvocations(httpRequestMock, httpResponseMock);
        }

    }

    @Test
    public void testCallbacksCalled() {
        final Proctor proctorMock = mock(Proctor.class);
        final ProctorResult proctorResultMock = mock(ProctorResult.class);
        final Logger loggerMock = mock(Logger.class);
        final Identifiers identifiers = Identifiers.of(TestType.ANONYMOUS_USER, "fooUser");
        final AbstractGroupsManager manager = new AbstractGroupsManager(
                () -> proctorMock,
                () -> new GroupsManagerInterceptor() {
                    @Override
                    public void beforeDetermineGroups(
                            final Identifiers identifiers,
                            final Map<String, Object> context,
                            final Map<String, Integer> forcedGroups
                    ) {
                        loggerMock.info("called before");
                    }

                    @Override
                    public void afterDetermineGroups(final ProctorResult proctorResult) {
                        loggerMock.info("called after");
                    }
                }
        ) {
            @Override
            public Map<String, String> getProvidedContext() {
                return null;
            }

            @Override
            protected Map<String, TestBucket> getDefaultBucketValues() {
                return null;
            }
        };

        when(proctorMock.determineTestGroups(identifiers, emptyMap(), emptyMap()))
                .thenReturn(proctorResultMock);

        manager.determineBucketsInternal(identifiers, emptyMap(), ForceGroupsOptions.empty());

        verify(proctorMock).determineTestGroups(identifiers, emptyMap(), ForceGroupsOptions.empty(), emptyList());
        verify(loggerMock).info("called before");
        verify(loggerMock).info("called after");
    }
}
