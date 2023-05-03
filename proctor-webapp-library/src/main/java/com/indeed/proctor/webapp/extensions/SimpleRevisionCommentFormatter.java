package com.indeed.proctor.webapp.extensions;

import java.util.Map;

/**
 * Simple formatter.
 */
public class SimpleRevisionCommentFormatter implements RevisionCommitCommentFormatter {
    @Override
    public String formatComment(final String comment, final Map<String, String[]> extensionFields) {
        return comment;
    }
}
