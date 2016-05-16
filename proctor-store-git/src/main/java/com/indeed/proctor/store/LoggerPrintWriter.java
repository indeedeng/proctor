package com.indeed.proctor.store;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

/**
 * java.io.PrintWriter adapter for org.apache.log4j.Logger.
 */
class LoggerPrintWriter extends PrintWriter {
    public LoggerPrintWriter(final Logger logger, final Level level) {
        super(new InternalLoggerWriter(logger, level));
    }

    static class InternalLoggerWriter extends Writer {

        final Logger logger;
        final Level level;

        InternalLoggerWriter(final Logger logger, final Level level) {
            this.logger = logger;
            this.level = level;
        }

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            if (logger.isEnabledFor(level)) {
                // Remove leading carriage returns inserted by jgit
                if (cbuf[off] == '\r') {
                    off++;
                    len--;
                }
                logger.log(level, String.copyValueOf(cbuf, off, len));
            }
        }

        @Override
        public void flush() throws IOException {
            // no-op
        }

        @Override
        public void close() throws IOException {
            // no-op
        }
    }
}
