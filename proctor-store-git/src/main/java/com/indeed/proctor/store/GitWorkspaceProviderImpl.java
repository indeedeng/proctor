package com.indeed.proctor.store;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.log4j.Logger;

import com.google.common.base.Preconditions;

public class GitWorkspaceProviderImpl extends TimerTask implements GitWorkspaceProvider {
    private static final Logger LOGGER = Logger.getLogger(GitWorkspaceProviderImpl.class);

    private static final String DEFAULT_PREFIX = "proctor-git";

    /**
     * The prefix used when creating the workspace directories
     */
    private final String prefix;

    /**
     * The root directory into which all workspaces are created
     */
    private final File rootDirectory;

    /**
     * The age in milliseconds for which we should
     * keep temp directories. Actively cleaning up directories whose last modified age
     * is older than the cleanup age.
     */
    final long cleanupAgeMillis;

    public GitWorkspaceProviderImpl(final File rootDirectory, long cleanupAgeMillis) {
        this(rootDirectory, DEFAULT_PREFIX, cleanupAgeMillis);
    }
    public GitWorkspaceProviderImpl(final File rootDirectory, final String prefix, long cleanupAgeMillis) {
        this.cleanupAgeMillis = cleanupAgeMillis;
        this.rootDirectory = Preconditions.checkNotNull(rootDirectory, "Root Directory cannot be null");
        this.prefix = prefix;
        Preconditions.checkArgument(cleanupAgeMillis > 0, "cleanup age millis (%s) should be greater than zero", cleanupAgeMillis);
        Preconditions.checkArgument(rootDirectory.isDirectory(), "File %s should be a directory", rootDirectory.getAbsolutePath());
        Preconditions.checkArgument(rootDirectory.exists(), "File %s should exist", rootDirectory.getAbsolutePath());
    }

    static final FileFilter olderThanFileFilter(final long cleanUpAgeMillis) {
        return new FileFilter() {
            @Override
            public boolean accept(File file) {
                return (System.currentTimeMillis() - file.lastModified()) > cleanUpAgeMillis;
            }
        };
    }

    public File getRootDirectory() {
        return rootDirectory;
    }

    @Override
    public void run() {
        try {
            LOGGER.info("Actively cleaning up directories older than " + TimeUnit.MILLISECONDS.toHours(cleanupAgeMillis) + " hours");
            final IOFileFilter olderThanFilter = FileFilterUtils.asFileFilter(olderThanFileFilter(cleanupAgeMillis));
            final IOFileFilter tempDirFilter =
                    FileFilterUtils.prefixFileFilter(prefix);

            /**
             * Delete directories that are:
             * older than [clean up age millis]
             * starts with temp-dir-prefix
             */
            final IOFileFilter deleteAfterMillisFilter = FileFilterUtils.makeDirectoryOnly(
                    FileFilterUtils.andFileFilter(olderThanFilter, tempDirFilter)
            );
            deleteUserDirectories(rootDirectory, deleteAfterMillisFilter);
        } catch (Exception e) {
            LOGGER.error("Unhandled Exception during directory cleanup", e);
        }
    }

    /**
     * Deletes all of the Directories in root that match the FileFilter
     *
     * @param root
     * @param filter
     */
    private static void deleteUserDirectories(final File root,
            final FileFilter filter) {
        final File[] dirs = root.listFiles(filter);
        LOGGER.info("Identified (" + dirs.length + ") directories to delete");
        for (final File dir : dirs) {
            LOGGER.info("Deleting " + dir);
            if (!FileUtils.deleteQuietly(dir)) {
                LOGGER.info("Failed to delete directory " + dir);
            }
        }
    }

    @Override
    public boolean deleteWorkspaceQuietly(final String user) {
        if (rootDirectory.exists()) {
            return deleteUserDirectoryQuietly(user, rootDirectory);
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Workspace not found for " + user);
            }
            return true;
        }
    }

    private static boolean deleteUserDirectoryQuietly(final String user, final File directory) {
        LOGGER.info("Deleting user directory " + directory + " for user: " + user);
        final boolean success = FileUtils.deleteQuietly(directory);
        if (!success) {
            LOGGER.error("Failed to delete user directory " + directory + " for user: " + user);
        }
        return success;
    }

    public boolean cleanWorkingDirectory() {
        try {
            FileUtils.cleanDirectory(rootDirectory);
        } catch (IOException e) {
            LOGGER.error("Unable to clean working directory", e);
            return false;
        }
        return true;
    }
}