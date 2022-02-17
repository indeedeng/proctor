package com.indeed.proctor.consumer.logging;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class TestUsageCompositeObserverTest {
    @Test
    public void testMarkUsedForToggling() {
        final TestUsageObserver observer1 = mock(TestUsageObserver.class);
        final TestUsageObserver observer2 = mock(TestUsageObserver.class);

        final TestUsageCompositeObserver observer = new TestUsageCompositeObserver(observer1, observer2);

        final String testName = "test";
        observer.markUsedForToggling(testName);
        final List<String> testNames = ImmutableList.of("test1", "test2");
        observer.markUsedForToggling(testNames);
        verify(observer1).markUsedForToggling(testName);
        verify(observer2).markUsedForToggling(testName);
        verify(observer1).markUsedForToggling(testNames);
        verify(observer2).markUsedForToggling(testNames);
        verifyNoMoreInteractions(observer1, observer2);
    }
}
