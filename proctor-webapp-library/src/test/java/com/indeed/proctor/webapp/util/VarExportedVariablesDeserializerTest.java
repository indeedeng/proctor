package com.indeed.proctor.webapp.util;

import com.indeed.util.varexport.ManagedVariable;
import com.indeed.util.varexport.Variable;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;

import static com.indeed.proctor.webapp.util.VarExportedVariablesDeserializer.ESCAPE_TARGET_PATTERN;
import static org.assertj.core.api.Assertions.assertThat;

public class VarExportedVariablesDeserializerTest {
    @Test
    public void testEscapeTargetPattern() {
        assertThat(ESCAPE_TARGET_PATTERN.asPredicate())
                .accepts(
                        "\\",
                        "\\\\",
                        "\\r",
                        "\\123Z",
                        "\\\\:",
                        "\\\\="
                )
                .rejects(
                        "",
                        "abc",
                        "\\:",
                        "\\=",
                        "\\u1010"
                );
    }

    @Test
    public void testDeserialize_simpleString() throws IOException {
        testDeserializeSingleString("");
        testDeserializeSingleString("value");
    }

    @Test
    public void testDeserialize_Symbols() throws IOException {
        testDeserializeSingleString("\"");
        testDeserializeSingleString("\\\"");
        testDeserializeSingleString("\\");
        testDeserializeSingleString("\\r\\n");
        testDeserializeSingleString("\\\\");
        testDeserializeSingleString("\\\"");
        testDeserializeSingleString("\\\"\\");
        testDeserializeSingleString("\\=:\\:\\=");
        testDeserializeSingleString("\\\\\\=\\:\\\\\\:\\\\\\\\=");
    }

    @Test
    public void testDeserialize_NonAscii() throws IOException {
        testDeserializeSingleString("これは日本語です");
    }

    @Test
    public void testDeserialize_ImpossibleCase() throws IOException {
        /*
         * For deserialization,
         * it's not possible to distinguish
         * "\\u1010" and "\u1010" because
         * Variable escapes "\u1010" to "\\u1010" first.
         * The original string "\\u1010\u1010" is visible
         * as "\\u1010\\u1010" for the class.
         */
        testDeserializeSingleString(
                "\\u1010\u1010",
                "\u1010\u1010"
        );
    }

    private static void testDeserializeSingleString(
            final String value,
            final String expected
    ) {
        final Variable<String> variable = ManagedVariable.<String>builder()
                .setName("key")
                .setValue(value)
                .build();
        final Properties expectedProps = new Properties();
        expectedProps.setProperty("key", expected);
        assertThat(
                VarExportedVariablesDeserializer.deserialize(
                        variable.toString()
                )
        )
                .as("Check deserialization of %s", value)
                .isEqualTo(expectedProps);
    }

    private static void testDeserializeSingleString(
            final String value
    ) {
        testDeserializeSingleString(
                value,
                value // the same value is expected expect for edge cases
        );
    }
}
