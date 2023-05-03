package com.indeed.proctor.webapp.db;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.indeed.proctor.store.FileBasedProctorStore;
import com.indeed.proctor.store.GitProctor;
import com.indeed.proctor.store.GitProctorCore;
import com.indeed.proctor.store.GitWorkspaceProviderImpl;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.cache.CachingProctorStore;
import com.indeed.proctor.store.cache.GlobalCachingProctorStore;
import com.indeed.proctor.webapp.extensions.GlobalCacheStore;
import com.indeed.util.varexport.VarExporter;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

public class GitProctorStoreFactory implements TrunkQaProdStoresFactory {
    private static final Logger LOGGER = LogManager.getLogger(GitProctorStoreFactory.class);

    private String gitUrl;
    private String gitUsername;
    private String gitPassword;
    private String testDefinitionsDirectory = FileBasedProctorStore.DEFAULT_TEST_DEFINITIONS_DIRECTORY;

    /* The root directory into which we should put the "qa-matrices" or "trunk-matrices"
     * If not set - a the temp directory will be used
     * */
    private final File tempRoot;

    private final int gitDirectoryLockTimeoutSeconds;
    private final int gitPullPushTimeoutSeconds;
    private final int gitCloneTimeoutSeconds;
    private final boolean gitCleanInitialization;

    @Nullable
    private final GlobalCacheStore globalCacheStore;

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
                                  final String tempRootDirectory,
                                  final int gitDirectoryLockTimeoutSeconds,
                                  final int gitPullPushTimeoutSeconds,
                                  final int gitCloneTimeoutSeconds,
                                  final boolean gitCleanInitialization) throws IOException {
        this(
                gitUrl,
                gitUsername,
                gitPassword,
                testDefinitionsDirectory,
                tempRootDirectory,
                gitDirectoryLockTimeoutSeconds,
                gitPullPushTimeoutSeconds,
                gitCloneTimeoutSeconds,
                gitCleanInitialization,
                null
        );
    }

    public GitProctorStoreFactory(final String gitUrl,
                                  final String gitUsername,
                                  final String gitPassword,
                                  final String testDefinitionsDirectory,
                                  final String tempRootDirectory,
                                  final int gitDirectoryLockTimeoutSeconds,
                                  final int gitPullPushTimeoutSeconds,
                                  final int gitCloneTimeoutSeconds,
                                  final boolean gitCleanInitialization
    ) throws IOException {
        this(
                gitUrl,
                gitUsername,
                gitPassword,
                testDefinitionsDirectory,
                tempRootDirectory,
                gitDirectoryLockTimeoutSeconds,
                gitPullPushTimeoutSeconds,
                gitCloneTimeoutSeconds,
                gitCleanInitialization,
                null
        );
    }

    public GitProctorStoreFactory(final String gitUrl,
                                  final String gitUsername,
                                  final String gitPassword,
                                  final String testDefinitionsDirectory,
                                  final String tempRootDirectory,
                                  final int gitDirectoryLockTimeoutSeconds,
                                  final int gitPullPushTimeoutSeconds,
                                  final int gitCloneTimeoutSeconds,
                                  final boolean gitCleanInitialization,
                                  final GlobalCacheStore globalCacheStore
    ) throws IOException {
        this.gitUrl = gitUrl;
        this.gitUsername = gitUsername;
        this.gitPassword = gitPassword;
        this.testDefinitionsDirectory = testDefinitionsDirectory;

        if (StringUtils.isEmpty(tempRootDirectory)) {
            tempRoot = identifyImplicitTempRoot();
        } else {
            tempRoot = new File(tempRootDirectory);
        }

        this.gitDirectoryLockTimeoutSeconds = gitDirectoryLockTimeoutSeconds;
        this.gitPullPushTimeoutSeconds = gitPullPushTimeoutSeconds;
        this.gitCloneTimeoutSeconds = gitCloneTimeoutSeconds;
        this.gitCleanInitialization = gitCleanInitialization;
        this.globalCacheStore = globalCacheStore;
    }

    // Build ProctorStore which does initial proctor data downloading synchronously in constructor
    public ProctorStore getTrunkStore() {
        return createStore("proctor/git/trunk");
    }

    // Build ProctorStore which does initial proctor data downloading synchronously in constructor
    public ProctorStore getQaStore() {
        return createStore("proctor/git/qa");
    }

    // Build ProctorStore which does initial proctor data downloading synchronously in constructor
    public ProctorStore getProductionStore() {
        return createStore("proctor/git/production");
    }

    public ProctorStore createStore(final String relativePath) {
        final File tempDirectory = createTempDirectoryForPath(relativePath);

        Preconditions.checkArgument(StringUtils.isNotBlank(gitUrl), "scm.path property cannot be empty");

        final GitWorkspaceProviderImpl provider = new GitWorkspaceProviderImpl(tempDirectory, gitDirectoryLockTimeoutSeconds);
        final GitProctorCore gitCore = new GitProctorCore(gitUrl, gitUsername, gitPassword, testDefinitionsDirectory,
                provider, gitPullPushTimeoutSeconds, gitCloneTimeoutSeconds, gitCleanInitialization);

        final String branchName = relativePath.substring(relativePath.lastIndexOf("/") + 1);
        final GitProctor store = new GitProctor(gitCore, testDefinitionsDirectory, branchName);
        final String prefix = relativePath.replace('/', '-');
        final VarExporter exporter = VarExporter.forNamespace(GitProctor.class.getSimpleName()).includeInGlobal();
        exporter.export(store, prefix + "-");
        return createStoreWithGlobalCache(branchName, new CachingProctorStore(store));
    }

    ProctorStore createStoreWithGlobalCache(final String branchName, final ProctorStore store) {
        if (globalCacheStore == null) {
            LOGGER.info("Global cache is disabled");
            return store;
        }
        final Environment environment = Environment.fromName(branchName);
        Preconditions.checkNotNull(
                environment,
                "branch name" + branchName + " should be trunk, qa, or production"
        );
        return new GlobalCachingProctorStore(store, globalCacheStore, environment);
    }

    private File createTempDirectoryForPath(final String relativePath) {
        // replace "/" with "-" omit first "/" but omitEmptyStrings
        final String dirName = CharMatcher.is(File.separatorChar).trimAndCollapseFrom(relativePath, '-');
        final File temp = new File(tempRoot, dirName);
        if (temp.exists()) {
            if (!temp.isDirectory()) {
                throw new IllegalStateException(temp + " exists but is not a directory");
            }
        } else {
            if (!temp.mkdirs()) {
                throw new IllegalStateException("Could not create directory : " + temp);
            }
        }
        return temp;
    }

    /**
     * Identify the root-directory for TempFiles
     *
     * @return
     */
    private File identifyImplicitTempRoot() throws IOException {
        final File tempFile = File.createTempFile("implicit", GitProctorStoreFactory.class.getSimpleName());

        tempFile.delete();
        return tempFile.getParentFile();
    }

}
