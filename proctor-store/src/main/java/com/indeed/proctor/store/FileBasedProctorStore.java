package com.indeed.proctor.store;

import com.google.common.collect.Maps;
import com.indeed.util.varexport.Export;
import com.indeed.proctor.common.Serializers;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author ketan
 * @author parker
 */
public abstract class FileBasedProctorStore implements ProctorStore {
    private static final Logger LOGGER = Logger.getLogger(FileBasedProctorStore.class);
    private static final String SUFFIX = ".json";
    static final String TEST_DEFINITIONS_DIRECTORY = "test-definitions";
    static final String TEST_METADATA_FILENAME = "metadata" + SUFFIX;
    static final String TEST_DEFINITION_FILENAME = "definition" + SUFFIX;
    final ObjectMapper objectMapper = Serializers.strict();

    protected final FileBasedPersisterCore core;

    protected FileBasedProctorStore(FileBasedPersisterCore core) {
        this.core = core;
    }

    /**
     * @return true if the file has changed
     */
    protected static <T> boolean writeIfChanged(final ObjectMapper mapper, final File f, final T newThing) throws StoreException.TestUpdateException {
        if (f.exists()) {
            try {
                final T currentThing = (T) mapper.readValue(f, newThing.getClass());
                if (currentThing.equals(newThing)) {
                    return false;
                }
            } catch (final JsonParseException e) {
                throw new StoreException.TestUpdateException("Unable to parse instance of " + newThing.getClass().getCanonicalName() + " from " + f, e);
            } catch (final JsonMappingException e) {
                throw new StoreException.TestUpdateException("Unable to parse instance of " + newThing.getClass().getCanonicalName() + " from " + f, e);
            } catch (final IOException e) {
                throw new StoreException.TestUpdateException("Unable to parse instance of " + newThing.getClass().getCanonicalName() + " from " + f, e);
            }
        }
        FileBasedProctorStore.writeThing(mapper, f, newThing);
        return true;
    }

    protected static <T> void writeThing(final ObjectMapper mapper, final File f, final T newThing) throws StoreException.TestUpdateException {
        try {
            mapper.defaultPrettyPrintingWriter().writeValue(f, newThing);
        } catch (final JsonGenerationException e) {
            throw new StoreException.TestUpdateException("Unable to write instance of " + newThing.getClass().getCanonicalName() + " to " + f, e);
        } catch (final JsonMappingException e) {
            throw new StoreException.TestUpdateException("Unable to write instance of " + newThing.getClass().getCanonicalName() + " to " + f, e);
        } catch (final IOException e) {
            throw new StoreException.TestUpdateException("Unable to write instance of " + newThing.getClass().getCanonicalName() + " to " + f, e);
        }
    }

    static File getTestDefinitionDirectory(final String testName, File workingDir) {
        return new File(workingDir + File.separator + TEST_DEFINITIONS_DIRECTORY + File.separator + testName);
    }

    @Override
    public TestMatrixVersion getCurrentTestMatrix() throws StoreException {
        return getTestMatrix(this.getLatestVersion());
    }

    @Export(name="core", doc="")
    public FileBasedPersisterCore getCore() {
        return core;
    }

