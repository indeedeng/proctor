package com.indeed.proctor.store;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonProcessingException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class GitPersisterCoreImpl implements GitPersisterCore, Closeable {
    private static final Logger LOGGER = Logger.getLogger(GitPersisterCoreImpl.class);

    private final String gitUrl;
    private final Repository repo;
    private final File tempDir;
/*
    public GitPersisterCoreImpl(final String gitUrl, final String username, final String password, final File tempDir) {
        this(gitUrl, username, password, tempDir);
    }
*/
    /**
     * @param gitUrl
     * @param username
     * @param password
     * @param
     */
    public GitPersisterCoreImpl(final String gitUrl, final String username, final String password, final File tempDir) {
        String localPath = "/Users/jcheng/Documents/git/abcabctest"; // change to tempDir
        //this.gitUrl = gitUrl;
        this.tempDir = tempDir;
        this.gitUrl = "http://github.wvrgroup.internal/jcheng/git-proctor-test-definitions.git";

        UsernamePasswordCredentialsProvider user = new UsernamePasswordCredentialsProvider(username, password);
        Git git = null;
        try {
            git = Git.open(new File(localPath));
        } catch (IOException e) {
            //System.err.println("Could not open directory at " + localPath, e);
            e.printStackTrace();
        }
        this.repo = git.getRepository();
        // CLONING A GITHUB REPO
        /*
        CloneCommand clone = git.cloneRepository();
        clone.setBare(false);
        clone.setCloneAllBranches(true);
        clone.setDirectory(new File(localPath)).setURI(gitUrl);
        clone.setCredentialsProvider(user);
        git = clone.call();
        */

        try {
            System.out.println(git.branchList().call());
            for (Ref ref : git.branchList().call()) {
                System.out.println(ref.getName());
                //request.setAttribute("bob", ref.getName());//doesn't work
            }
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

    }

    @Override
    public Repository getRepo() {
        return repo;
    }

    @Override
    public String getGitUrl() {
        return gitUrl;
    }

    @Override
    public boolean cleanUserWorkspace(String username) {
        return false;
    }

    @Override
    public <C> C getFileContents(Class<C> c, String[] path, C defaultValue, long revision)
            throws StoreException.ReadException, JsonProcessingException {
        return null;
    }

    @Override
    public void doInWorkingDirectory(String username, String password, String comment, long previousVersion,
            FileBasedProctorStore.ProctorUpdater updater) throws StoreException.TestUpdateException {

    }

    @Override
    public FileBasedProctorStore.TestVersionResult determineVersions(long fetchRevision)
            throws StoreException.ReadException {
        return null;
    }

    @Override
    public void close() throws IOException {

    }
}