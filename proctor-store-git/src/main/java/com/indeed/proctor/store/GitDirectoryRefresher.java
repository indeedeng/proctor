package com.indeed.proctor.store;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.util.TimerTask;
import java.util.concurrent.Callable;

/**
 * Timer task used to periodically run git fetch/reset in a git directory
 * We don't need refresher task anymore. Call {@link ProctorStore#refresh()} to pull the change.
 */
@Deprecated
public class GitDirectoryRefresher extends TimerTask {
    private static final Logger LOGGER = Logger.getLogger(GitDirectoryRefresher.class);
    private static final TextProgressMonitor PROGRESS_MONITOR = new TextProgressMonitor(new LoggerPrintWriter(LOGGER, Level.DEBUG));
    private final GitProctorCore gitProctorCore;
    private final GitWorkspaceProvider workspaceProvider;
    private final UsernamePasswordCredentialsProvider user;

    GitDirectoryRefresher(final GitWorkspaceProvider workspaceProvider,
                          final GitProctorCore git,
                          final String username,
                          final String password) {
        this.workspaceProvider = workspaceProvider;
        this.gitProctorCore = git;
        this.user = new UsernamePasswordCredentialsProvider(username, password);
    }

    @Override
    public void run() {
        workspaceProvider.synchronizedOperation(new Callable<Void>() {
            @Override
            public Void call() {
                try {

                    final PullResult result = gitProctorCore.getGit()
                            .pull()
                            .setProgressMonitor(PROGRESS_MONITOR)
                            .setRebase(true)
                            .setCredentialsProvider(user)
                            .setTimeout(GitProctorUtils.DEFAULT_GIT_PULL_PUSH_TIMEOUT_SECONDS)
                            .call();
                    if (!result.isSuccessful()) {
                        /** if git pull failed, use git reset **/
                        gitProctorCore.undoLocalChanges();
                    }
                } catch (final Exception e) {
                    LOGGER.error("Error when refreshing git directory " + getDirectoryPath(), e);
                }
                return null;
            }
        });

    }

    public String getDirectoryPath() {
        return workspaceProvider.getRootDirectory().getPath();
    }
}
