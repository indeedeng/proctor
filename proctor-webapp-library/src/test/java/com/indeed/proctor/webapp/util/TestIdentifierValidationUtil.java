package com.indeed.proctor.webapp.util;

import com.google.common.collect.ImmutableList;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestType;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static com.indeed.proctor.webapp.util.IdentifierValidationUtil.isValidBucketName;
import static com.indeed.proctor.webapp.util.IdentifierValidationUtil.isValidMetaTag;
import static com.indeed.proctor.webapp.util.IdentifierValidationUtil.validateMetaTags;
import static com.indeed.proctor.webapp.util.IdentifierValidationUtil.validateTestName;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestIdentifierValidationUtil {

    @Test
    public void testValidateTestName() {
        final String shortName = StringUtils.repeat("a", 100);
        final String longName = StringUtils.repeat("a", 101);

        for (final String input : new String[]{"", "0", "_", "__", ".", "_0", "0_", "a_0", "0_a", "a#b"}) {
            assertThatThrownBy(() -> validateTestName(input))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Test Name must be alpha-numeric underscore and not start/end with a number");
        }
        assertThatThrownBy(() -> validateTestName(longName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Test Name length can't be longer than 100");

        // Nothing happens for valid test names
        for (final String input : new String[]{"a", "A", "a_a", "a1a", "_0_", "_0___", "_a_b_", "_a", "a_", shortName}) {
            validateTestName(input);
        }
    }

    @Test
    public void testIsValidMetaTag() {
        final String shortName = StringUtils.repeat("a", 100);
        final String longName = StringUtils.repeat("a", 101);

        final List<String> validNames = ImmutableList.of("a", "A", "a_a", "a_b", "a1a", "_0_", "_0___", "_a_b_",
                "_a", "a_", "_0", "0", "0_", "a_0", "0_a", shortName);

        for (final String input : validNames) {
            assertTrue(isValidMetaTag(input));
        }

        for (final String input : new String[]{"", "_", "__", ".", "a#b", longName}) {
            assertFalse(isValidMetaTag(input));
        }
    }

    @Test
    public void testIsValidMetaTags() {
        final TestDefinition invalidMetaTagDefinition = createTestDefinition(ImmutableList.of("0", "_", "a", "__"));
        final TestDefinition emptyMetaTagDefinition = createTestDefinition(ImmutableList.of());
        final TestDefinition validMetaTagDefinition = createTestDefinition(ImmutableList.of("a", "a_a"));

        assertThatThrownBy(() -> validateMetaTags(invalidMetaTagDefinition))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Meta Tag must \n" +
                        " - be alpha-numeric underscore\n" +
                        " - not be longer than 100\n" +
                        " but found: [_, __]");

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
                metaTags
        );
    }

    @Test
    public void testIsValidBucketName() {
        assertFalse(isValidBucketName(""));
        assertTrue(isValidBucketName("valid_bucket_Name"));
        assertTrue(isValidBucketName("valid_bucket_Name0"));
        assertFalse(isValidBucketName("0invalid_bucket_Name"));
    }
}
