package com.indeed.proctor.webapp.db;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.indeed.proctor.store.FileBasedProctorStore;
import com.indeed.proctor.store.GitProctor;
import com.indeed.proctor.store.GitProctorCore;
import com.indeed.proctor.store.GitWorkspaceProviderImpl;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.cache.ProctorStoreCaching;
import com.indeed.util.varexport.VarExporter;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

public class GitProctorStoreFactory implements StoreFactory {
    private static final Logger LOGGER = Logger.getLogger(GitProctorStoreFactory.class);

    private String gitUrl;
    private String gitUsername;
    private String gitPassword;
    private String testDefinitionsDirectory = FileBasedProctorStore.DEFAULT_TEST_DEFINITIONS_DIRECTORY;

    /* The root directory into which we should put the "qa-matrices" or "trunk-matrices"
     * If not set - a the temp directory will be used
     * */
    private final File tempRoot;

    /**
     * @deprecated executor and gitRefreshSecond are no longer required. Use other constructors instead.
     */
    @Deprecated
    public GitProctorStoreFactory(final ScheduledExecutorService executor,
                                  final long gitRefreshSeconds,
                                  final String gitUrl,
                                  final String gitUsername,
                                  final String gitPassword,
                                  final String testDefinitionsDirectory,
                                  final String tempRootDirectory) throws IOException, ConfigurationException {
        this.gitUrl = gitUrl;
        this.gitUsername = gitUsername;
        this.gitPassword = gitPassword;
        this.testDefinitionsDirectory = testDefinitionsDirectory;

        if (StringUtils.isEmpty(tempRootDirectory)) {
            tempRoot = identifyImplicitTempRoot();
        } else {
            tempRoot = new File(tempRootDirectory);
        }
    }

    public GitProctorStoreFactory(final String gitUrl,
                                  final String gitUsername,
                                  final String gitPassword,
                                  final String testDefinitionsDirectory,
                                  final String tempRootDirectory) throws IOException, ConfigurationException {
        this.gitUrl = gitUrl;
        this.gitUsername = gitUsername;
        this.gitPassword = gitPassword;
        this.testDefinitionsDirectory = testDefinitionsDirectory;

        if (StringUtils.isEmpty(tempRootDirectory)) {
            tempRoot = identifyImplicitTempRoot();
        } else {
            tempRoot = new File(tempRootDirectory);
        }
    }

    public ProctorStore getTrunkStore() {
        return createStore("proctor/git/trunk");
    }

    public ProctorStore getQaStore() {
        return createStore("proctor/git/qa");
    }

    public ProctorStore getProductionStore() {
        return createStore("proctor/git/production");
    }

    public ProctorStore createStore(final String relativePath) {

        final File tempDirectory = createTempDirectoryForPath(relativePath);

        Preconditions.checkArgument(!CharMatcher.WHITESPACE.matchesAllOf(Strings.nullToEmpty(gitUrl)), "scm.path property cannot be empty");

        final GitWorkspaceProviderImpl provider = new GitWorkspaceProviderImpl(tempDirectory);
        final GitProctorCore gitCore = new GitProctorCore(gitUrl, gitUsername, gitPassword, testDefinitionsDirectory, provider);

        final String branchName = relativePath.substring(relativePath.lastIndexOf("/")+1);
        final GitProctor store = new GitProctor(gitCore, testDefinitionsDirectory, branchName);
        final String prefix = relativePath.replace('/', '-');
        final VarExporter exporter = VarExporter.forNamespace(GitProctor.class.getSimpleName()).includeInGlobal();
        exporter.export(store, prefix + "-");
        return new ProctorStoreCaching(store);
    }

    private File createTempDirectoryForPath(final String relativePath) {
        // replace "/" with "-" omit first "/" but omitEmptyStrings
        final String dirName = CharMatcher.is(File.separatorChar).trimAndCollapseFrom(relativePath, '-');
        final File temp = new File(tempRoot, dirName);
        if(temp.exists()) {
           if(!temp.isDirectory()) {
               throw new IllegalStateException(temp + " exists but is not a directory");
           }
        } else {
            if(!temp.mkdirs()) {
                throw new IllegalStateException("Could not create directory : " + temp);
            }
        }
        return temp;
    }

    /**
     * Identify the root-directory for TempFiles
     * @return
     */
    private File identifyImplicitTempRoot() throws IOException {
        final File tempFile = File.createTempFile("implicit", GitProctorStoreFactory.class.getSimpleName());

        tempFile.delete();
        return tempFile.getParentFile();
    }
}
