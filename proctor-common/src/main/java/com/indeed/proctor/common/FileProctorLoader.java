package com.indeed.proctor.common;

import com.indeed.proctor.common.model.TestMatrixArtifact;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.el.FunctionMapper;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Support class for loading a test matrix artifact from a JSON file
 *
 * @author ketan
 */
public class FileProctorLoader extends AbstractJsonProctorLoader {
    @Nonnull private final File inputFile;

    public FileProctorLoader(
            @Nonnull final ProctorSpecification specification,
            @Nonnull final String inputFile,
            @Nonnull final FunctionMapper functionMapper) {
        this(specification, new File(inputFile), functionMapper);
    }

    public FileProctorLoader(
            @Nonnull final ProctorSpecification specification,
            @Nonnull final File inputFile,
            @Nonnull final FunctionMapper functionMapper) {
        super(FileProctorLoader.class, specification, functionMapper);
        this.inputFile = inputFile;
    }

    @Nonnull
    @Override
    protected String getSource() {
        return inputFile.getAbsolutePath();
    }

    @CheckForNull
    @Override
    protected TestMatrixArtifact loadTestMatrix() throws IOException, MissingTestMatrixException, TestMatrixOutdatedException {
        if (! inputFile.exists()) {
            throw new MissingTestMatrixException("File " + inputFile + " does not exist");
        }
        if (!inputFile.canRead()) {
            throw new MissingTestMatrixException("Cannot read input file " + inputFile);
        }
        final Reader reader = new FileReader(inputFile);
        return loadJsonTestMatrix(reader);
    }
}
