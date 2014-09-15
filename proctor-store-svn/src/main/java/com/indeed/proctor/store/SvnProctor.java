package com.indeed.proctor.store;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.indeed.proctor.common.model.TestMatrixVersion;
import org.apache.commons.io.FileUtils;
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

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class SvnProctor extends FileBasedProctorStore {
    private static final Logger LOGGER = Logger.getLogger(SvnProctor.class);

    /* Storage Schema:
        ${svnPath}/
            test-definitions/
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
        this(new SvnPersisterCoreImpl(svnPath, username, password, Files.createTempDir()));
    }

    public SvnProctor(final SvnPersisterCore core) {
        super(core);
        this.svnUrl = core.getSvnUrl();
    }

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
                final String testPath = TEST_DEFINITIONS_DIRECTORY + "/" + test;
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
    public boolean cleanUserWorkspace(String username) {
        return getSvnCore().cleanUserWorkspace(username);
    }

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

    public static void main(String args[]) throws IOException {
        final String svnpath = System.console().readLine("svn path: ");
        final String svnuser = System.console().readLine("user: ");
        final String password = new String(System.console().readPassword("password: "));
        final boolean usecache = "y".equals(System.console().readLine("cache (y/n): "));
        final int num_revisions = Integer.parseInt(System.console().readLine("number of histories: "));

        final File tempDir = Files.createTempDir();
        try {
            final SvnPersisterCoreImpl core = new SvnPersisterCoreImpl(svnpath, svnuser, password, tempDir);
            final SvnPersisterCore core1;
            if (usecache) {
                core1 = new CachedSvnPersisterCore(core);
            } else {
                core1 = core;
            }
            final SvnProctor client = new SvnProctor(core1);

            System.out.println("Running load matrix for last " + num_revisions + " revisions");
            final long start = System.currentTimeMillis();
            final List<Revision> revisions = client.getMatrixHistory(0, num_revisions);
            for (final Revision rev : revisions) {
                final TestMatrixVersion matrix = client.getTestMatrix(rev.getRevision());
            }
            final long elapsed = System.currentTimeMillis() - start;
            System.out.println("Finished reading matrix history (" + revisions.size() + ") in " + elapsed + " ms");
            client.close();
        } catch (StoreException e) {
            e.printStackTrace(System.err);
            LOGGER.error(e);
        } finally {
            System.out.println("Deleting temp dir : " + tempDir);
            FileUtils.deleteDirectory(tempDir);
        }
    }
}
