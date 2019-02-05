package com.indeed.proctor.webapp.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.verification.VerificationModeFactory;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { RetryWithExponentialBackoff.class } )
public class RetryWithExponentialBackoffTest {
    private static final int MAX_RETRY_COUNT = 10;
    private static final int MAX_RETRY_INTERVAL_INCREASE = 8;
    private static final BiConsumer<Throwable, Integer> REPORT_FAIL_ONCE = (throwable, retryCount) -> {};

    @Mock
    private Supplier<Integer> supplier;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        PowerMockito.spy(Thread.class);
        PowerMockito.doNothing().when(Thread.class);
        Thread.sleep(anyLong());
    }

    @Test
    public void testNoRetry() {
        when(supplier.get()).thenReturn(1);
        RetryWithExponentialBackoff.retry(supplier, MAX_RETRY_COUNT, MAX_RETRY_INTERVAL_INCREASE, REPORT_FAIL_ONCE);

        verify(supplier, times(1)).get();
    }

    @Test
    public void testRetry() throws Exception {
        when(supplier.get())
                .thenThrow(new RuntimeException("something went wrong"))
                .thenReturn(null)
                .thenReturn(1);
        RetryWithExponentialBackoff.retry(supplier, MAX_RETRY_COUNT, MAX_RETRY_INTERVAL_INCREASE, REPORT_FAIL_ONCE);

        verify(supplier, times(3)).get();
        PowerMockito.verifyStatic(VerificationModeFactory.times(1));
        Thread.sleep(1000); // (2 ^ 0) * 1000
        PowerMockito.verifyStatic(VerificationModeFactory.times(1));
        Thread.sleep(2000); // (2 ^ 1) * 1000
    }
}
