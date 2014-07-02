package com.indeed.proctor.store;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.indeed.proctor.common.model.TestMatrixVersion;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class GitProctor extends FileBasedProctorStore {
    private static final Logger LOGGER = Logger.getLogger(GitProctor.class);

    /* Storage Schema:
        ${gitPath}/
            test-definitions/
                test-name-one/
                    definition.json
                    metadata.json
                test-name-two/
                    definition.json
                    metadata.json
    */

    private final Repository repo;
    private final String gitUrl;

    public GitProctor(final String gitPath,
                      final String username,
                      final String password) throws IOException {
        this(new GitPersisterCoreImpl(gitPath, username, password, Files.createTempDir()));
    }

    public GitProctor(final GitPersisterCore core) {
        super(core);
        this.repo = core.getRepo();
        this.gitUrl = core.getGitUrl();
    }
    
    /*
    public static void main(String args[]) throws IOException {
        final String gitUrl = System.console().readLine("git url: ");
        final String gituser = System.console().readLine("user: ");
        final String password = new String(System.console().readPassword("password: "));
        
        final File tempDir = Files.createTempDir();
        try {
            final GitProctor client = new GitProctor(gitUrl, gituser, password);

            System.out.println("Running load matrix for last " + num_revisions + " revisions");
            final long start = System.currentTimeMillis();
            final List<Revision> revisions = client.getMatrixHistory(0, num_revisions);
            for(final Revision rev : revisions) {
                final TestMatrixVersion matrix = client.getTestMatrix(rev.getRevision());
            }
            final long elapsed = System.currentTimeMillis() - start;
            System.out.println("Finished reading matrix history (" + revisions.size() + ") in " + elapsed + " ms");
            client.close();
        } catch (StoreException e) {
            LOGGER.error(e);
        } finally {
            System.out.println("Deleting temp dir : " + tempDir);
            FileUtils.deleteDirectory(tempDir);
        }
    }
    */

    @Override
    public void verifySetup() throws StoreException {
        // TODO
    }

    @Override
    public boolean cleanUserWorkspace(String username) {
        return false; // TODO
    }

    @Override
    public long getLatestVersion() throws StoreException {
        return 0; // TODO
    }

    @Override
    public List<Revision> getMatrixHistory(int start, int limit) throws StoreException {
        return Collections.emptyList(); // TODO
    }

    @Override
    public List<Revision> getHistory(String test, int start, int limit) throws StoreException {
        return Collections.emptyList(); // TODO
    }

    @Override
    public List<Revision> getHistory(String test, long revision, int start, int limit) throws StoreException {
        return Collections.emptyList(); // TODO
    }
}
