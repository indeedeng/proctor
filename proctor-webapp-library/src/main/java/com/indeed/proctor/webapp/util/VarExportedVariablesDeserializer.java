package com.indeed.proctor.webapp.util;

import com.indeed.util.varexport.Variable;
import com.indeed.util.varexport.servlet.ViewExportedVariablesServlet;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * Deserializer of exported variables from response of {@link ViewExportedVariablesServlet}
 * This class is required because escape logic in {@link Variable} is not complete
 * so naive deserialization with {@link Properties} fails.
 */
public class VarExportedVariablesDeserializer {
    /**
     * Deserialize exported variables from response of {@link ViewExportedVariablesServlet}
     * This loads the input as {@link Properties}
     * after preprocess for correct escaping.
     *
     * Note that it's not possible to recover correct escaping for all possible cases.
     * For example, "\\u1010\u1010" (as Java string literal) will be deserialized
     * to "\u1010\u1010" (as Java string literal)
     */
    public static Properties deserialize(final String input) {
        final Properties properties = new Properties();
        final String escapedInput = escapeForProperties(input);
        try {
            properties.load(new ByteArrayInputStream(escapedInput.getBytes()));
        } catch (final IOException e) {
            // throws unchecked exception because "throws IOException" is not
            // documented in ByteArrayInputStream methods.
            throw new UncheckedIOException("Unexpectedly, it failed to read from byte array.", e);
        }
        return properties;
    }

    public static Properties deserialize(final InputStream input) throws IOException {
        return deserialize(IOUtils.toString(input));
    }

    /**
     * Make additional escape so that we can load it as Properties.
     */
    private static String escapeForProperties(final String input) {
        return input.replaceAll(
                "\\\\(?!(:|=|u[0-9a-f]{4}))",
                "\\\\\\\\"
        );
    }
}
