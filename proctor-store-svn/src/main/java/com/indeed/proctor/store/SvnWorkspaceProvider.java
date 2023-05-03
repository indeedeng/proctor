package com.indeed.proctor.store;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * @author parker
 */
public interface SvnWorkspaceProvider extends Closeable {
    /**
     * Creates or reuses user-directory with a given prefix
     * @param suffix A unique suffix identifying the workspace
     * @param deleteIfExists If the user workspace exists, delete its contents
     * before returning the directory
     * @return
     * @throws IOException
     */
    File createWorkspace(String suffix, boolean deleteIfExists) throws IOException;

    /**
     * Delete the workspace directory.
     * @param suffix
     * @return A flag whose value indicates if the directory removal was
     * successful
     */
    boolean deleteWorkspaceQuietly(String suffix);

}
