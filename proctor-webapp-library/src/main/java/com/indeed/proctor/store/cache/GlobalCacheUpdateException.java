package com.indeed.proctor.store.cache;

/**
 * Exception thrown when {@code GlobalCacheStore} failed to update its cache in any reason like race condition.
 */
public class GlobalCacheUpdateException extends RuntimeException {
    public GlobalCacheUpdateException(final String message) {
        super(message);
    }

    public GlobalCacheUpdateException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Exception thrown when failed to update global cache since history can't be read from delegated proctor store.
     */
    public static class HistoryReadException extends GlobalCacheUpdateException {
        public HistoryReadException(final String message) {
            super(message);
        }

        public HistoryReadException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
