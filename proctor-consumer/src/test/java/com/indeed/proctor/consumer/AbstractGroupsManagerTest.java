package com.indeed.proctor.consumer;

import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.Proctor;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestType;
import org.apache.log4j.Logger;
import org.junit.Test;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.util.Map;
import java.util.Timer;

import static com.indeed.proctor.consumer.ProctorConsumerUtils.FORCE_GROUPS_PARAMETER;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
                    httpRequestMock, httpResponseMock, identifiers, emptyMap(), true);
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
                    httpRequestMock, httpResponseMock, identifiers, emptyMap(), false);
            verifyNoMoreInteractions(httpRequestMock, httpResponseMock);
            clearInvocations(httpRequestMock, httpResponseMock);
        }
        {
            // allow force groups = true
            manager.determineBucketsInternal(
                    httpRequestMock, httpResponseMock, identifiers, emptyMap(), true);
            verify(httpRequestMock, times(1)).getContextPath();
            verify(httpRequestMock, times(1)).getParameter(anyString());
            verify(httpResponseMock, times(1)).addCookie(isA(Cookie.class));
            verifyNoMoreInteractions(httpRequestMock, httpResponseMock);
            clearInvocations(httpRequestMock, httpResponseMock);
        }

    }

    @Test
    public void testLogGroupResolutionTime() {
        final Proctor proctorMock = mock(Proctor.class);
        final ProctorResult proctorResultMock = mock(ProctorResult.class);
        final Logger loggerMock = mock(Logger.class);
        final Identifiers identifiers = Identifiers.of(TestType.ANONYMOUS_USER, "fooUser");
        final AbstractGroupsManager manager = new AbstractGroupsManager(
                () -> proctorMock,
                (time) -> loggerMock.info("logging resolution time: " + time)
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

        manager.determineBucketsInternal(identifiers, emptyMap(), emptyMap());

        verify(proctorMock).determineTestGroups(identifiers, emptyMap(), emptyMap());
        verify(loggerMock).info(anyString());
    }

    @Test
    public void testStopWatch() {
        final Clock clockMock = mock(Clock.class);
        final AbstractGroupsManager.StopWatch stopWatch = new AbstractGroupsManager.StopWatch(clockMock);

        final long startMillis = 100;
        final long stopMillis = 125;
        when(clockMock.millis())
                .thenReturn(startMillis)
                .thenReturn(stopMillis);

        stopWatch.start();
        final long result = stopWatch.stop();

        assertThat(result).isEqualTo(stopMillis - startMillis);
    }

    @Test
    public void testStopWatchThrowsWhenStoppedBeforeStarting() {
        final Clock clockMock = mock(Clock.class);
        final AbstractGroupsManager.StopWatch stopWatch = new AbstractGroupsManager.StopWatch(clockMock);

        assertThatThrownBy(stopWatch::stop)
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Timer stop called before start");
    }
}
