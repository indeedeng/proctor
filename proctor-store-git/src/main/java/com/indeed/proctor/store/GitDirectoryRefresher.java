package com.indeed.proctor.store;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.util.TimerTask;

/**
 * Timer task used to periodically run git fetch/reset in a git directory
 */
public class GitDirectoryRefresher extends TimerTask {
    private static final Logger LOGGER = Logger.getLogger(GitDirectoryRefresher.class);
    private static final TextProgressMonitor PROGRESS_MONITOR = new TextProgressMonitor(new LoggerPrintWriter(LOGGER, Level.DEBUG));
    private final File directory;
    private final GitProctorCore gitProctorCore;
    private final UsernamePasswordCredentialsProvider user;

    GitDirectoryRefresher(final File directory,
                          final GitProctorCore git,
                          final String username,
                          final String password) {
        this.directory = directory;
        this.gitProctorCore = git;
        this.user = new UsernamePasswordCredentialsProvider(username, password);
    }

    @Override
    public void run() {
        try {
            synchronized (directory) {
                gitProctorCore.getGit().fetch().setProgressMonitor(PROGRESS_MONITOR).setCredentialsProvider(user).call();
                gitProctorCore.undoLocalChanges();
                gitProctorCore.getGit().gc().setProgressMonitor(PROGRESS_MONITOR).call(); /** clean garbage **/
            }
        } catch (GitAPIException e) {
            LOGGER.error("Error when calling git pull", e);
        }
    }

    public String getDirectoryPath() {
        return directory.getPath();
    }
}
