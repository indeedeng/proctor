package com.indeed.proctor.store;

import com.indeed.proctor.store.FileBasedPersisterCore;
import org.eclipse.jgit.lib.Repository;

public interface GitPersisterCore extends FileBasedPersisterCore {
    Repository getRepo();

    String getGitUrl();

    boolean cleanUserWorkspace(final String username);
}
