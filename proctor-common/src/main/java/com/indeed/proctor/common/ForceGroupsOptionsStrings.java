package com.indeed.proctor.common;

import com.indeed.proctor.common.model.Payload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Owns utility functions to convert from/to a string value (force groups string)
 * that usually comes from URL query, cookie, or HTTP header.
 *
 * The format of the force groups strings is
 * <ul>
 *     <li>It's a concatenation of string elements separated by commas. (e.g., my_btn_tst1,default_to_fallback)</li>
 *     <li>Each element represents a forced test group, or an option.</li>
 *     <li>A forced group is specified by a test name followed by a bucket value (e.g., my_btn_tst1)</li>
 *     <li>A forced payload is specified by a force group followed by a semicolon and a payload definition (e.g., my_btn_tst1;doubleValue:0.2) </li>
 *     <li>A option is specified by predefined tokens that doesn't contain integers (e.g., default_to_fallback) </li>
 *     <li>If two elements conflict (e.g., specifying different buckets for the same test),
 *     the latter takes precedence</li>
 * </ul>
 */
public class ForceGroupsOptionsStrings {
    private static final Logger LOGGER = LogManager.getLogger(ForceGroupsOptionsStrings.class);
    private static final int FORCE_PARAMETER_BUCKET_IDX = 0;
    private static final int FORCE_PARAMETER_PAYLOAD_IDX = 1;
    private static final int FORCE_PARAMETER_MAX_SIZE = 2;

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
            // split string to separate force group and payload
            final String[] bucketAndPayloadValuesStr = rawPiece.split(";", FORCE_PARAMETER_MAX_SIZE);
            final String groupString = bucketAndPayloadValuesStr[FORCE_PARAMETER_BUCKET_IDX].trim();
            if (groupString.isEmpty()) {
                continue;
            }

            ForceGroupsDefaultMode.fromToken(groupString)
                    .ifPresent(builder::setDefaultMode);

            final Optional<Integer> bucketValueStart = getBucketValueStart(groupString);

            if (bucketValueStart.isPresent()) {
                //  bucketValueStart should now be the index of the minus sign or the first digit in a run of digits going to the end of the word
                final String testName = groupString.substring(0, bucketValueStart.get()).trim();
                final String bucketValueStr = groupString.substring(bucketValueStart.get()).trim();
                try {
                    final Integer bucketValue = Integer.valueOf(bucketValueStr);
                    builder.putForceGroup(testName, bucketValue);
                    if (bucketAndPayloadValuesStr.length == FORCE_PARAMETER_MAX_SIZE) {
                        final Payload payloadValue = parseForcePayloadString(bucketAndPayloadValuesStr[FORCE_PARAMETER_PAYLOAD_IDX]);
                        if (payloadValue != null) {
                            builder.putForcePayload(testName, payloadValue);
                        }
                    }
                } catch (final NumberFormatException e) {
                    LOGGER.error("Unable to parse bucket value " + bucketValueStr + " as integer", e);
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
     * <ul>
     *     <li>payloadType:payloadValue</li>
     *     <li>Where payloadType is one of the 7 payload types supported by Proctor (stringValue, stringArray, doubleValue, longValue, longArray, map)</li>
     *     <li>If payloadValue is an array is expected in the following format: [value value value]</li>
     *     <li>If payloadValue is a map expecting following format: [key:value key:value key:value]</li>
     * </ul>
     */
    @Nullable
    public static Payload parseForcePayloadString(final String payloadString )
    {
        final Payload payload = new Payload();

        final String[] payloadPieces = payloadString.split(":",2);

        try {
            final PayloadType payloadType = PayloadType.payloadTypeForName(payloadPieces[0]);
            String payloadValue = payloadPieces[1];
            switch (payloadType) {
                case DOUBLE_VALUE:
                {
                    payload.setDoubleValue(Double.parseDouble(payloadValue));
                    break;
                }
                case DOUBLE_ARRAY:
                {
                    payloadValue = payloadValue.replace("[","").replace("]","");
                    payload.setDoubleArray(Arrays.stream(payloadValue.split(" "))
                            .map(Double::valueOf)
                            .toArray(Double[]::new) );
                    break;
                }
                case LONG_VALUE:
                {
                    payload.setLongValue(Long.parseLong(payloadValue));
                    break;
                }
                case LONG_ARRAY:
                {
                    payloadValue = payloadValue.replace("[","").replace("]","");
                    payload.setLongArray( Arrays.stream(payloadValue.split(" "))
                            .map(Long::valueOf)
                            .toArray(Long[]::new) );
                    break;
                }
                case STRING_VALUE:
                {
                    payload.setStringValue(payloadValue.replace("\"",""));
                    break;
                }
                case STRING_ARRAY:
                {
                    payloadValue = payloadValue.replace("[","").replace("]","");
                    payload.setStringArray(payloadValue.replace("\"","").split(" "));
                    break;
                }
                case MAP:
                {
                    // Remove outside brackets e.g. ([map : values])
                    payloadValue = payloadValue.substring(1,payloadValue.length()-1);

                    // Parse each entry of map and add to payload map, which inserts all values as strings later validated against actual test
                    final Map<String, Object> map = new HashMap<>();
                    final List<String> mapPayloadPieces = new ArrayList<>();
                    boolean indexInArray = false;
                    int startIndex = 0;
                    for (int payloadIdx = 0; payloadIdx < payloadValue.length(); payloadIdx++) {
                        // if value is an array ignore whitespace inside array brackets
                        if (payloadValue.charAt(payloadIdx) == '[') {
                            indexInArray = true;
                        } else if (payloadValue.charAt(payloadIdx) == ']') {
                            indexInArray = false;
                        }
                        if ((payloadValue.charAt(payloadIdx) == ' ' || payloadIdx == payloadValue.length()-1) && !indexInArray) {
                            final String mapPayloadPiece = payloadValue
                                    .substring(startIndex,payloadIdx)
                                    .replace("[","")
                                    .replace("]","")
                                    .replace("\"","");
                            final String[] keyValuePair = mapPayloadPiece.split(":");
                            map.put(keyValuePair[0], keyValuePair[1]);
                            startIndex = payloadIdx+1;
                        }
                    }
                    payload.setMap(map);
                    break;
                }
            }
        }
        catch (final IllegalArgumentException | ArrayStoreException | ClassCastException e) {
            return null;
        }

        return payload;
    }

    public static String generateForceGroupsString(final ForceGroupsOptions options) {
        final List<String> tokens = new ArrayList<>();

        // options come earlier for better visibility
        options.getDefaultMode().getToken()
                .ifPresent(tokens::add);

        final Map<String, Payload> forcePayloads = options.getForcePayloads();

        options.getForceGroups()
                .forEach((testName, bucketValue) ->
                        tokens.add( forcePayloads.containsKey(testName) ? testName + bucketValue + ";" + forcePayloads.get(testName) : testName + bucketValue));
        return String.join(",", tokens);
    }
}
