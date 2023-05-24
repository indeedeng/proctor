package com.indeed.proctor.webapp.extensions;

import java.util.Map;

/** */
public interface RevisionCommitCommentFormatter {
    String formatComment(final String comment, final Map<String, String[]> extensionFields);
}
