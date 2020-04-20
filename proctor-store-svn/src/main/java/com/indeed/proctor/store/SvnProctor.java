package com.indeed.proctor.store;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixDefinition;
import org.apache.log4j.Logger;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNRevisionProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNRevision;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SvnProctor extends FileBasedProctorStore {
    private static final Logger LOGGER = Logger.getLogger(SvnProctor.class);

    /* Storage Schema:
        ${svnPath}/
            ${testDefinitionsDirectory}/
                test-name-one/
                    definition.json
                    metadata.json
                test-name-two/
                    definition.json
                    metadata.json
    */

    private final SVNURL svnUrl;

    public SvnProctor(final String svnPath,
                      final String username,
                      final String password) throws IOException {
        this(svnPath, username, password, DEFAULT_TEST_DEFINITIONS_DIRECTORY);
    }

    public SvnProctor(final String svnPath,
                      final String username,
                      final String password,
                      final String testDefinitionsDirectory) throws IOException {
        this(new SvnPersisterCoreImpl(svnPath, username, password, testDefinitionsDirectory, Files.createTempDir()), testDefinitionsDirectory);
    }

    public SvnProctor(final SvnPersisterCore core) {
        this(core, DEFAULT_TEST_DEFINITIONS_DIRECTORY);
    }

    public SvnProctor(final SvnPersisterCore core, final String testDefinitionsDirectory) {
        super(core, testDefinitionsDirectory);
        this.svnUrl = core.getSvnUrl();
    }

    @Nonnull
    @Override
    public List<Revision> getHistory(final String test, final int start, final int limit) throws StoreException {
        return getSvnCore().doWithClientAndRepository(new SvnPersisterCore.SvnOperation<List<Revision>>() {
            @Override
            public List<Revision> execute(final SVNRepository repo, final SVNClientManager clientManager) throws Exception {
                final long latestRevision = repo.getLatestRevision();
                return getHistory(test, String.valueOf(latestRevision), start, limit);
            }

            @Override
            public StoreException handleException(final Exception e) throws StoreException {
                throw new StoreException.ReadException("Unable to get older revisions for " + test, e);
            }
        });
    }

    @Nonnull
    @Override
    public List<Revision> getHistory(final String test,
                                     final String version,
                                     final int start,
                                     final int limit) throws StoreException {
        final Long revision = SvnPersisterCoreImpl.parseRevisionOrDie(version);

        return getSvnCore().doWithClientAndRepository(new SvnPersisterCore.SvnOperation<List<Revision>>() {
            @Override
            public List<Revision> execute(final SVNRepository repo, final SVNClientManager clientManager) throws Exception {
                // check path before executing svn log
                final String testPath = getTestDefinitionsDirectory() + "/" + test;
                final SVNNodeKind kind = repo.checkPath(testPath, revision);
                if (kind == SVNNodeKind.NONE) {
                    return Collections.emptyList();
                }

                final String[] targetPaths = {testPath};


                final SVNRevision svnRevision = SVNRevision.create(revision);
                return getSVNLogs(clientManager, targetPaths, svnRevision, start, limit);
            }

            @Override
            public StoreException handleException(final Exception e) throws StoreException {
                throw new StoreException.ReadException("Unable to get older revisions for " + test + " r" + revision, e);
            }
        });
    }

    /**
     * @deprecated Unsupported operation.
     */
    @Deprecated
    @Override
    public RevisionDetails getRevisionDetails(final String revisionId) {
        throw new UnsupportedOperationException("revision details is not supported in SVN store");
    }

    @Nonnull
    @Override
    public List<TestDefinition> getTestDefinitions(final String testName, final String revision, final int start, final int limit) throws StoreException {
        throw new UnsupportedOperationException("test definitions is not supported in SVN store");
    }

    @Nonnull
    @Override
    public Map<String, List<Revision>> getAllHistories() throws StoreException {
        final TestMatrixDefinition testMatrixDefinition = getCurrentTestMatrix().getTestMatrixDefinition();
        if (testMatrixDefinition == null) {
            return Collections.emptyMap();
        }

        final Map<String, List<Revision>> histories = Maps.newHashMap();
        for (final String test : testMatrixDefinition.getTests().keySet()) {
            histories.put(test, getHistory(test, 0, Integer.MAX_VALUE));
        }
        return histories;
    }

    @Override
    public void refresh() throws StoreException {
        /* do nothing */
    }

    @Nonnull
    @Override
    public String getLatestVersion() throws StoreException {
        return getSvnCore().doWithClientAndRepository(new SvnPersisterCore.SvnOperation<String>() {
            @Override
            public String execute(final SVNRepository repo, final SVNClientManager clientManager) throws Exception {
                final String[] targetPaths = {};
                final SVNRevision svnRevision = SVNRevision.HEAD;
                final SVNLogClient logClient = clientManager.getLogClient();
                final FilterableSVNLogEntryHandler handler = new FilterableSVNLogEntryHandler();

                // In order to get history is "descending" order, the startRevision should be the one closer to HEAD
                logClient.doLog(svnUrl, targetPaths, /* pegRevision */ SVNRevision.HEAD, svnRevision, SVNRevision.create(1),
                                /* stopOnCopy */ false, /* discoverChangedPaths */ false, /* includeMergedRevisions */ false,
                                /* limit */ 1,
                                new String[]{SVNRevisionProperty.LOG}, handler);
                final SVNLogEntry entry = handler.getLogEntries().size() > 0 ? handler.getLogEntries().get(0) : null;
                return entry == null ? "-1" : String.valueOf(entry.getRevision());
            }

            @Override
            public StoreException handleException(final Exception e) throws StoreException {
                throw new StoreException.ReadException("Unable to get latest revision", e);
            }
        });
    }

    @Override
    public boolean cleanUserWorkspace(final String username) {
        return getSvnCore().cleanUserWorkspace(username);
    }

    @Nonnull
    @Override
    public List<Revision> getMatrixHistory(final int start, final int limit) throws StoreException {
        final String[] targetPaths = {};
        return getSvnCore().doWithClientAndRepository(new SvnPersisterCore.SvnOperation<List<Revision>>() {
            @Override
            public List<Revision> execute(final SVNRepository repo, final SVNClientManager clientManager) throws Exception {
                return getSVNLogs(clientManager, targetPaths, SVNRevision.HEAD, start, limit);
            }

            @Override
            public StoreException handleException(final Exception e) throws StoreException {
                throw new StoreException.ReadException("Unable to get matrix history", e);
            }
        });

    }

    @Nonnull
    @Override
    public List<Revision> getMatrixHistory(final Instant sinceInclusive, final Instant untilExclusive) throws StoreException {
        throw new UnsupportedOperationException("Not implemented");
    }

    private List<Revision> getSVNLogs(final SVNClientManager clientManager,
                                      final String[] paths,
                                      final SVNRevision startRevision,
                                      final int start, final int limit) throws StoreException.ReadException {
        try {
            final SVNLogClient logClient = clientManager.getLogClient();
            final FilterableSVNLogEntryHandler handler = new FilterableSVNLogEntryHandler();

            // In order to get history is "descending" order, the startRevision should be the one closer to HEAD
            logClient.doLog(svnUrl, paths, /* pegRevision */ SVNRevision.HEAD, startRevision, SVNRevision.create(1),
                            /* stopOnCopy */ false, /* discoverChangedPaths */ false, /* includeMergedRevisions */ false,
                            /* limit */ start + limit,
                            new String[]{SVNRevisionProperty.LOG, SVNRevisionProperty.AUTHOR, SVNRevisionProperty.DATE}, handler);

            final List<SVNLogEntry> entries = handler.getLogEntries();

            final List<Revision> revisions;
            if (entries.size() <= start) {
                revisions = Collections.emptyList();
            } else {
                final int end = Math.min(start + limit, entries.size());

                revisions = Lists.newArrayListWithCapacity(end - start);

                for (int i = 0; i < end - start; i++) {
                    final SVNLogEntry entry = entries.get(start + i);
                    revisions.add(new Revision(String.valueOf(entry.getRevision()), entry.getAuthor(), entry.getDate(), entry.getMessage()));
                }
            }
            return revisions;
        } catch (final SVNException e) {
            throw new StoreException.ReadException("Unable to get older revisions");
        }
    }

    @Override
    public void verifySetup() throws StoreException {
        final Long latestRevision = getSvnCore().doWithClientAndRepository(new SvnPersisterCore.SvnOperation<Long>() {
            @Override
            public Long execute(final SVNRepository repo, final SVNClientManager clientManager) throws Exception {
                return repo.getLatestRevision();
            }

            @Override
            public StoreException handleException(final Exception e) throws StoreException {
                throw new StoreException("Failed to get latest revision for svn-path: " + svnUrl, e);
            }
        });
        if (latestRevision <= 0) {
            throw new StoreException("Found non-positive revision (" + latestRevision + ") for svn-path: " + svnUrl);
        }
    }

    @Override
    public String toString() {
        return core.toString();
    }

    private SvnPersisterCore getSvnCore() {
        return (SvnPersisterCore) this.core;
    }

    @Override
    public String getName() {
        return SvnProctor.class.getName();
    }
}
