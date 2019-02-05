package com.indeed.proctor.store;

import org.eclipse.jgit.api.errors.TransportException;

public class GitAPIExceptionWrapper {
    private String gitUrl;

    public void setGitUrl(final String gitUrl) {
        this.gitUrl = gitUrl;
    }

    public StoreException.TestUpdateException wrapException(final StoreException.TestUpdateException exception) {
        final Throwable cause = exception.getCause();

        if (gitUrl.endsWith(".git")) {
            if (cause instanceof TransportException) {
                if (cause.getMessage().contains("not authorized")) {
                    return new GitNoAuthorizationException("Please check your user name and password", exception);
                } else if (cause.getMessage().contains("git-receive-pack not permitted")) {
                    return new GitNoDevelperAccessLevelException(
                            "Check if your access level is developer in [" + gitUrl.substring(0, gitUrl.length() - 4) + "/project_members]",
                            exception);
                } else if (cause.getMessage().matches("^50\\d\\s.*")) {
                    return new GitServerErrorException("Check if " + gitUrl + " is available", exception);
                }
            } else if (cause instanceof IllegalStateException) {
                if ("pre-receive hook declined".equals(cause.getMessage())) {
                    return new GitNoMasterAccessLevelException(
                            "Check if your access level is master in [" + gitUrl.substring(0, gitUrl.length() - 4) + "/project_members]",
                            exception);
                }
            }
        }
        return exception;
    }
}