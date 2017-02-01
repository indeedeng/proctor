package com.indeed.proctor.store;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author atran
 */
public class GitProctorUtils {

    public static int DEFAULT_GIT_PULL_PUSH_TIMEOUT_SECONDS = 45;
    public static int DEFAULT_GIT_CLONE_TIMEOUT_SECONDS = 180;

    private GitProctorUtils() {
        // util class - not meant to be instantiated
    }

    /**
     * Helper method to retrieve a canonical revision for git commits migrated from SVN. Migrated commits are
     * detected by the presence of 'git-svn-id' in the commit message.
     *
     * @param revision the commit/revision to inspect
     * @param branch the name of the branch it came from
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
}
