package com.indeed.proctor.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.indeed.proctor.common.Serializers;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import org.easymock.Capture;
import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FileBasedProctorStoreTest {

    private static final ObjectMapper OBJECT_MAPPER = Serializers.lenient();

    private static final String R0_REVISION = "r0";
    private FileBasedPersisterCore coreMock;
    private FileBasedProctorStore store;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private FileBasedProctorStore.RcsClient gitClient;

    @Before
    public void setUp() throws Exception {
        gitClient = EasyMock.createMock(FileBasedProctorStore.RcsClient.class);
        expect(gitClient.getRevisionControlType()).andReturn("git").anyTimes();
        gitClient.add(EasyMock.<File>anyObject());
        EasyMock.expectLastCall().anyTimes();
        replay(gitClient);
        coreMock = EasyMock.createMock(FileBasedPersisterCore.class);
        DelegatingCore coreMockWrapper = new DelegatingCore(coreMock, temporaryFolder.getRoot(), gitClient);
        store = new TestFileBasedProctorStore(coreMockWrapper);
    }

    @Test
    public void getTestMatrixNoTests() throws StoreException {
        final TestVersionResult versionStub = new TestVersionResult(
                Collections.<TestVersionResult.Test>emptyList(),
                new Date(),
                "fooAuthor",
                "fooVersion",
                "fooDescription");
        expect(coreMock.determineVersions(
                R0_REVISION
        ))
                // throw an unexpected type of runtime exception
                .andReturn(versionStub)
                // Must be evaluated, or this was not a valid test
                .once();
        replay(coreMock);
        final TestMatrixVersion result = store.getTestMatrix(R0_REVISION);
        assertNotNull(result);
        assertEquals(versionStub.getAuthor(), result.getAuthor());
        assertEquals(versionStub.getDescription(), result.getDescription());
        assertEquals(versionStub.getVersion(), result.getVersion());
        assertEquals(versionStub.getPublished(), result.getPublished());
    }

    @Test
    public void updateTestDefinitionTestDoesNotExist() throws Exception {
        final TestDefinition definition = new TestDefinition();
        final Map<String, String> metadata = Maps.newHashMap();

        final Capture<FileBasedProctorStore.ProctorUpdater> capturedArgument = new Capture<>();
        coreMock.doInWorkingDirectory(
                eq("fooUser"),
                eq("fooPassw0rd"),
                eq("fooComment"),
                eq("r0"),
                capture(capturedArgument)
        );
        EasyMock.expectLastCall();
        replay(coreMock);

        try {
            store.updateTestDefinition("fooUser", "fooPassw0rd", "fooAuthor", "r0",
                    definition, metadata, "fooComment");
            fail("Expected Exception");
        } catch (StoreException.TestUpdateException tue) {
            assertEquals("Attempting to update non-existent test r0", tue.getCause().getMessage());
        }
    }

    @Test
    public void updateTestDefinitionTestNoChange() throws Exception {
        final Map<String, String> metadata = Maps.newHashMap();

        final File definitionFolder = store.getTestDefinitionDirectoryForTest("r0", temporaryFolder.getRoot());
        assertTrue(definitionFolder.mkdirs());
        final TestDefinition definition = new TestDefinition();
        OBJECT_MAPPER.writeValue(new File(definitionFolder, FileBasedProctorStore.TEST_DEFINITION_FILENAME), definition);
        OBJECT_MAPPER.writeValue(new File(definitionFolder, FileBasedProctorStore.TEST_METADATA_FILENAME), metadata);

        final Capture<FileBasedProctorStore.ProctorUpdater> capturedArgument = new Capture<>();
        coreMock.doInWorkingDirectory(
                eq("fooUser"),
                eq("fooPassw0rd"),
                eq("fooComment"),
                eq("r0"),
                capture(capturedArgument)
        );
        EasyMock.expectLastCall();
        replay(coreMock);

        try {
            store.updateTestDefinition("fooUser", "fooPassw0rd", "fooAuthor", "r0",
                    definition, metadata, "fooComment");
            fail("Expected Exception");
        } catch (StoreException.TestUpdateException tue) {
            assertEquals("Attempting to save test definition without changes for test r0", tue.getCause().getMessage());
        }
    }

    private static class TestFileBasedProctorStore extends FileBasedProctorStore {

        protected TestFileBasedProctorStore(final FileBasedPersisterCore core) {
            super(core);
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public void verifySetup() throws StoreException {

        }

        @Override
        public boolean cleanUserWorkspace(final String username) {
            return false;
        }

        @Nonnull
        @Override
        public String getLatestVersion() throws StoreException {
            return null;
        }

        @Nonnull
        @Override
        public List<Revision> getMatrixHistory(final int start, final int limit) throws StoreException {
            return null;
        }

        @Nonnull
        @Override
        public List<Revision> getHistory(final String test, final int start, final int limit) throws StoreException {
            return null;
        }

        @Nonnull
        @Override
        public List<Revision> getHistory(final String test, final String revision, final int start, final int limit) throws StoreException {
            return null;
        }

        @CheckForNull
        @Override
        public RevisionDetails getRevisionDetails(final String revisionId) throws StoreException {
            return null;
        }

        @Nonnull
        @Override
        public Map<String, List<Revision>> getAllHistories() throws StoreException {
            return null;
        }

        @Override
        public void refresh() throws StoreException {

        }
    }

    /**
     * calls delegate, but for doInWorkingDirectory, calls client
     */
    private static class DelegatingCore implements FileBasedPersisterCore {
        private final FileBasedPersisterCore delegate;
        private FileBasedProctorStore.RcsClient client;
        private File dir;

        public DelegatingCore(final FileBasedPersisterCore mock, final File newdir, final FileBasedProctorStore.RcsClient newClient) {
            delegate = mock;
            dir = newdir;
            client = newClient;
        }

        @Override
        public <C> C getFileContents(final Class<C> c, final String[] path, final C defaultValue, final String revision) throws StoreException.ReadException, JsonProcessingException {
            return delegate.getFileContents(c, path, defaultValue, revision);
        }

        @Override
        public void doInWorkingDirectory(final String username, final String password, final String comment, final String previousVersion, final FileBasedProctorStore.ProctorUpdater updater) throws StoreException.TestUpdateException {
            try {
                updater.doInWorkingDirectory(client, dir);
            } catch (Exception e) {
                throw new StoreException.TestUpdateException("test", e);
            }
        }

        @Override
        public void doInWorkingDirectory(final String username, final String password, final String author, final String comment, final String previousVersion, final FileBasedProctorStore.ProctorUpdater updater) throws StoreException.TestUpdateException {
            try {
                updater.doInWorkingDirectory(client, dir);
            } catch (Exception e) {
                throw new StoreException.TestUpdateException("test", e);
            }
        }

        @Override
        public TestVersionResult determineVersions(final String fetchRevision) throws StoreException.ReadException {
            return delegate.determineVersions(fetchRevision);
        }

        @Override
        public String getAddTestRevision() {
            return delegate.getAddTestRevision();
        }

        @Override
        public void close() throws IOException {
            delegate.close();
        }
    }
}
