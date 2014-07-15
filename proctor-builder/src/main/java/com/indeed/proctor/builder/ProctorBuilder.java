package com.indeed.proctor.builder;

import com.indeed.proctor.common.IncompatibleTestMatrixException;
import com.indeed.proctor.store.ProctorReader;
import com.indeed.proctor.store.StoreException;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.Writer;

public class ProctorBuilder {

    private static final Logger LOGGER = Logger.getLogger(ProctorBuilder.class);

    private final ProctorReader proctorReader;
    private final Writer outputSink;
    private final String author;
    private String version;

    public ProctorBuilder(final ProctorReader proctorReader, Writer outputSink) {
        this(proctorReader, outputSink, null);
    }
    public ProctorBuilder(final ProctorReader proctorReader, Writer outputSink, String author) {
        this(proctorReader, outputSink, author, "");
    }
    public ProctorBuilder(final ProctorReader proctorReader, Writer outputSink, String author, String version) {
        this.proctorReader = proctorReader;
        this.outputSink = outputSink;
        this.author = author;
        this.version = version;
    }

    public void execute() throws StoreException, IOException, IncompatibleTestMatrixException {
        ProctorBuilderUtils.generateArtifact(proctorReader, outputSink, author, version);
    }
}
