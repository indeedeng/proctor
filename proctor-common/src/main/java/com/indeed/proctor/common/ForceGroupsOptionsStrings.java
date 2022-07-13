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

/**
 * Owns utility functions to convert from/to a string value (force groups string)
 * that usually comes from URL query, cookie, or HTTP header.
 *
 * The format of the force groups strings is
 * <ul>
 *     <li>It's a concatenation of string elements separated by commas. (e.g., my_btn_tst1,default_to_fallback)</li>
 *     <li>Each element represents a forced test group, or an option.</li>
 *     <li>A forced group is specified by a test name followed by a bucket value (e.g., my_btn_tst1)</li>
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
            final String[] bucketAndPayloadValuesStr = rawPiece.split(";", FORCE_PARAMETER_MAX_SIZE);
            final String piece = bucketAndPayloadValuesStr[FORCE_PARAMETER_BUCKET_IDX].trim();
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
            final String bucketValueStr = piece.substring(bucketValueStart).trim();
            try {
                final Integer bucketValue = Integer.valueOf(bucketValueStr);
                builder.putForceGroup(testName, bucketValue);
                if (bucketAndPayloadValuesStr.length == FORCE_PARAMETER_MAX_SIZE) {
                    builder.putForcePayload(testName, parseForcePayloadString(bucketAndPayloadValuesStr[FORCE_PARAMETER_PAYLOAD_IDX]));
                }
            } catch (final NumberFormatException e) {
                LOGGER.error("Unable to parse bucket value " + bucketAndPayloadValuesStr[FORCE_PARAMETER_BUCKET_IDX] + " as integer", e);
            }
        }
        return builder.build();
    }

    public static Payload parseForcePayloadString(final String payloadString )
    {
        final Payload payload = new Payload();

        final String[] splitPayloads = payloadString.split(";");
        for (final String splitPayload : splitPayloads) {
            final String[] payloadPieces = payloadString.split(":",2);
            try {
                final PayloadType payloadType = PayloadType.payloadTypeForName(payloadPieces[0]);
                final String payloadValue = payloadPieces[1].replace("[","").replace("]","");
                switch (payloadType) {
                    case DOUBLE_VALUE:
                    {
                        payload.setDoubleValue(Double.parseDouble(payloadValue));
                        break;
                    }
                    case DOUBLE_ARRAY:
                    {
                        payload.setDoubleArray( Arrays.stream(payloadValue.split(" "))
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
                        payload.setStringArray(payloadValue.replace("\"","").split(" "));
                        break;
                    }
                    case MAP:
                    {
                        final Map<String, Object> map = new HashMap<>();
                        final String[] mapPayloadPieces = payloadValue.replace("\"","").split(" ");

                        for (final String mapPayloadPiece: mapPayloadPieces) {
                            final String[] keyValuePair = mapPayloadPiece.split(":");
                            map.put(keyValuePair[0], keyValuePair[1]);
                        }

                        payload.setMap(map);
                        break;
                    }
                }
            }
            catch (final IllegalArgumentException | ArrayStoreException e) {
                return Payload.EMPTY_PAYLOAD;
            }
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