    @Override
    public final TestMatrixVersion getTestMatrix(final long fetchRevision) throws StoreException {
        long start = System.currentTimeMillis();
        final TestVersionResult result = core.determineVersions(fetchRevision);
        if(LOGGER.isDebugEnabled()) {
            final long elapsed = System.currentTimeMillis() - start;
            LOGGER.debug(String.format("Took %d ms to identify %d potential tests", elapsed, result.getTests().size()));
        }
        if(result == null) {
            LOGGER.error("Unable to determine tests for " + core.toString());
            return null;
        }
        TestMatrixVersion tmv = new TestMatrixVersion();

        final Map<String, TestDefinition> testDefinitions = Maps.newLinkedHashMap();
        start = System.currentTimeMillis();
        for (final TestVersionResult.Test testDefFile : result.getTests()) {
            final long startForTest = System.currentTimeMillis();
            final TestDefinition testDefinition = getTestDefinition(testDefFile.getTestName(), testDefFile.getRevision());
            if(LOGGER.isTraceEnabled()) {
                final long elapsed = System.currentTimeMillis() - startForTest;
                LOGGER.debug(String.format("Took %d ms to load %s (r%d) %s", elapsed, testDefFile.getTestName(), testDefFile.getRevision(), testDefinition == null ? "unsuccessfully" : "successfully"));
            }
            if(testDefinition == null) {
                LOGGER.info("Returning null TestMatrix because " + testDefFile.getTestName() + " returned null test-definition.");
                return null;
            }
            testDefinitions.put(testDefFile.getTestName(), testDefinition);
        }
        if(LOGGER.isDebugEnabled()) {
            final long elapsed = System.currentTimeMillis() - start;
            LOGGER.debug(String.format("Took %d ms to load all %d tests", elapsed, testDefinitions.size()));
        }

        final TestMatrixDefinition tmd = new TestMatrixDefinition();
        tmd.setTests(testDefinitions);

        tmv.setTestMatrixDefinition(tmd);

        tmv.setPublished(result.getPublished());
        tmv.setAuthor(result.getAuthor());
        tmv.setVersion(result.getVersion());
        tmv.setDescription(result.getDescription());

        return tmv;
    }

    @Override
    public TestDefinition getCurrentTestDefinition(final String testName) throws StoreException {
        // Get the first test history
        final List<Revision> tdvList = this.getHistory(testName, 0, 1);
        if(tdvList.size() == 1) {
            final Revision tdv = tdvList.get(0);
            return getTestDefinition(testName, tdv.getRevision());
        } else {
            LOGGER.info("Not history returned for " + testName + ", returning null");
            return null;
        }
    }

    @Override
    public TestDefinition getTestDefinition(final String testName, long fetchRevision) throws StoreException {
        try {
            return getFileContents(TestDefinition.class, new String[] { TEST_DEFINITIONS_DIRECTORY, testName, TEST_DEFINITION_FILENAME }, null, fetchRevision);
        } catch (final JsonProcessingException e) {
            LOGGER.error(String.format("Unable to deserialize JSON for %s r%d", testName, fetchRevision), e);
            return null;
        } catch (final StoreException.ReadException e) {
            LOGGER.error(String.format("Unable to read test %s for r%d", testName, fetchRevision), e);
            return null;
        }
    }

    public void shutdown() {
        try {
            close();
        } catch (IOException e) {
            LOGGER.error("Ignored exception during closing", e);
        }
    }

    @Override
    public void close() throws IOException {
        core.close();
    }

    private final <C> C getFileContents(Class<C> c, String[] path, C defaultValue, long revision) throws StoreException.ReadException, JsonProcessingException {
        return core.getFileContents(c, path, defaultValue, revision);
    }

    protected final <T> boolean updateThing(final FileBasedProctorStore.RcsClient rcsClient, final File file, final T thing) throws Exception {
        final boolean thingExisted = file.exists();
        final boolean thingChanged = FileBasedProctorStore.writeIfChanged(objectMapper, file, thing);
        if (!thingExisted) {
            rcsClient.add(file);
        }
        return thingChanged;
    }

