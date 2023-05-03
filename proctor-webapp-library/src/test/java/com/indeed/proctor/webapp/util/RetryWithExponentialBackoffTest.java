package com.indeed.proctor.webapp.util;

import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RetryWithExponentialBackoffTest {
    private static final int MAX_ATTEMPT_COUNT = 10;
    private static final int MAX_ATTEMPT_INTERVAL_INCREASE = 8;
    private static final int SUPPLIER_RESULT = 123;
    private static final Supplier<Integer> SUPPLIER = () -> SUPPLIER_RESULT;

    @Mock
    private Supplier<Integer> supplier;
    @Mock
    private Function<Long, Void> sleep;
    @Mock
    private BiConsumer<Exception, Integer> reportFailOnce;

    private final RetryWithExponentialBackoff retryWithExponentialBackoff = new RetryWithExponentialBackoff() {
        @Override
        public void sleep(final long sleepTimeMillis) {
            try {
                sleep.apply(sleepTimeMillis);
            } catch (final Exception e) {
                fail();
            }
        }
    };

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(sleep.apply(anyLong())).thenReturn(null);
        doNothing().when(reportFailOnce).accept(any(Exception.class), anyInt());
    }

    @Test
    public void testNoRetry() {
        final Optional result = retryWithExponentialBackoff.retry(
                SUPPLIER,
                MAX_ATTEMPT_COUNT,
                MAX_ATTEMPT_INTERVAL_INCREASE,
                reportFailOnce
        );

        assertEquals(Optional.of(SUPPLIER_RESULT), result);
        verify(reportFailOnce, never()).accept(any(Exception.class), anyInt());
    }

    @Test
    public void testRetry() {
        when(supplier.get())
                .thenThrow(new RuntimeException("something went wrong"))
                .thenReturn(null)
                .thenReturn(SUPPLIER_RESULT);

        final Optional result = retryWithExponentialBackoff.retry(
                supplier,
                MAX_ATTEMPT_COUNT,
                MAX_ATTEMPT_INTERVAL_INCREASE,
                reportFailOnce
        );

        assertEquals(Optional.of(SUPPLIER_RESULT), result);
        verify(supplier, times(3)).get();
        verify(reportFailOnce, times(2)).accept(any(Exception.class), anyInt());
        verify(sleep).apply(1000L);
        verify(sleep).apply(2000L);
    }

    private void testRetryNeverSucceeds(final int maxAttemptCount, final int maxAttemptIntervalIncrease) {
        when(supplier.get()).thenThrow(new RuntimeException("something went wrong"));

        final Optional result = retryWithExponentialBackoff.retry(
                supplier,
                maxAttemptCount,
                maxAttemptIntervalIncrease,
                reportFailOnce
        );

        assertEquals(Optional.empty(), result);
        verify(supplier, times(maxAttemptCount)).get();
        verify(reportFailOnce, times(maxAttemptCount)).accept(any(Exception.class), anyInt());
        verify(sleep, times(maxAttemptCount - 1))
                .apply(AdditionalMatchers.leq((1L << maxAttemptIntervalIncrease) * 1000));
        verify(sleep, never())
                .apply(AdditionalMatchers.gt((1L << maxAttemptIntervalIncrease) * 1000));
    }

    @Test
    public void testRetryNeverSucceeds() {
        testRetryNeverSucceeds(MAX_ATTEMPT_COUNT, MAX_ATTEMPT_INTERVAL_INCREASE);
    }

    @Test
    public void testRetryNeverSucceedsWithDifferentMaxAttemptIntervalIncrease() {
        testRetryNeverSucceeds(MAX_ATTEMPT_COUNT, 4);
    }

    @Test
    public void testRetryNeverSucceedsWithDifferentMaxAttemptCount() {
        testRetryNeverSucceeds(15, MAX_ATTEMPT_INTERVAL_INCREASE);
    }
}
