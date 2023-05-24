package com.indeed.proctor.webapp.jobs;

import com.indeed.proctor.webapp.extensions.RevisionCommitCommentFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CommentFormatter {
    private final RevisionCommitCommentFormatter revisionCommitCommentFormatter;

    @Autowired
    public CommentFormatter(final RevisionCommitCommentFormatter revisionCommitCommentFormatter) {
        this.revisionCommitCommentFormatter = revisionCommitCommentFormatter;
    }

    public String formatFullComment(
            final String comment, final Map<String, String[]> requestParameterMap) {
        if (revisionCommitCommentFormatter != null) {
            return revisionCommitCommentFormatter.formatComment(comment, requestParameterMap);
        } else {
            return comment.trim();
        }
    }
}
