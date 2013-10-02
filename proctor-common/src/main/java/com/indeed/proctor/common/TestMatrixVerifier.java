package com.indeed.proctor.common;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

public class TestMatrixVerifier {
    private static final Logger LOGGER = Logger.getLogger(TestMatrixVerifier.class);

    @Nonnull
    private static final ObjectMapper OBJECT_MAPPER = Serializers.strict();
    @Nonnull
    private final String source;
    @Nonnull
    private final String testMatrixJson;
    private final int connectionTimeout;

    public TestMatrixVerifier(
            @Nonnull final String source,
            @Nonnull final String testMatrixJson,
            final int connectionTimeout
    ) {
        this.source = source;
        this.testMatrixJson = testMatrixJson;
        this.connectionTimeout = connectionTimeout;
    }

    @SuppressWarnings("UnusedDeclaration") // TODO Remove?
    public boolean verifyArtifact(@Nonnull final URL requiredTestsUrl) throws IncompatibleTestMatrixException, IOException, MisconfiguredProctorConsumerException, MissingTestMatrixException {
        final HttpURLConnection urlConnection = (HttpURLConnection) requiredTestsUrl.openConnection();
        urlConnection.setReadTimeout(connectionTimeout);

        InputStream inputStream = null;
        try {
            inputStream = urlConnection.getInputStream();
            final SpecificationResult specResults = OBJECT_MAPPER.readValue(inputStream, SpecificationResult.class);

            final String errorMessage = specResults.getError();
            if (errorMessage != null) {
                throw new MisconfiguredProctorConsumerException(errorMessage);
            }

            final ProctorSpecification spec = specResults.getSpecification();

            return verifyArtifact(spec);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (@Nonnull final IOException e) {
                    LOGGER.error("Unable to close input stream from " + requiredTestsUrl, e);
                }
            }
        }
    }

    private boolean verifyArtifact(final ProctorSpecification spec) throws IOException, IncompatibleTestMatrixException, MissingTestMatrixException {
        final StringProctorLoader loader = new StringProctorLoader(spec, source, testMatrixJson);
        loader.doLoad();
        return true;
    }

    public static void main(@Nonnull final String[] args) throws IOException, IncompatibleTestMatrixException, MisconfiguredProctorConsumerException, MissingTestMatrixException {
        if (args.length != 2) {
            System.err.println("Usage: java " + TestMatrixVerifier.class.getCanonicalName() + " <required-tests.json> <test-matrix.json>");
            System.exit(-2);
        }

        final String specificationFile = args[0];
        final String testMatrixFilename = args[1];

        final String testMatrixJson = readTestMatrix(testMatrixFilename);
        final TestMatrixVerifier verifier = new TestMatrixVerifier(testMatrixFilename, testMatrixJson, 15*1000);

        final ProctorSpecification spec = OBJECT_MAPPER.readValue(new File(specificationFile), ProctorSpecification.class);
        verifier.verifyArtifact(spec);
        LOGGER.info("Success verifying testMatrix against " + specificationFile);
    }

    @Nonnull
    private static String readTestMatrix(final String testMatrixFilename) throws IOException {
        final File testMatrixFile = new File(testMatrixFilename);

        final StringBuilder testMatrixJson = new StringBuilder();
        final char[] buffer = new char[2048];
        final Reader reader = new FileReader(testMatrixFile);

        while (true) {
            final int read = reader.read(buffer);
            if (read == -1) {
                break;
            }
            if (read > 0) {
                testMatrixJson.append(buffer, 0, read);
            }
        }

        return testMatrixJson.toString();
    }
}
