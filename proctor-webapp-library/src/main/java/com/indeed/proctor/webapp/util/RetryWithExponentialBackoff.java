package com.indeed.proctor.webapp.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Util to run a supplier several time until success or max-attempts
 */
public class RetryWithExponentialBackoff {

    private static final Logger LOGGER = LogManager.getLogger(RetryWithExponentialBackoff.class);

    /**
     * This tries supplier.get() up to maxAttemptCount times.
     * Between each attempts, it has interval time which is increased by twice.
     * The initial interval time is 1 second.
     * The maximum interval time is 2^maxAttemptIntervalIncrease seconds.
     * @param supplier this can throw an exception.
     * @param maxAttemptCount the max limit of the number of retries
     * @param maxAttemptIntervalIncrease the max limit of the interval time increase
     * @param reportFailOnce log the exception and attempt-count when supplier fails.
     * @param <T> the type of the return value of supplier.
     * @return the result of supplier.get() wrapped by Optional. It can be empty if it failed after maxAttemptCount attempts.
     */
    public <T> Optional<T> retry(
            final Supplier<T> supplier,
            final int maxAttemptCount,
            final long maxAttemptIntervalIncrease,
            final BiConsumer<Exception, Integer> reportFailOnce
    ) {
        for (int attemptCount = 0; attemptCount < maxAttemptCount; attemptCount++) {
            if (attemptCount > 0) {
                final long sleepTimeMillis = (1L << Math.min(attemptCount - 1, maxAttemptIntervalIncrease)) * 1000;
                sleep(sleepTimeMillis);
            }

            try {
                final T value = supplier.get();
                if (value == null) {
                    throw new IllegalStateException("Supplier supplied null");
                }
                return Optional.of(value);
            } catch (final Exception e) {
                reportFailOnce.accept(e, attemptCount + 1);
            }
        }
        return Optional.empty();
    }

    public void sleep(final long sleepTimeMillis) {
        try {
            LOGGER.info(String.format("Sleep %s seconds before retrying", sleepTimeMillis / 1000));
            Thread.sleep(sleepTimeMillis);
        } catch (final InterruptedException e) {
            LOGGER.error("Failed to sleep", e);
        }
    }

}
