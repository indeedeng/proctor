package com.indeed.proctor.store;

import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.util.TimerTask;

/**
 * Timer task used to periodically run git pull in a git directory
 */
public class GitDirectoryRefresher extends TimerTask {
    private final Logger LOGGER = Logger.getLogger(GitDirectoryRefresher.class);
    private final File directory;
    private final Git git;
    private final UsernamePasswordCredentialsProvider user;

    GitDirectoryRefresher(final File directory,
                          final Git git,
                          final String username,
                          final String password) {
        this.directory = directory;
        this.git = git;
        this.user = new UsernamePasswordCredentialsProvider(username, password);
    }

    @Override
    public void run() {
        try {
            git.pull().setCredentialsProvider(user).call();
        } catch (GitAPIException e) {
            LOGGER.error("Error when calling git pull", e);
        }
    }

    public String getDirectoryPath() {
        return directory.getPath();
    }
}