    @Override
    public final void updateTestDefinition(final String username, final String password, final long previousVersion, final String testName, final TestDefinition testDefinition, final Map<String, String> metadata, final String comment) throws StoreException.TestUpdateException {
        LOGGER.info(String.format("Update Test Definition: %s %s r%d", username, testName, previousVersion));
        core.doInWorkingDirectory(username, password, comment, previousVersion, new ProctorUpdater() {
            @Override
            public boolean doInWorkingDirectory(final RcsClient rcsClient, final File workingDir) throws Exception {
                final File testDefinitionDirectory = getTestDefinitionDirectory(testName, workingDir);
                final File testDefinitionFile = new File(testDefinitionDirectory + File.separator + TEST_DEFINITION_FILENAME);
                final File metaDataFile = new File(testDefinitionDirectory + File.separator + TEST_METADATA_FILENAME);
                if (!testDefinitionFile.exists()) {
                    throw new StoreException.TestUpdateException("Attempting to update non-existent test " + testName);
                }

                //  this is easier than trying to get svnKit to do a useful diff
                boolean thingsChanged = updateThing(rcsClient, testDefinitionFile, testDefinition);
                thingsChanged = updateThing(rcsClient, metaDataFile, metadata) || thingsChanged;
                return thingsChanged;
            }
        });
    }

    @Override
    public final void addTestDefinition(final String username, final String password, final String testName, final TestDefinition testDefinition, final Map<String, String> metadata, final String comment) throws StoreException.TestUpdateException {
        LOGGER.info(String.format("Add Test Definition: %s %s", username, testName));
        core.doInWorkingDirectory(username, password, comment, 0, new ProctorUpdater() {
            @Override
            public boolean doInWorkingDirectory(final RcsClient rcsClient, final File workingDir) throws Exception {
                final File testDefinitionDirectory = getTestDefinitionDirectory(testName, workingDir);
                final File testDefinitionFile = new File(testDefinitionDirectory + File.separator + TEST_DEFINITION_FILENAME);
                final File metaDataFile = new File(testDefinitionDirectory + File.separator + TEST_METADATA_FILENAME);

                if (testDefinitionFile.exists() || metaDataFile.exists()) {
                    throw new StoreException.TestUpdateException("Supposedly new test '" + testName + "' already exists");
                }

                testDefinitionDirectory.mkdirs();

                writeThing(objectMapper, testDefinitionFile, testDefinition);
                rcsClient.add(testDefinitionFile);

                writeThing(objectMapper, metaDataFile, metadata);
                rcsClient.add(metaDataFile);

                return true;
            }
        });
    }

    @Override
    public final void deleteTestDefinition(final String username, final String password, final long previousVersion, final String testName, final TestDefinition testDefinition, final String comment)
            throws StoreException.TestUpdateException {
        LOGGER.info(String.format("Delete Test Definition: %s %s r%d ", username, testName, previousVersion));
        core.doInWorkingDirectory(username, password, comment, previousVersion, new ProctorUpdater() {
            @Override
            public boolean doInWorkingDirectory(final RcsClient rcsClient, final File workingDir) throws Exception {
                final File testDefinitionDirectory = getTestDefinitionDirectory(testName, workingDir);

                if (!testDefinitionDirectory.exists()) {
                    throw new StoreException.TestUpdateException("Unable to delete non-existent test " + testName);
                }
                rcsClient.delete(testDefinitionDirectory);

                return true;
            }
        });
    }

    interface ProctorUpdater {
        boolean doInWorkingDirectory(FileBasedProctorStore.RcsClient rcsClient, File workingDir) throws Exception;
    }

    interface RcsClient {
        void add(File file) throws Exception;

        void delete(File testDefinitionDirectory) throws Exception;
    }

    static class TestVersionResult {
        private List<TestVersionResult.Test> tests;
        private Date published;
        private String author;
        private long version;
        private String description;

        TestVersionResult(List<TestVersionResult.Test> tests, Date published, String author, long version, String description) {
            this.tests = tests;
            this.published = published;
            this.author = author;
            this.version = version;
            this.description = description;
        }

        static class Test {
            final String testName;
            final long revision;

            Test(String testName, long revision) {
                this.testName = testName;
                this.revision = revision;
            }

            public String getTestName() {
                return testName;
            }

            public long getRevision() {
                return revision;
            }
        }

        public List<Test> getTests() {
            return tests;
        }

        public Date getPublished() {
            return published;
        }

        public String getAuthor() {
            return author;
        }

        public long getVersion() {
            return version;
        }

        public String getDescription() {
            return description;
        }
    }
}
