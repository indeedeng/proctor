package com.indeed.proctor.webapp.util;

import org.apache.log4j.Logger;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Util to run a supplier several time until success or max-retries
 */
public class RetryWithExponentialBackoff {

    private static final Logger LOGGER = Logger.getLogger(RetryWithExponentialBackoff.class);

    /**
     * This tries supplier.get() up to maxRetryCount times.
     * For each retires, it has interval time which is increased by twice.
     * The initial interval time is 1 second.
     * The maximum interval time is 2^maxRetryIntervalIncrease seconds.
     * @param supplier this can throw an exception.
     * @param maxRetryCount the max limit of the number of retries
     * @param maxRetryIntervalIncrease the max limit of the interval time increase
     * @param reportFailOnce log the exception and retry-count when supplier fails.
     * @param <T> the type of the return value of supplier.
     * @return the result of supplier.get() wrapped by Optional. It can be empty if it failed after maxRetryCount retries.
     */
    public static <T> Optional<T> retry(
            final Supplier<T> supplier,
            final int maxRetryCount,
            final long maxRetryIntervalIncrease,
            final BiConsumer<Throwable, Integer> reportFailOnce
    ) {
        for (int retryCount = 0; retryCount < maxRetryCount; retryCount++) {
            if (retryCount > 0) {
                final long sleepTimeMillis = (1L << Math.min(retryCount - 1, maxRetryIntervalIncrease)) * 1000;
                sleep(sleepTimeMillis);
            }

            try {
                final T value = supplier.get();
                if (value == null) {
                    throw new IllegalStateException("Supplier supplied null");
                }
                return Optional.of(value);
            } catch (final Exception e) {
                reportFailOnce.accept(e, retryCount);
            }
        }
        return Optional.empty();
    }

    static void sleep(final long sleepTimeMillis) {
        try {
            LOGGER.info(String.format("Sleep %s seconds before retrying", sleepTimeMillis / 1000));
            Thread.sleep(sleepTimeMillis);
        } catch (final InterruptedException e) {
            LOGGER.error("Failed to sleep", e);
        }
    }

}
