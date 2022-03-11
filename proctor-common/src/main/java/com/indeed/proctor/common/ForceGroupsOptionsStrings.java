package com.indeed.proctor.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Owns utility functions to convert from/to a string value (force groups string)
 * that usually comes from URL query, cookie, or HTTP header.
 *
 * The format of the force groups strings is
 * <ul>
 *     <li>It's a concatenation of string elements separated by commas. (e.g., my_btn_tst1,default_fallback)</li>
 *     <li>Each element represents a forced test group, or an option.</li>
 *     <li>A forced group is specified by a test name followed by a bucket value (e.g., my_btn_tst1)</li>
 *     <li>A option is specified by predefined tokens that doesn't contain integers (e.g., default_fallback) </li>
 *     <li>If two elements conflict (e.g., specifying different buckets for the same test),
 *     the latter takes precedence</li>
 * </ul>
 */
public class ForceGroupsOptionsStrings {
    private static final Logger LOGGER = LogManager.getLogger(ForceGroupsOptionsStrings.class);

    private ForceGroupsOptionsStrings() {
    }

    @Nonnull
    public static ForceGroupsOptions parseForceGroupsString(@Nullable final String forceGroupsString) {
        final ForceGroupsOptions.Builder builder = ForceGroupsOptions.builder();
        if (forceGroupsString == null) {
            return builder.build();
        }
        // using single char in split regex avoids Pattern creation since java8
        final String[] pieces = forceGroupsString.split(",");
        // detect integer number from end of string
        for (final String rawPiece : pieces) {
            final String piece = rawPiece.trim();
            if (piece.isEmpty()) {
                continue;
            }

            ForceGroupsDefaultMode.fromToken(piece)
                    .ifPresent(builder::setDefaultMode);

            int bucketValueStart = piece.length() - 1;
            for (; bucketValueStart >= 0; bucketValueStart--) {
                if (!Character.isDigit(piece.charAt(bucketValueStart))) {
                    break;
                }
            }
            //  if no name or no value was found, it's not a valid proctor test bucket name
            if ((bucketValueStart == piece.length() - 1) || (bucketValueStart < 1)) {
                continue;
            }
            //  minus sign can only be at the beginning of a run
            if (piece.charAt(bucketValueStart) != '-') {
                bucketValueStart++;
            }
            //  bucketValueStart should now be the index of the minus sign or the first digit in a run of digits going to the end of the word
            final String testName = piece.substring(0, bucketValueStart).trim();
            final String bucketValueStr = piece.substring(bucketValueStart);
            try {
                final Integer bucketValue = Integer.valueOf(bucketValueStr);
                builder.putForceGroup(testName, bucketValue);
            } catch (final NumberFormatException e) {
                LOGGER.error("Unable to parse bucket value " + bucketValueStr + " as integer", e);
            }
        }
        return builder.build();
    }

    public static String generateForceGroupsString(final ForceGroupsOptions options) {
        final List<String> tokens = new ArrayList<>();

        // options come earlier for better visibility
        options.getDefaultMode().getToken()
                .ifPresent(tokens::add);

        options.getForceGroups()
                .forEach((testName, bucketValue) -> tokens.add(testName + bucketValue));
        return String.join(",", tokens);
    }
}
