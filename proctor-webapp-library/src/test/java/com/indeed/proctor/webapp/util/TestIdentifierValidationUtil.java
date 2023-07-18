package com.indeed.proctor.webapp.util;

import com.google.common.collect.ImmutableList;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.indeed.proctor.webapp.util.IdentifierValidationUtil.isValidBucketName;
import static com.indeed.proctor.webapp.util.IdentifierValidationUtil.validateMetaTags;
import static com.indeed.proctor.webapp.util.IdentifierValidationUtil.validateTestName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestIdentifierValidationUtil {

    @Test
    public void testValidateTestName() {
        final String maxLengthName = StringUtils.repeat("a", 100);
        final String tooLongName = StringUtils.repeat("a", 101);

        for (final String input :
                new String[] {"", "0", "_", "__", ".", "_0", "0_", "a_0", "0_a", "a#b"}) {
            assertThatThrownBy(() -> validateTestName(input))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(
                            "Test Name must be alpha-numeric underscore and not start/end with a number");
        }
        assertThatThrownBy(() -> validateTestName(tooLongName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Test Name length can't be longer than 100");

        // Nothing happens for valid test names
        assertThat(
                        Arrays.asList(
                                "a",
                                "A",
                                "a_a",
                                "a1a",
                                "_0_",
                                "_0___",
                                "_a_b_",
                                "_a",
                                "a_",
                                maxLengthName))
                .allSatisfy(IdentifierValidationUtil::validateTestName);
    }

    @Test
    public void testIsValidMetaTag() {
        final String maxLengthName = StringUtils.repeat("a", 100);
        final String tooLongName = StringUtils.repeat("a", 101);

        assertThat(
                        Arrays.asList(
                                "a",
                                "A",
                                "a_a",
                                "a_b",
                                "0a",
                                "a1a",
                                "a.a",
                                "a:a",
                                "_0_",
                                "_0___",
                                "_a_b_",
                                "_a",
                                "a_",
                                "_0",
                                "0",
                                "0_",
                                "a_0",
                                "0_a",
                                maxLengthName))
                .allMatch(IdentifierValidationUtil::isValidMetaTag);

        assertThat(
                        Arrays.asList(
                                "",
                                "_",
                                "__",
                                ",",
                                ";",
                                ".",
                                ":",
                                "a.",
                                "a:",
                                ".a",
                                ":a",
                                "a#b",
                                tooLongName))
                .as("should be invalid")
                .allMatch(metaTag -> !IdentifierValidationUtil.isValidMetaTag(metaTag));
    }

    @Test
    public void testIsValidMetaTags() {
        final TestDefinition invalidMetaTagDefinition =
                createTestDefinition(ImmutableList.of("0", "_", "a", "__"));
        final TestDefinition emptyMetaTagDefinition = createTestDefinition(ImmutableList.of());
        final TestDefinition validMetaTagDefinition =
                createTestDefinition(ImmutableList.of("a", "a_a"));

        assertThatThrownBy(() -> validateMetaTags(invalidMetaTagDefinition))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "Meta Tag must \n"
                                + " - be alpha-numeric underscore\n"
                                + " - not be longer than 100\n"
                                + " but found: [_, __]");

        // Nothing happens for valid meta tags
        validateMetaTags(emptyMetaTagDefinition);
        validateMetaTags(validMetaTagDefinition);
    }

    private TestDefinition createTestDefinition(final List<String> metaTags) {
        return new TestDefinition(
                "",
                "",
                TestType.EMAIL_ADDRESS,
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                false,
                Collections.emptyMap(),
                Collections.emptyMap(),
                "",
                metaTags);
    }

    @Test
    public void testIsValidBucketName() {
        assertThat(Arrays.asList("", "0invalid_bucket_Name"))
                .allMatch(name -> !isValidBucketName(name));
        assertThat(Arrays.asList("valid_bucket_Name", "valid_bucket_Name0"))
                .allMatch(name -> isValidBucketName(name));
    }
}
