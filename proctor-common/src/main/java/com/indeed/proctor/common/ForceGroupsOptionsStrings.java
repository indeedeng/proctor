package com.indeed.proctor.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.proctor.common.model.Payload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Owns utility functions to convert from/to a string value (force groups string) that usually comes
 * from URL query, cookie, or HTTP header.
 *
 * <p>The format of the force groups strings is
 *
 * <ul>
 *   <li>It's a concatenation of string elements separated by commas. (e.g.,
 *       my_btn_tst1,default_to_fallback)
 *   <li>Each element represents a forced test group, or an option.
 *   <li>A forced group is specified by a test name followed by a bucket value (e.g., my_btn_tst1)
 *   <li>A forced payload is specified by a force group followed by a semicolon and a payload
 *       definition (e.g., my_btn_tst1;doubleValue:0.2)
 *   <li>A option is specified by predefined tokens that doesn't contain integers (e.g.,
 *       default_to_fallback)
 *   <li>If two elements conflict (e.g., specifying different buckets for the same test), the latter
 *       takes precedence
 * </ul>
 */
public class ForceGroupsOptionsStrings {
    private static final Logger LOGGER = LogManager.getLogger(ForceGroupsOptionsStrings.class);
    private static final int FORCE_PARAMETER_BUCKET_IDX = 0;
    private static final int FORCE_PARAMETER_PAYLOAD_IDX = 1;
    private static final int FORCE_PARAMETER_MAX_SIZE = 2;

    // -?\d+\.?\d* - matches a integer or double and allows negatives
    // or
    // \"(?:\\.|[^\"\\]+)*\"- matches a string
    private static final String REGEX_STRING_OR_NUM =
            "-?\\d+\\.?\\d*|\\\"(?:\\\\.|[^\\\"\\\\]+)*\\\"";

    // matches an array of string or numbers
    private static final String REGEX_ARRAY =
            "\\["
                    + "(?:\\s*"
                    + "(?:"
                    + REGEX_STRING_OR_NUM
                    + ")"
                    + "(?:\\s*,\\s*"
                    + "(?:"
                    + REGEX_STRING_OR_NUM
                    + ")"
                    + ")*"
                    + ")?"
                    + "\\]";

    // matches a map of key type string and value of array, string, or number
    private static final String REGEX_MAP =
            "\\{"
                    + "(?:\\s*"
                    + "(?:\\\"(?:\\\\.|[^\\\"\\\\]+)*\\\"\\s*:\\s*)"
                    + "(?:"
                    + REGEX_STRING_OR_NUM
                    + "|"
                    + REGEX_ARRAY
                    + ")"
                    + "(?:\\s*,\\s*"
                    + "(?:\\\"(?:\\\\.|[^\\\"\\\\]+)*\\\"\\s*:\\s*)"
                    + "(?:"
                    + REGEX_STRING_OR_NUM
                    + "|"
                    + REGEX_ARRAY
                    + ")"
                    + ")*"
                    + ")?"
                    + "\\}";

    // \w+-?\d+ - matches force group without payload (ie. example_tst1 or another_tst-1)
    //      ;\w+: - matches ; and payload type (ie. ;stringValue:) followed by one of the following:
    //          string or number
    //          array
    //          map
    // [a-z_]+ - matches default value (ie. default_to_min_live)
    private static final Pattern PATTERN =
            Pattern.compile(
                    "\\w+-?\\d+"
                            + "(?:"
                            + ";\\w+:"
                            + "(?:"
                            + REGEX_STRING_OR_NUM
                            + "|"
                            + REGEX_ARRAY
                            + "|"
                            + REGEX_MAP
                            + ")"
                            + ")?"
                            + "|[a-z_]+");

    private static final TypeReference<Map<String, Object>> TYPE_REFERENCE =
            new TypeReference<Map<String, Object>>() {};

    private ForceGroupsOptionsStrings() {}

    @Nonnull
    public static ForceGroupsOptions parseForceGroupsString(
            @Nullable final String forceGroupsString) {
        return parseForceGroupsString(forceGroupsString, Collections.emptySet());
    }

    @Nonnull
    public static ForceGroupsOptions parseForceGroupsString(
            @Nullable final String forceGroupsString, final Set<String> forcePayloadTests) {
        final ForceGroupsOptions.Builder builder = ForceGroupsOptions.builder();
        if (forceGroupsString == null) {
            return builder.build();
        }
        final Matcher matcher = PATTERN.matcher(forceGroupsString);
        while (matcher.find()) {
            final int endOfMatch = matcher.end();
            // continue if match has extra characters following it (ie. example_tst1-)
            if (endOfMatch < forceGroupsString.length()
                    && forceGroupsString.charAt(endOfMatch) != ',') {
                continue;
            }
            // split string to separate force group and payload
            final String match = matcher.group();
            final String[] bucketAndPayloadValuesStr = match.split(";", FORCE_PARAMETER_MAX_SIZE);
            final String groupString = bucketAndPayloadValuesStr[FORCE_PARAMETER_BUCKET_IDX].trim();

            if (groupString.isEmpty()) {
                continue;
            }

            ForceGroupsDefaultMode.fromToken(groupString).ifPresent(builder::setDefaultMode);

            final Optional<Integer> bucketValueStart = getBucketValueStart(groupString);

            if (bucketValueStart.isPresent()) {
                //  bucketValueStart should now be the index of the minus sign or the first digit in
                // a run of digits going to the end of the word
                final String testName = groupString.substring(0, bucketValueStart.get()).trim();
                final String bucketValueStr = groupString.substring(bucketValueStart.get()).trim();
                try {
                    final Integer bucketValue = Integer.valueOf(bucketValueStr);
                    builder.putForceGroup(testName, bucketValue);
                    if (bucketAndPayloadValuesStr.length == FORCE_PARAMETER_MAX_SIZE
                            && forcePayloadTests.contains(testName)) {
                        final Payload payloadValue =
                                parseForcePayloadString(
                                        bucketAndPayloadValuesStr[FORCE_PARAMETER_PAYLOAD_IDX]);
                        if (payloadValue != null) {
                            builder.putForcePayload(testName, payloadValue);
                        }
                    }
                } catch (final NumberFormatException e) {
                    LOGGER.error(
                            "Unable to parse bucket value " + bucketValueStr + " as integer", e);
                }
            }
        }
        return builder.build();
    }

