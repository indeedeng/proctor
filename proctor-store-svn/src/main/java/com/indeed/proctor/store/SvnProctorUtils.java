package com.indeed.proctor.store;

import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import org.apache.logging.log4j.Logger;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNCommitClient;
import org.tmatesoft.svn.core.wc.SVNCommitItem;
import org.tmatesoft.svn.core.wc.SVNCommitPacket;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusClient;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.io.File;
import java.io.IOException;

/**
 * @author parker
 */
class SvnProctorUtils {
    private SvnProctorUtils() { throw new UnsupportedOperationException("SvnProctorUtils is a utils class"); }

    /**
     * Cleans up the directory using the client manager.
     * <p/>
     * If the directory not a working directory or is some state that is not "NORMAL",
     * a full checkout will be performed from HEAD.
     * <p/>
     * Otherwise, an update will be attempted
     *
     * @param userDir
     * @param userClientManager
     * @throws org.tmatesoft.svn.core.SVNException
     * @throws java.io.IOException
     */
    static void cleanUpWorkingDir(final Logger logger,
                                  final File userDir,
                                  final SVNURL svnUrl,
                                  final SVNClientManager userClientManager) throws SVNException, IOException {
        Preconditions.checkNotNull(userDir, "user dir should not be null");
        if (userDir.exists()) {
            Preconditions.checkArgument(userDir.isDirectory(), "user dir (%s) should be a directory if it exists", userDir.getAbsolutePath());
            Files.touch(userDir);
        } else {
            if (!userDir.mkdir()) {
                throw new IOException("Could not create directory " + userDir);
            }
        }

        final SVNUpdateClient updateClient = userClientManager.getUpdateClient();
        if (SVNWCUtil.isVersionedDirectory(userDir)) {
            final SVNStatusClient statusClient = userClientManager.getStatusClient();
            final SVNStatus status = statusClient.doStatus(userDir, false);
            final SVNStatusType contentStatus = status.getContentsStatus();
            logger.info("(svn) status for " + userDir + " is " + contentStatus);
            if (contentStatus == SVNStatusType.STATUS_NORMAL) {
                long elapsed = -System.currentTimeMillis();
                if (logger.isDebugEnabled()) {
                    logger.debug("(svn) current r" + status.getRevision().getNumber() +  " svn update " + svnUrl + " into " + userDir);
                }
                long workingDirRevision = updateClient.doUpdate(userDir, SVNRevision.HEAD, SVNDepth.INFINITY, false, false);
                elapsed += System.currentTimeMillis();
                logger.info(String.format("Updated working directory (%s) to revision %d in %d ms", userDir.getAbsolutePath(), workingDirRevision, elapsed));
            } else {
                logger.warn(String.format("Working directory (%s) is in a bad state: %s Cleaning up and checking out fresh.", userDir.getAbsolutePath(), contentStatus));
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Deleting working directory contents (%s)", userDir.getAbsolutePath()));
                }
                deleteDirectoryContents(userDir);
                long elapsed = -System.currentTimeMillis();
                if (logger.isDebugEnabled()) {
                    logger.debug("(svn) svn co " + svnUrl + " into " + userDir);
                }
                long workingDirRevision = updateClient.doCheckout(svnUrl, userDir, null, SVNRevision.HEAD, SVNDepth.INFINITY, false);
                elapsed += System.currentTimeMillis();
                logger.info(String.format("Checked out working directory (%s) to revision %d in %d ms", userDir.getAbsolutePath(), workingDirRevision, elapsed));
            }
        } else {
            long elapsed = -System.currentTimeMillis();
            if (logger.isDebugEnabled()) {
                logger.debug("(svn) svn co " + svnUrl + " into " + userDir);
            }
            long workingDirRevision = updateClient.doCheckout(svnUrl, userDir, null, SVNRevision.HEAD, SVNDepth.INFINITY, false);
            elapsed += System.currentTimeMillis();
            logger.info(String.format("Checked out working directory (%s) to revision %d in %d ms", userDir.getAbsolutePath(), workingDirRevision, elapsed));
        }
    }


    static void doInWorkingDirectory(
        final Logger logger,
        final File userDir,
        final String username,
        final String password,
        final SVNURL svnUrl,
        final FileBasedProctorStore.ProctorUpdater updater,
        final String comment) throws IOException, SVNException, Exception {
        final BasicAuthenticationManager authManager = new BasicAuthenticationManager(username, password);
        final SVNClientManager userClientManager = SVNClientManager.newInstance(null, authManager);
        final SVNWCClient wcClient = userClientManager.getWCClient();

        try {
            // Clean up the UserDir
            SvnProctorUtils.cleanUpWorkingDir(logger, userDir, svnUrl, userClientManager);

            /*
                if (previousVersion != 0) {
                    final Collection<?> changesSinceGivenVersion = repo.log(new String[] { "" }, null, previousVersion, -1, false, false);
                    if (! changesSinceGivenVersion.isEmpty()) {
                        //  TODO: the baseline version is out of date, so need to go back to the user
                    }
                }
                updateClient.doCheckout(checkoutUrl, workingDir, null, SVNRevision.HEAD, SVNDepth.INFINITY, false);
            */

            final FileBasedProctorStore.RcsClient rcsClient = new SvnPersisterCoreImpl.SvnRcsClient(wcClient);
            final boolean thingsChanged = updater.doInWorkingDirectory(rcsClient, userDir);

            if (thingsChanged) {
                final SVNCommitClient commitClient = userClientManager.getCommitClient();
                final SVNCommitPacket commit = commitClient.doCollectCommitItems(new File[]{userDir}, false, false, SVNDepth.INFINITY, new String[0]);
                long elapsed = -System.currentTimeMillis();
                final SVNCommitInfo info = commitClient.doCommit(commit, /* keepLocks */ false, comment);
                elapsed += System.currentTimeMillis();
                if (logger.isDebugEnabled()) {
                    final StringBuilder changes = new StringBuilder("Committed " + commit.getCommitItems().length + " changes: ");
                    for (final SVNCommitItem item : commit.getCommitItems()) {
                        changes.append(item.getKind() + " - " + item.getPath() + ", ");
                    }
                    changes.append(String.format(" in %d ms new revision: r%d", elapsed, info.getNewRevision()));
                    logger.debug(changes.toString());
                }
            }
        } finally {
            userClientManager.dispose();
        }
    }


    /**
     * Cloned from guava's com.google.common.io.Files in r9.  It was later erased completely.
     * <p/>
     * Deletes all the files within a directory. Does not delete the
     * directory itself.
     * <p/>
     * <p>If the file argument is a symbolic link or there is a symbolic
     * link in the path leading to the directory, this method will do
     * nothing. Symbolic links within the directory are not followed.
     *
     * @param directory the directory to delete the contents of
     * @throws IllegalArgumentException if the argument is not a directory
     * @throws IOException if an I/O error occurs
     * @see #deleteRecursively
     */
    private static void deleteDirectoryContents(File directory)
        throws IOException {
        Preconditions.checkArgument(directory.isDirectory(),
                                    "Not a directory: %s", directory);
        // Symbolic links will have different canonical and absolute paths
        if (!directory.getCanonicalPath().equals(directory.getAbsolutePath())) {
            return;
        }
        File[] files = directory.listFiles();
        if (files == null) {
            throw new IOException("Error listing files for " + directory);
        }
        for (File file : files) {
            deleteRecursively(file);
        }
    }

    /**
     * Cloned from guava's com.google.common.io.Files in r9.  It was later erased completely.
     * <p/>
     * Deletes a file or directory and all contents recursively.
     * <p/>
     * <p>If the file argument is a symbolic link the link will be deleted
     * but not the target of the link. If the argument is a directory,
     * symbolic links within the directory will not be followed.
     *
     * @param file the file to delete
     * @throws IOException if an I/O error occurs
     * @see #deleteDirectoryContents
     */
    private static void deleteRecursively(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectoryContents(file);
        }
        if (!file.delete()) {
            throw new IOException("Failed to delete " + file);
        }
    }

}
