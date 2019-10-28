package com.indeed.proctor.store;

import org.apache.log4j.Logger;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;
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
    final SvnPersisterCore svnPersisterCore;

    SvnDirectoryRefresher(final AtomicBoolean shutdown,
                          final File directory,
                          final SVNURL svnUrl,
                          final SvnPersisterCore svnPersisterCore) {
        this.shutdown = shutdown;
        this.directory = directory;
        this.svnUrl = svnUrl;
        this.svnPersisterCore = svnPersisterCore;
    }

    @Override
    public void run() {
        try {
            if (!shutdown.get()) {
                svnPersisterCore.doWithClientAndRepository(new SvnPersisterCore.SvnOperation<Void>() {
                    @Override
                    public Void execute(final SVNRepository repo, final SVNClientManager clientManager) throws Exception {
                        LOGGER.info("(svn) refresh of " + directory);
                        final long startms = System.currentTimeMillis();
                        SvnProctorUtils.cleanUpWorkingDir(LOGGER, directory, svnUrl, clientManager);
                        final long elapsedms = System.currentTimeMillis() - startms;
                        LOGGER.info("(svn) refresh of " + directory + " in " + elapsedms + " ms");
                        return null;
                    }

                    @Override
                    public StoreException handleException(final Exception e) throws StoreException {
                        throw new StoreException("Unabled to svn refresh: " + directory, e);
                    }
                });
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