    private static Optional<Integer> getBucketValueStart(final String groupString) {
        int bucketValueStart = groupString.length() - 1;
        for (; bucketValueStart >= 0; bucketValueStart--) {
            if (!Character.isDigit(groupString.charAt(bucketValueStart))) {
                break;
            }
        }
        //  if no name or no value was found, it's not a valid proctor test bucket name
        if ((bucketValueStart == groupString.length() - 1) || (bucketValueStart < 1)) {
            return Optional.empty();
        }
        //  minus sign can only be at the beginning of a run
        if (groupString.charAt(bucketValueStart) != '-') {
            bucketValueStart++;
        }

        return Optional.of(bucketValueStart);
    }

    /**
     * The format of the payloadString is the following:
     *
     * <ul>
     *   <li>payloadType:payloadValue
     *   <li>Where payloadType is one of the 7 payload types supported by Proctor (stringValue,
     *       stringArray, doubleValue, longValue, longArray, map)
     *   <li>If payloadValue is an array is expected in the following format: [value,value,value]
     *   <li>If payloadValue is a map expecting following format: [key:value,key:value,key:value]
     * </ul>
     */
    @Nullable
    public static Payload parseForcePayloadString(final String payloadString) {
        final Payload payload = new Payload();
        final String[] payloadPieces = payloadString.split(":", 2);
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            final PayloadType payloadType = PayloadType.payloadTypeForName(payloadPieces[0]);
            final String payloadValue = payloadPieces[1];
            switch (payloadType) {
                case DOUBLE_VALUE:
                    {
                        payload.setDoubleValue(objectMapper.readValue(payloadValue, Double.class));
                        break;
                    }
                case DOUBLE_ARRAY:
                    {
                        payload.setDoubleArray(
                                objectMapper.readValue(payloadValue, Double[].class));
                        break;
                    }
                case LONG_VALUE:
                    {
                        payload.setLongValue(objectMapper.readValue(payloadValue, Long.class));
                        break;
                    }
                case LONG_ARRAY:
                    {
                        payload.setLongArray(objectMapper.readValue(payloadValue, Long[].class));
                        break;
                    }
                case STRING_VALUE:
                    {
                        payload.setStringValue(objectMapper.readValue(payloadValue, String.class));
                        break;
                    }
                case STRING_ARRAY:
                    {
                        payload.setStringArray(
                                objectMapper.readValue(payloadValue, String[].class));
                        break;
                    }
                case MAP:
                    {
                        payload.setMap(objectMapper.readValue(payloadValue, TYPE_REFERENCE));
                        break;
                    }
                case JSON:
                    {
                        // Json not currently supported
                        return null;
                    }
            }
        } catch (final IllegalArgumentException
                | ArrayStoreException
                | ClassCastException
                | IOException e) {
            return null;
        }

        return payload;
    }

    public static String generateForceGroupsString(final ForceGroupsOptions options) {
        final List<String> tokens = new ArrayList<>();

        // options come earlier for better visibility
        options.getDefaultMode().getToken().ifPresent(tokens::add);

        final Map<String, Payload> forcePayloads = options.getForcePayloads();

        options.getForceGroups()
                .forEach(
                        (testName, bucketValue) ->
                                tokens.add(
                                        forcePayloads.containsKey(testName)
                                                ? testName
                                                        + bucketValue
                                                        + ";"
                                                        + createForcePayloadString(
                                                                forcePayloads.get(testName))
                                                : testName + bucketValue));
        return String.join(",", tokens);
    }

    @Nonnull
    private static String createForcePayloadString(final Payload payload) {
        final StringBuilder s = new StringBuilder(50);
        final ObjectMapper objectMapper = new ObjectMapper();
        try {
            if (payload.getDoubleValue() != null) {
                final String doubleValue =
                        objectMapper.writeValueAsString(payload.getDoubleValue());
                s.append("doubleValue:").append(doubleValue);
            }
            if (payload.getDoubleArray() != null) {
                final String doubleArray =
                        objectMapper.writeValueAsString(payload.getDoubleArray());
                s.append("doubleArray:").append(doubleArray);
            }
            if (payload.getLongValue() != null) {
                final String longValue = objectMapper.writeValueAsString(payload.getLongValue());
                s.append("longValue:").append(longValue);
            }
            if (payload.getLongArray() != null) {
                final String longArray = objectMapper.writeValueAsString(payload.getLongArray());
                s.append("longArray:").append(longArray);
            }
            if (payload.getStringValue() != null) {
                final String stringValue =
                        objectMapper.writeValueAsString(payload.getStringValue());
                s.append("stringValue:").append(stringValue);
            }
            if (payload.getStringArray() != null) {
                final String stringArray =
                        objectMapper.writeValueAsString(payload.getStringArray());
                s.append("stringArray:").append(stringArray);
            }
            if (payload.getMap() != null) {
                final String mapValue = objectMapper.writeValueAsString(payload.getMap());
                s.append("map:").append(mapValue);
            }
            return s.toString();
        } catch (final JsonProcessingException e) {
            return "";
        }
    }
}
