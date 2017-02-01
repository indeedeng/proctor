package com.indeed.proctor.store;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GitWorkspaceProviderImpl implements GitWorkspaceProvider {
    private static final Logger LOGGER = Logger.getLogger(GitWorkspaceProviderImpl.class);
    private static final int DEFAULT_LOCK_TIMEOUT_SECONDS = 90;
    /**
     * The root directory into which all workspaces are created
     */
    private final File rootDirectory;
    private final Lock directoryLock;
    private final int lockTimeoutSeconds;

    public GitWorkspaceProviderImpl(final File rootDirectory) {
        this(rootDirectory, DEFAULT_LOCK_TIMEOUT_SECONDS);
    }

    public GitWorkspaceProviderImpl(final File rootDirectory, final int lockTimeoutSeconds) {
        this.rootDirectory = Preconditions.checkNotNull(rootDirectory, "Root Directory cannot be null");
        directoryLock = new ReentrantLock();
        Preconditions.checkArgument(rootDirectory.isDirectory(), "File %s should be a directory", rootDirectory.getAbsolutePath());
        Preconditions.checkArgument(rootDirectory.exists(), "File %s should exist", rootDirectory.getAbsolutePath());
        this.lockTimeoutSeconds = lockTimeoutSeconds;
    }

    @Override
    public File getRootDirectory() {
        return rootDirectory;
    }

    @Override
    public <T> T synchronizedOperation(final Callable<T> callable) {
        try {
            if (directoryLock.tryLock(lockTimeoutSeconds, TimeUnit.SECONDS)) {
                try {
                    return callable.call();
                } catch (final Exception e) {
                    throw Throwables.propagate(e);
                } finally {
                    directoryLock.unlock();
                }
            } else {
                throw Throwables.propagate(new StoreException("Attempt to acquire lock on working directory was timeout: " + lockTimeoutSeconds + "s. Maybe due to dead lock"));
            }
        } catch (final InterruptedException e) {
            LOGGER.error("Thread interrupted. ", e);
        }
        return null;
    }

    public boolean cleanWorkingDirectory() {
        synchronizedOperation(new Callable<Void>() {
            @Override
            public Void call() {
                try {
                    FileUtils.cleanDirectory(rootDirectory);
                } catch (final IOException e) {
                    LOGGER.error("Unable to clean working directory", e);
                }
                return null;
            }
        });
        return true;
    }
}