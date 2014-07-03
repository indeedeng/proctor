package com.indeed.proctor.store;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.indeed.proctor.common.Serializers;

public class GitProctorCore implements FileBasedPersisterCore {
    private static final Logger LOGGER = Logger.getLogger(GitProctorCore.class);
    private static final String DIRECTORY_IDENTIFIER = ".git";

    private Git git; //TODO should be final
    private final String gitUrl;
    private final Repository repo;
    private final File tempDir;

    /**
     * @param gitUrl
     * @param username
     * @param password
     * @param tempDir
     * @param
     */
    public GitProctorCore(final String gitUrl, final String username, final String password, final File tempDir) {
        /*
         * Eventually, make a temp dir, clone the github repo, then delete temp dir when done
         */
        String localPath = "/Users/jcheng/Documents/git/abcabctest"; // change to tempDir
        //File localPath = tempDir;
        //this.gitUrl = gitUrl;
        this.tempDir = tempDir;
        this.gitUrl = "http://github.wvrgroup.internal/jcheng/git-proctor-test-definitions.git";

        UsernamePasswordCredentialsProvider user = new UsernamePasswordCredentialsProvider(username, password);
        //Git git = null;
        try {
            git = Git.open(new File(localPath));
        } catch (IOException e) {
            //System.err.println("Could not open directory at " + localPath, e);
            System.out.println("Error when opening git\n\n");
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
            }

            /***********
             * try and read a file from github
             ***********/
            // find the HEAD
            ObjectId lastCommitId = repo.resolve(Constants.HEAD);

            // a RevWalk allows to walk over commits based on some filtering that is defined
            RevWalk revWalk = new RevWalk(repo);
            RevCommit commit = revWalk.parseCommit(lastCommitId);
            // and using commit's tree find the path
            RevTree tree = commit.getTree();
            System.out.println("Having tree: " + tree);

            // now try to find a specific file
            TreeWalk treeWalk = new TreeWalk(repo);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(PathFilter.create("README.md"));
            if (!treeWalk.next()) {
                throw new IllegalStateException("Did not find expected file 'README.md'");
            }

            ObjectId objectId = treeWalk.getObjectId(0);
            ObjectLoader loader = repo.open(objectId);

            // and then one can the loader to read the file
            loader.copyTo(System.out);

            repo.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public <C> C getFileContents(final Class<C> c,
                                 final java.lang.String[] path,
                                 final C defaultValue,
                                 final String revision) throws StoreException.ReadException, JsonProcessingException {
        try {
            if (!ObjectId.isId(revision)) {
                throw new StoreException.ReadException("Malformed id " + revision);
            }
            final ObjectId blobOrCommitId = ObjectId.fromString(revision);

            final ObjectLoader loader = git.getRepository().open(blobOrCommitId);

            if (loader.getType() == Constants.OBJ_COMMIT) {
                // look up the file at this revision
                final RevCommit commit = RevCommit.parse(loader.getCachedBytes());

                final TreeWalk treeWalk2 = new TreeWalk(git.getRepository());
                treeWalk2.addTree(commit.getTree());
                treeWalk2.setRecursive(true);
                final String joinedPath = "matrices" + "/" + Joiner.on("/").join(path);
                System.out.println("getFileContents joinedPath var - " + joinedPath);
                treeWalk2.setFilter(PathFilter.create(joinedPath));

                if (!treeWalk2.next()) {
                    throw new StoreException.ReadException("Did not find expected file '" + joinedPath + "'");
                }
                final ObjectId blobId = treeWalk2.getObjectId(0);
                return getFileContents(c, blobId);
            } else if (loader.getType() == Constants.OBJ_BLOB) {
                return getFileContents(c, blobOrCommitId);
            } else {
                throw new StoreException.ReadException("Invalid Object Type " + loader.getType() + " for id " + revision);
            }
        } catch (IOException e) {
            throw new StoreException.ReadException(e);
        }
    }

    private <C> C getFileContents(final Class<C> c,
                                  final ObjectId blobId) throws IOException {
        ObjectLoader loader = git.getRepository().open(blobId);
        final ObjectMapper mapper = Serializers.lenient();
        return mapper.readValue(loader.getBytes(), c);
    }

    static class GitRcsClient implements FileBasedProctorStore.RcsClient {
        private final Git git;

        public GitRcsClient(Git git) {
            this.git = git;
        }

        @Override
        public void add(File file) throws Exception {
            git.add().addFilepattern(".").call(); //TODO
        }

        @Override
        public void delete(File testDefinitionDirectory) throws Exception {
            //TODO
        }
    }

    @Override
    public void doInWorkingDirectory(String username, String password, String comment, String previousVersion,
            FileBasedProctorStore.ProctorUpdater updater) throws StoreException.TestUpdateException {
        System.out.println("tempDir name: " + tempDir.getName());
        System.out.println("tempDir path: " + tempDir.getAbsolutePath());
        System.out.println("username: " + username);
        System.out.println("comment: " + comment);
        String localPath = "/Users/jcheng/Documents/git/abcabctest"; // TODO shouldn't be hardcoded here
        String gitURI = "http://github.wvrgroup.internal/jcheng/git-proctor-test-definitions.git"; //TODO shouldn't be hardcoded here

        UsernamePasswordCredentialsProvider user = new UsernamePasswordCredentialsProvider(username, password);
        Git git = null; // TODO fix try catch structure here
        try {
            git = Git.open(new File(localPath));
            System.out.println(git.branchList().call());
            for (Ref ref : git.branchList().call()) {
                System.out.println(ref.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            final FileBasedProctorStore.RcsClient rcsClient = new GitProctorCore.GitRcsClient(git);
            System.out.println(git.getRepository().getDirectory().getAbsolutePath());
            System.out.println(git.getRepository().getDirectory().getName());
            final boolean thingsChanged = updater.doInWorkingDirectory(rcsClient, new File(localPath));

            if (thingsChanged) {
                System.out.println("things changed! now on to committing and pushing all files"); //TODO
                rcsClient.add(null);
                git.commit().setMessage(comment).call();
                git.push().setCredentialsProvider(user).call();

            }
        } catch (Exception e) {
            System.out.println("error while messing with rcsClient " + e);
        }

        // CLONING A GITHUB REPO
            /*
            CloneCommand clone = git.cloneRepository();
            clone.setBare(false);
            clone.setCloneAllBranches(true);
            clone.setDirectory(new File(localPath)).setURI(gitURI);
            clone.setCredentialsProvider(user);
            git = clone.call();
            */

    }
/*
    @Override
    public FileBasedProctorStore.TestVersionResult determineVersions(long fetchRevision)
            throws StoreException.ReadException {
        return new FileBasedProctorStore.TestVersionResult(
                Collections.<FileBasedProctorStore.TestVersionResult.Test>emptyList(),
                new Date(),
                "dummyAuthor",
                0,
                "dummydesccommitmessage"
        ); // TODO
    }
*/

    @Override
    public TestVersionResult determineVersions(final String fetchRevision) throws StoreException.ReadException {
        try {
            System.out.println("determineVersions start");
            final RevWalk walk = new RevWalk(git.getRepository());
            final ObjectId commitId = ObjectId.fromString(fetchRevision);
            final RevCommit headTree = walk.parseCommit(commitId);
            final RevTree tree = headTree.getTree();
            System.out.println("Having tree: " + tree);


            // now use a TreeWalk to iterate over all files in the Tree recursively
            // you can set Filters to narrow down the results if needed
            TreeWalk treeWalk = new TreeWalk(git.getRepository());
            treeWalk.addTree(tree);
            treeWalk.setFilter(AndTreeFilter
                    .create(PathFilter.create("test-definitions"), PathSuffixFilter.create("definition.json")));
            treeWalk.setRecursive(true);

            final List<TestVersionResult.Test> tests = Lists.newArrayList();
            while (treeWalk.next()) {
                final ObjectId id = treeWalk.getObjectId(0);
                // final RevTree revTree = walk.lookupTree(id);

                final String path = treeWalk.getPathString();
                System.out.println("determineVersions path var - " + path);
                final String[] pieces = path.split("/");
                final String testname = pieces[pieces.length - 2]; // tree / parent directory name
                System.out.println("determineVersions testname var - " + testname);

                // testname, blobid pair
                // note this is the blobid hash - not a commit hash
                // RevTree.id and RevBlob.id
                tests.add(new TestVersionResult.Test(testname, id.name()));
            }

            walk.dispose();
            return new TestVersionResult(
                tests,
                new Date(headTree.getCommitTime()),
                headTree.getAuthorIdent().toExternalString(),
                headTree.toObjectId().getName(),
                headTree.getFullMessage()
            );
        } catch (IOException e) {
            throw new StoreException.ReadException(e);
        }
    }

    Git getGit() {
        return git;
    }

    String getRefName() {
        return Constants.HEAD;
        //return refName;
    }

    @Override
    public void close() throws IOException {
        // Is this ThreadSafe ?
        git.getRepository().close();
    }


    @Override
    public String getAddTestRevision() {
        return ObjectId.zeroId().name();
    }
}