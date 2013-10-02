package com.indeed.proctor.store;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.indeed.proctor.common.Serializers;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * @author parker
 */
public class LocalDirectoryCore implements FileBasedPersisterCore {
    private static final Logger LOGGER = Logger.getLogger(LocalDirectoryCore.class);
    final ObjectMapper objectMapper = Serializers.strict();

    final File baseDir;

    public LocalDirectoryCore(File baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public <C> C getFileContents(Class<C> c, String[] path_parts, C defaultValue, long revision) throws StoreException.ReadException, JsonProcessingException {
        final String path = Joiner.on(File.separator).join(path_parts);
        FileReader reader = null;

        try {
            final File file = new File(baseDir + File.separator + path);
            if(file.exists()) {
                reader = new FileReader(file);
                final C testDefinition = objectMapper.readValue(reader, c);
                return testDefinition;
            } else {
                if(LOGGER.isInfoEnabled()) {
                    LOGGER.info(file + " does not exists, returning defaultValue.");
                }
                return defaultValue;
            }
        } catch (IOException e) {
            Throwables.propagateIfInstanceOf(e, JsonProcessingException.class);
            throw new StoreException.ReadException("Error reading " + path, e);
        } finally {
            if(reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    LOGGER.error("Suppressing throwable thrown when closing "+reader, e);
                }
            }
        }
    }

    @Override
    public void close() {
        // no op
    }

    static class LocalRcsClient implements FileBasedProctorStore.RcsClient {
        @Override
        public void add(File file) throws Exception {
        }

        @Override
        public void delete(File testDefinitionDirectory) throws Exception {
            testDefinitionDirectory.delete();
        }
    }


    @Override
    public void doInWorkingDirectory(String username, String password, String comment, long previousVersion, FileBasedProctorStore.ProctorUpdater updater) throws StoreException.TestUpdateException {
        try {
            final FileBasedProctorStore.RcsClient rcsClient = new LocalRcsClient();
            final boolean thingsChanged = updater.doInWorkingDirectory(rcsClient, baseDir);
        } catch (final Exception e) {
            throw new StoreException.TestUpdateException("Unable to perform operation", e);
        } finally {
        }
    }

    @Override
    public FileBasedProctorStore.TestVersionResult determineVersions(long fetchRevision) throws StoreException.ReadException {
        final File testDir = new File(baseDir + File.separator + FileBasedProctorStore.TEST_DEFINITIONS_DIRECTORY);
        // List all of the directories, excluding the directories created by svn (implementation is ignoring directories named '.svn'
        final File[] testDefFiles = testDir.listFiles( (FileFilter) FileFilterUtils.makeSVNAware(FileFilterUtils.directoryFileFilter()) );
        final List<FileBasedProctorStore.TestVersionResult.Test> tests = Lists.newArrayListWithExpectedSize(testDefFiles.length);
        for (final File testDefFile : testDefFiles) {
            final String testName = testDefFile.getName();
            tests.add(new FileBasedProctorStore.TestVersionResult.Test(testName, fetchRevision));
        }

        return new FileBasedProctorStore.TestVersionResult(
            tests,
            new Date(System.currentTimeMillis()),
            System.getenv("USER"),
            System.currentTimeMillis(),
            ""
        );

    }
}
