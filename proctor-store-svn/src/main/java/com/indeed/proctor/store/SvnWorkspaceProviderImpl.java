package com.indeed.proctor.store;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author parker
 */
public class SvnWorkspaceProviderImpl extends TimerTask implements SvnWorkspaceProvider, Closeable {
    private static final Logger LOGGER = Logger.getLogger(SvnWorkspaceProviderImpl.class);

    private static final String DEFAULT_PREFIX = "svn";

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

    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public SvnWorkspaceProviderImpl(final File rootDirectory, long cleanupAgeMillis) {
        this(rootDirectory, DEFAULT_PREFIX, cleanupAgeMillis);
    }

    public SvnWorkspaceProviderImpl(final File rootDirectory,
                                    final String prefix,
                                    long cleanupAgeMillis) {
        this.cleanupAgeMillis = cleanupAgeMillis;
        this.rootDirectory = Preconditions.checkNotNull(rootDirectory, "Root Directory cannot be null");
        this.prefix = prefix;
        Preconditions.checkArgument(cleanupAgeMillis > 0, "cleanup age millis (%s) should be greater than zero", cleanupAgeMillis);
        Preconditions.checkArgument(rootDirectory.isDirectory(), "File %s should be a directory", rootDirectory.getAbsolutePath());
        Preconditions.checkArgument(rootDirectory.exists(), "File %s should exists", rootDirectory.getAbsolutePath());
        Preconditions.checkArgument(StringUtils.isNotBlank(prefix), "Prefix should not be empty");
    }

    @Override
    public File createWorkspace(String suffix, boolean deleteIfExists) throws IOException {
        checkShutdownState();
        return createWorkspace(prefix, suffix, rootDirectory, deleteIfExists);
    }

    @Override
    public boolean deleteWorkspaceQuietly(final String suffix) {
        checkShutdownState();
        final File dir = getUserDirectory(prefix, suffix, rootDirectory);
        if (dir.exists()) {
            return deleteUserDirectoryQuietly(suffix, dir);
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Workspace not found for " + suffix);
            }
            return true;
        }
    }

    @Override
    public void run() {
        try {
            if (!shutdown.get()) {
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
            } else {
                LOGGER.info("Currently shutdown, skipping older-than directory cleanup");
            }
        } catch (Exception e) {
            LOGGER.error("Unhandled Exception during directory cleanup", e);
        }
    }

    // backwards compatible with Terminable interface
    public void shutdown() {
        close();
    }

    @Override
    public void close() {
        if (shutdown.compareAndSet(false, true)) {
            LOGGER.info("[shutdown] started: deleting all working directories in " + rootDirectory);
            final long start = System.currentTimeMillis();
            // Delete any directories in the root directory that start with prefix
            deleteUserDirectories(rootDirectory, FileFilterUtils.makeDirectoryOnly(FileFilterUtils.prefixFileFilter(prefix)));
            LOGGER.info("[shutdown] complete: deleted working directories in " + (System.currentTimeMillis() - start) + " ms");
        }
    }

    private void checkShutdownState() {
        if (shutdown.get()) {
            throw new IllegalStateException(this.getClass().getSimpleName() + " is shutdown");
        }
    }

    /**
     * Creates a directory with a given suffix and prefix in the parent directory.
     * If the absolute path exists and is not a directory, throws IOException
     * If deleteIfExists is true and directory exists, its contents will be
     * deleted prior to returning.
     * Otherwise, a new directory will be created: throws IOException if fails to create a new directory
     * @param suffix
     * @param parent
     * @param deleteIfExists
     * @return
     */
    private static File createWorkspace(final String prefix,
                                        final String suffix,
                                        final File parent,
                                        final boolean deleteIfExists) throws IOException {
        final File dir = getUserDirectory(prefix, suffix, parent);
        if (dir.exists()) {
            if (!dir.isDirectory()) {
                throw new IOException("File " + dir + " exists but is not a directory");
            }
            if (deleteIfExists) {
                FileUtils.cleanDirectory(dir);
            }
        } else {
            if (!dir.mkdir()) {
                throw new IOException("Failed to create directory: " + dir);
            }
        }
        return dir;
    }

    /**
     * Returns a File object whose path is the expected user directory.
     * Does not create or check for existence.
     * @param prefix
     * @param suffix
     * @param parent
     * @return
     */
    private static File getUserDirectory(final String prefix, final String suffix, final File parent) {
        final String dirname = formatDirName(prefix, suffix);
        return new File(parent, dirname);
    }

    private static final CharMatcher VALID_SUFFIX_CHARS = CharMatcher.inRange('a', 'z').or(CharMatcher.inRange('A', 'Z')).or(CharMatcher.inRange('0', '9')).precomputed();
    /**
     * Returns the expected name of a workspace for a given suffix
     * @param suffix
     * @return
     */
    private static String formatDirName(final String prefix, final String suffix) {
        // Replace all invalid characters with '-'
        final CharMatcher invalidCharacters = VALID_SUFFIX_CHARS.negate();
        return String.format("%s-%s", prefix, invalidCharacters.trimAndCollapseFrom(suffix.toLowerCase(), '-'));
    }

    private static boolean deleteUserDirectoryQuietly(final String user, final File directory) {
        LOGGER.info("Deleting user directory " + directory + " for user: " + user);
        final boolean success = FileUtils.deleteQuietly(directory);
        if (!success) {
            LOGGER.error("Failed to delete user directory " + directory + " for user: " + user);
        }
        return success;
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

    static final FileFilter olderThanFileFilter(final long cleanUpAgeMillis) {
        return new FileFilter() {
            @Override
            public boolean accept(File file) {
                return (System.currentTimeMillis() - file.lastModified()) > cleanUpAgeMillis;
            }
        };
    }
}
