package com.indeed.proctor.webapp.util;

import com.google.common.annotations.VisibleForTesting;
import com.indeed.util.varexport.Variable;
import com.indeed.util.varexport.servlet.ViewExportedVariablesServlet;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Deserializer of exported variables from response of {@link ViewExportedVariablesServlet} This
 * class is required because escape logic in {@link Variable} is not complete so naive
 * deserialization with {@link Properties} fails.
 *
 * <p>This is a example case how deserialization fails. 1. Consider variable `var` that stores this
 * json as string { "value": "this string contains double quote \" "} 2. Verexporter expose this as
 * follows var={ "value": "this string contains double quote \" "} 3. When loading it as property,
 * `var` will be loaded as follows { "value": "this string contains double quote " "} 4. This is not
 * more valid json format.
 */
public class VarExportedVariablesDeserializer {
    /**
     * Pattern to match string that needs to be escaped with additional '\'
     *
     * <p>Example string to match at the first character \ \\ \r \\: \\=
     *
     * <p>Example string not to match at the first character abc \: \= \u0101
     */
    @VisibleForTesting
    static final Pattern ESCAPE_TARGET_PATTERN = Pattern.compile("\\\\(?!(:|=|u[0-9a-f]{4}))");

    private static final String ESCAPE_REPLACEMENT_STRING = "\\\\\\\\";

    /**
     * Deserialize exported variables from response of {@link ViewExportedVariablesServlet} This
     * loads the input as {@link Properties} after preprocess for correct escaping.
     *
     * <p>Note that it's not possible to recover correct escaping for all possible cases. For
     * example, "\\u1010\u1010" (as Java string literal) will be deserialized to "\u1010\u1010" (as
     * Java string literal)
     */
    public static Properties deserialize(final String input) {
        final Properties properties = new Properties();
        final String escapedInput = escapeForProperties(input);
        try {
            properties.load(new ByteArrayInputStream(escapedInput.getBytes()));
        } catch (final IOException e) {
            // throws unchecked exception because
            // ByteArrayInputStream is not expected to throw IOException
            throw new UncheckedIOException("Unexpectedly, it failed to read from byte array.", e);
        }
        return properties;
    }

    public static Properties deserialize(final InputStream input) throws IOException {
        return deserialize(IOUtils.toString(input));
    }

    /** Make additional escape so that we can load it as Properties. */
    private static String escapeForProperties(final String input) {
        return ESCAPE_TARGET_PATTERN.matcher(input).replaceAll(ESCAPE_REPLACEMENT_STRING);
    }
}
