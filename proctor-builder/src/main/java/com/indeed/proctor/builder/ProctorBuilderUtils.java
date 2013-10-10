package com.indeed.proctor.builder;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.indeed.proctor.common.IncompatibleTestMatrixException;
import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.ProctorReader;
import com.indeed.proctor.store.StoreException;

import java.io.*;
import java.util.Map;

/**
 * @author parker
 */
class ProctorBuilderUtils {

    static void generateArtifact(final ProctorReader proctorPersister, final Writer outputSink,
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

        ProctorUtils.serializeArtifact(outputSink, artifact);
    }
}
