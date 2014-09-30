package com.indeed.proctor.store;

import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;

/**
 * @author parker
 */
public interface SvnPersisterCore extends FileBasedPersisterCore {

    SVNURL getSvnUrl();

    boolean cleanUserWorkspace(final String username);

    <T> T doWithClientAndRepository(final SvnOperation<T> operation) throws StoreException;

    interface SvnOperation<T> {
        T execute(final SVNRepository repo, final SVNClientManager clientManager) throws Exception;
        StoreException handleException(final Exception e) throws StoreException;
    }
}
