package com.indeed.proctor.store;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.revwalk.RevCommit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author atran
 */
public class GitProctorUtils {

    public static final int DEFAULT_GIT_PULL_PUSH_TIMEOUT_SECONDS = 45;
    public static final int DEFAULT_GIT_CLONE_TIMEOUT_SECONDS = 180;

    private GitProctorUtils() {
        // util class - not meant to be instantiated
    }

    /**
     * Helper method to retrieve a canonical revision for git commits migrated from SVN. Migrated commits are
     * detected by the presence of 'git-svn-id' in the commit message.
     *
     * @param revision the commit/revision to inspect
     * @param branch   the name of the branch it came from
     * @return the original SVN revision if it was a migrated commit from the branch specified, otherwise the git revision
     */
    public static String resolveSvnMigratedRevision(final Revision revision, final String branch) {
        if (revision == null) {
            return null;
        }
        final Pattern pattern = Pattern.compile("^git-svn-id: .*" + branch + "@([0-9]+) ", Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(revision.getMessage());
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return revision.getRevision();
        }
    }

    /**
     * Determines id of a user who made a test edit from commit's author field.
     */
    public static String determineAuthorId(final RevCommit commit) {
        final String name = Strings.nullToEmpty(commit.getAuthorIdent().getName());
        final String email = Strings.nullToEmpty(commit.getAuthorIdent().getEmailAddress());
        return determineAuthorIdFromNameAndEmail(name, email);
    }

    @VisibleForTesting
    static String determineAuthorIdFromNameAndEmail(final String name, final String email) {
        /*
         * This logic determines if the commit comes from proctor webapp or manual push
         * and then estimates author's id from name or email.
         * See GitProctorUtilsTest for possible cases.
         */
        if (email.contains("@")) {
            // regarding local-part of the email as user's id for manual commits
            return StringUtils.split(email, "@")[0];
        } else {
            // otherwise using name as author's id at best effort.
            // it should be correct if it comes from proctor webapp.
            return name;
        }
    }
}
