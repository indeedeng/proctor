package com.indeed.proctor.builder;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.indeed.proctor.common.IncompatibleTestMatrixException;
import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.ProcterReader;
import com.indeed.proctor.store.StoreException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * @author parker
 */
class ProctorBuilderUtils {

    static void generateArtifact(final ProcterReader proctorPersister, final String outputDirectory, final String filename,
                                           final String authorOverride, final long versionOverride
    ) throws IOException, IncompatibleTestMatrixException, StoreException {
        final TestMatrixVersion currentTestMatrix = proctorPersister.getCurrentTestMatrix();
        if(currentTestMatrix == null) {
            throw new RuntimeException("Failed to load current test matrix for " + proctorPersister);
        }

        // I'm not sure if it's better for the LocalDirectoryPersister to be aware of this svn info, or for all the overrides to happen here.
        if(!CharMatcher.WHITESPACE.matchesAllOf(Strings.nullToEmpty(authorOverride))) {
            currentTestMatrix.setAuthor(authorOverride);
        }
        if(versionOverride > 0) {
            currentTestMatrix.setVersion(versionOverride);
        }

        final TestMatrixArtifact artifact = ProctorUtils.convertToConsumableArtifact(currentTestMatrix);

        // For each test, verify that it's internally consistent (buckets sum to 1.0, final null allocation)
        final String matrixSource = artifact.getAudit().getUpdatedBy() + "@" + artifact.getAudit().getVersion();
        for(final Map.Entry<String, ConsumableTestDefinition> td : artifact.getTests().entrySet()) {
            ProctorUtils.verifyInternallyConsistentDefinition(td.getKey(), matrixSource, td.getValue());
        }

        if("-".equals(outputDirectory)) {
            ProctorUtils.serializeArtifact(new PrintWriter(System.out), artifact);
        } else {
            final FileWriter writer = new FileWriter(outputDirectory + File.separator + filename);
            ProctorUtils.serializeArtifact(writer, artifact);
            writer.close();
        }
    }

    static void generateArtifact(ProcterReader proctorPersister, ProctorBuilderArgs arguments) throws IOException, IncompatibleTestMatrixException, StoreException {
        generateArtifact(proctorPersister, arguments.getOutputdir(), arguments.getFilename(),
                         arguments.getAuthor(), arguments.getVersion());
    }
}
