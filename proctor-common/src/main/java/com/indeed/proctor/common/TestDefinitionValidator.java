package com.indeed.proctor.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestDefinition;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Simple validator to check if the input JSON file is valid format as test definition.
 */
public class TestDefinitionValidator {
    private static final ObjectMapper OBJECT_MAPPER = Serializers.lenient();

    private TestDefinitionValidator() {
    }

    private static boolean isValidTestDefinition(final InputStream stream, final String testName, final String matrixSource) {
        try {
            final TestDefinition testDefinition = OBJECT_MAPPER.readValue(stream, TestDefinition.class);
            final ConsumableTestDefinition consumableTestDefinition = ConsumableTestDefinition.fromTestDefinition(testDefinition);
            ProctorUtils.verifyInternallyConsistentDefinition(testName, matrixSource, consumableTestDefinition);
            return true;
        } catch (final Throwable e) {
            System.err.println("Error: " + e.getMessage());
            return false;
        }
    }

    public static void main(final String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: java " + TestDefinitionValidator.class.getCanonicalName() + " <test_name> <matrix_source> <test_path>");
            System.exit(0);
        }

        final String testName = args[0];
        final String matrixSource = args[1];

        InputStream stream = null;
        try {
            stream = "-".equals(args[2]) ? System.in : new FileInputStream(args[2]);
        } catch (final FileNotFoundException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }

        final boolean result = isValidTestDefinition(stream, testName, matrixSource);
        System.exit(result ? 0 : 1);
    }
}
