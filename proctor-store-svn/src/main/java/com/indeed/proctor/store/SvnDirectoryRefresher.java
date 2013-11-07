package com.indeed.proctor.store;

import org.apache.log4j.Logger;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;

import java.io.File;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Timer task used to periodically refresh an directory
 * @author parker
 */
public class SvnDirectoryRefresher extends TimerTask {
    final Logger LOGGER = Logger.getLogger(SvnDirectoryRefresher.class);
    final AtomicBoolean shutdown;
    final File directory;
    final SVNURL svnUrl;
    final SVNClientManager userClientManager;

    SvnDirectoryRefresher(final AtomicBoolean shutdown,
                          final File directory,
                          final SVNURL svnUrl,
                          final SVNClientManager userClientManager) {
        this.shutdown = shutdown;
        this.directory = directory;
        this.svnUrl = svnUrl;
        this.userClientManager = userClientManager;
    }

    @Override
    public void run() {
        try {
            if(!shutdown.get()) {
                LOGGER.info("(svn) refresh of " + directory);
                final long ms = System.currentTimeMillis();
                SvnProctorUtils.cleanUpWorkingDir(LOGGER, directory, svnUrl, userClientManager);
                LOGGER.info("(svn) refresh of " + directory + " in " + ms + " ms");
            } else {
                LOGGER.info("Skipping svn refresh, shutdown in progress");
            }
        } catch (Exception exp) {
            LOGGER.warn("Exception during svn refresh of " + directory, exp);
        }
    }

    public String getDirectoryPath() {
        return directory.getPath();
    }
}
