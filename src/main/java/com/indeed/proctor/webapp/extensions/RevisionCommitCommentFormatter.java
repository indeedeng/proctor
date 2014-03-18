package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.common.model.TestDefinition;

import java.util.Map;

/**
 */
public interface RevisionCommitCommentFormatter {
    public String formatComment(final String comment, final Map<String, String[]> extensionFields);
}
