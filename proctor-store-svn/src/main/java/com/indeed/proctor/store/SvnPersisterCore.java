package com.indeed.proctor.store;

import com.indeed.proctor.store.FileBasedPersisterCore;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;

/**
 * @author parker
 */
public interface SvnPersisterCore extends FileBasedPersisterCore {
    SVNRepository getRepo();

    SVNClientManager getClientManager();

    SVNURL getSvnUrl();

    boolean cleanUserWorkspace(final String username);
}
