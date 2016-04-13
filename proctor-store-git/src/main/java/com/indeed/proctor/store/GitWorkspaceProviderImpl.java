package com.indeed.proctor.store;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import com.google.common.base.Preconditions;

public class GitWorkspaceProviderImpl implements GitWorkspaceProvider {
    private static final Logger LOGGER = Logger.getLogger(GitWorkspaceProviderImpl.class);

    /**
     * The root directory into which all workspaces are created
     */
    private final File rootDirectory;

    public GitWorkspaceProviderImpl(final File rootDirectory) {
        this.rootDirectory = Preconditions.checkNotNull(rootDirectory, "Root Directory cannot be null");
        Preconditions.checkArgument(rootDirectory.isDirectory(), "File %s should be a directory", rootDirectory.getAbsolutePath());
        Preconditions.checkArgument(rootDirectory.exists(), "File %s should exist", rootDirectory.getAbsolutePath());
    }

    @Override
    public File getRootDirectory() {
        return rootDirectory;
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