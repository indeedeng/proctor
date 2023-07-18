package com.indeed.proctor.store;

import com.indeed.proctor.store.StoreException.TestUpdateException;
import org.eclipse.jgit.api.errors.TransportException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GitAPIExceptionWrapperTest {
    private static final String GIT_URL = "repository.git";
    private static final TransportException NOT_AUTHORIZED_EXCEPTION =
            new TransportException("not authorized");
    private static final TransportException DEVELOPER_ACCESS_LEVEL_EXCEPTION =
            new TransportException("git-receive-pack not permitted");
    private static final TransportException SERVER_ERROR_EXCEPTION =
            new TransportException("500 Internal Server Error");
    private static final TransportException SERVER_ERROR_EXCEPTION2 =
            new TransportException("502 Bad Gateway");
    private static final IllegalStateException MASTER_ACCESS_LEVEL_EXCEPTION =
            new IllegalStateException("pre-receive hook declined");

    private GitAPIExceptionWrapper gitAPIExceptionWrapper;

    @Before
    public void setUp() {
        gitAPIExceptionWrapper = new GitAPIExceptionWrapper();
        gitAPIExceptionWrapper.setGitUrl(GIT_URL);
    }

    private static TestUpdateException getTestUpdateException(final Throwable cause) {
        return new TestUpdateException("TestUpdateException", cause);
    }

    @Test
    public void testWrapExceptionWithNonGitUrl() {
        final String gitUrl = "non_git_url";
        gitAPIExceptionWrapper.setGitUrl(gitUrl);

        {
            final TestUpdateException exception = getTestUpdateException(NOT_AUTHORIZED_EXCEPTION);
            final TestUpdateException result = gitAPIExceptionWrapper.wrapException(exception);
            assertEquals(exception, result);
        }
        {
            final TestUpdateException exception =
                    getTestUpdateException(DEVELOPER_ACCESS_LEVEL_EXCEPTION);
            final TestUpdateException result = gitAPIExceptionWrapper.wrapException(exception);
            assertEquals(exception, result);
        }
        {
            final TestUpdateException exception = getTestUpdateException(SERVER_ERROR_EXCEPTION);
            final TestUpdateException result = gitAPIExceptionWrapper.wrapException(exception);
            assertEquals(exception, result);
        }
    }

    @Test
    public void testWrapExceptionInGitNoAuthorizationException() {
        final TestUpdateException exception = getTestUpdateException(NOT_AUTHORIZED_EXCEPTION);
        final TestUpdateException result = gitAPIExceptionWrapper.wrapException(exception);
        assertTrue(result instanceof GitNoAuthorizationException);
    }

    @Test
    public void testWrapExceptionInGitNoDeveloperAccessLevelException() {
        final TestUpdateException exception =
                getTestUpdateException(DEVELOPER_ACCESS_LEVEL_EXCEPTION);
        final TestUpdateException result = gitAPIExceptionWrapper.wrapException(exception);
        assertTrue(result instanceof GitNoDevelperAccessLevelException);
    }

    @Test
    public void testWrapExceptionInGitServerErrorException() {
        {
            final TestUpdateException exception = getTestUpdateException(SERVER_ERROR_EXCEPTION);
            final TestUpdateException result = gitAPIExceptionWrapper.wrapException(exception);
            assertTrue(result instanceof GitServerErrorException);
        }
        {
            final TestUpdateException exception = getTestUpdateException(SERVER_ERROR_EXCEPTION2);
            final TestUpdateException result = gitAPIExceptionWrapper.wrapException(exception);
            assertTrue(result instanceof GitServerErrorException);
        }
    }

    @Test
    public void testWrapExceptionInGitNoMasterAccessLevelException() {
        final TestUpdateException exception = getTestUpdateException(MASTER_ACCESS_LEVEL_EXCEPTION);
        final TestUpdateException result = gitAPIExceptionWrapper.wrapException(exception);
        assertTrue(result instanceof GitNoMasterAccessLevelException);
    }
}
