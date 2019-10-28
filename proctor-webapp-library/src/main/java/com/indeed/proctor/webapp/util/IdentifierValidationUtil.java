package com.indeed.proctor.webapp.util;

import com.google.common.annotations.VisibleForTesting;
import com.indeed.proctor.common.model.TestDefinition;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class IdentifierValidationUtil {
    private static final Pattern VALID_TEST_NAME_PATTERN = Pattern.compile("^([a-z]([a-z0-9_]*[a-z_])?|_+([a-z]|[a-z0-9][a-z0-9_]*[a-z_]))$", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALID_META_TAG_PATTERN = Pattern.compile("^_*[a-z0-9][a-z0-9_]*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALID_BUCKET_NAME_PATTERN = Pattern.compile("^[a-z_][a-z0-9_]*$", Pattern.CASE_INSENSITIVE);
    private static final int TEST_NAME_LENGTH_LIMIT = 100;
    private static final int META_TAG_LENGTH_LIMIT = 100;

    public static void validateMetaTags(final TestDefinition definition) {
        final List<String> invalidMetaTags = definition.getMetaTags()
                .stream()
                .filter((metaTag) -> !isValidMetaTag(metaTag))
                .collect(Collectors.toList());

        if (!invalidMetaTags.isEmpty()) {
            throw new IllegalArgumentException(
                    "Meta Tag must \n" +
                            " - be alpha-numeric underscore\n" +
                            " - not be longer than " + META_TAG_LENGTH_LIMIT + "\n" +
                            " but found: " + invalidMetaTags
            );
        }
    }

    /**
     * Check the testName if it passes following conditions
     *   - testName does not start/end with a number
     *   - testName is not only underscores
     *   - testName is not longer than {@value #TEST_NAME_LENGTH_LIMIT}
     * The reason why it can't start/end with a number is that proctor will add a bucket number at the end of the test name and Java class name can't start with a number.
     * And testName can't be longer than {@value #TEST_NAME_LENGTH_LIMIT} because testName can be used in SQL database columns with length restrictions.
     */
    public static void validateTestName(final String testName) {
        final Matcher m = VALID_TEST_NAME_PATTERN.matcher(testName);
        if (!m.matches()) {
            throw new IllegalArgumentException("Test Name must be alpha-numeric underscore and not start/end with a number, found: '" + testName + "'");
        }

        if (testName.length() > TEST_NAME_LENGTH_LIMIT) {
            throw new IllegalArgumentException("Test Name length can't be longer than " + TEST_NAME_LENGTH_LIMIT +
                    ", found: " + testName.length());
        }
    }

    /**
     * Check the metaTag if it passes following conditions
     *   - metaTag is not only underscores
     *   - metaTag is not longer than {@value #META_TAG_LENGTH_LIMIT}
     * metaTag can't be longer than {@value #META_TAG_LENGTH_LIMIT} because metaTag can be used in SQL database columns with length restrictions.
     */
    @VisibleForTesting
    static boolean isValidMetaTag(final String metaTag) {
        final Matcher m = VALID_META_TAG_PATTERN.matcher(metaTag);
        return m.matches() && metaTag.length() <= META_TAG_LENGTH_LIMIT;
    }

    public static boolean isValidBucketName(final String bucketName) {
        final Matcher m = VALID_BUCKET_NAME_PATTERN.matcher(bucketName);
        return m.matches();
    }
}
