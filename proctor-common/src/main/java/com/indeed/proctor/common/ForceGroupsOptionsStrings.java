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
import java.util.Set;

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
    public static ForceGroupsOptions parseForceGroupsString(@Nullable final String forceGroupsString, final Set<String> forcePayloadTests) {
        final ForceGroupsOptions.Builder builder = ForceGroupsOptions.builder();
        if (forceGroupsString == null) {
            return builder.build();
        }

        int startIndex = 0, numQuotes = 0;
        final List<Character> brackets = new ArrayList<>();

        // iterate through string to find force groups and force payloads
        for (int forceGroupIndex = 0; forceGroupIndex < forceGroupsString.length(); forceGroupIndex++) {
            if (forceGroupsString.charAt(forceGroupIndex) == '[' || forceGroupsString.charAt(forceGroupIndex) == '{') {
                brackets.add(forceGroupsString.charAt(forceGroupIndex));
            } else if (forceGroupsString.charAt(forceGroupIndex) == ']' || forceGroupsString.charAt(forceGroupIndex) == '}') {
                brackets.remove(brackets.size()-1);
            } if (forceGroupsString.charAt(forceGroupIndex) == '"') {
                numQuotes++;
            }
            if ((forceGroupsString.charAt(forceGroupIndex) == ',' || forceGroupIndex == forceGroupsString.length()-1) && brackets.isEmpty() && (numQuotes % 2 == 0)) {
                // Add 1 to index if end of string because substring endIndex is exclusive
                if(forceGroupIndex == forceGroupsString.length()-1) {
                    forceGroupIndex++;
                }
                // split string to separate force group and payload
                final String[] bucketAndPayloadValuesStr = forceGroupsString
                        .substring(startIndex, forceGroupIndex)
                        .split(";", FORCE_PARAMETER_MAX_SIZE);

                final String groupString = bucketAndPayloadValuesStr[FORCE_PARAMETER_BUCKET_IDX].trim();

                startIndex = forceGroupIndex+1;

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
                        if (bucketAndPayloadValuesStr.length == FORCE_PARAMETER_MAX_SIZE && forcePayloadTests.contains(testName)) {
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
     *     <li>If payloadValue is an array is expected in the following format: [value,value,value]</li>
     *     <li>If payloadValue is a map expecting following format: [key:value,key:value,key:value]</li>
     * </ul>
     */
    @Nullable
    public static Payload parseForcePayloadString(final String payloadString )
    {
        final Payload payload = new Payload();
        final String[] payloadPieces = payloadString.split(":",2);
        try {
            final PayloadType payloadType = PayloadType.payloadTypeForName(payloadPieces[0]);
            final String payloadValue = payloadPieces[1];
            switch (payloadType) {
                case DOUBLE_VALUE:
                {
                    payload.setDoubleValue(Double.parseDouble(payloadValue));
                    break;
                }
                case DOUBLE_ARRAY:
                {
                    payload.setDoubleArray(Arrays.stream(getPayloadArray(payloadValue))
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
                    payload.setLongArray(Arrays.stream(getPayloadArray(payloadValue))
                            .map(Long::valueOf)
                            .toArray(Long[]::new));
                    break;
                }
                case STRING_VALUE:
                {
                    // Remove quotes
                    payload.setStringValue(payloadValue.substring(1,payloadValue.length()-1));
                    break;
                }
                case STRING_ARRAY:
                {
                    // Remove outside brackets e.g. ([abc, def, ...])
                    payload.setStringArray(getPayloadStringArray(payloadValue.substring(1,payloadValue.length()-1)));
                    break;
                }
                case MAP:
                {
                    // Remove outside brackets e.g. (map:[keys:values, ...])
                    payload.setMap(getPayloadMap(payloadValue.substring(1,payloadValue.length()-1)));
                    break;
                }
            }
        }
        catch (final IllegalArgumentException | ArrayStoreException | ClassCastException e) {
            return null;
        }

        return payload;
    }

    public static String[] getPayloadArray(final String payloadValue) {
        // Remove outside brackets e.g. ([0, 1, 2]) then split array
        return payloadValue
                .substring(1,payloadValue.length()-1)
                .split(",");
    }

    public static String[] getPayloadStringArray(final String payloadValue) {
        int startIndex = 0;
        int numQuotes = 0;
        final List<String> payloadList = new ArrayList<>();
        for (int payloadValueIndex = 0; payloadValueIndex < payloadValue.length(); payloadValueIndex++) {
            // if char is inside of quotes do not split string
            if (payloadValue.charAt(payloadValueIndex) == '"') {
                numQuotes++;
            }
            if ((payloadValue.charAt(payloadValueIndex) == ',' || payloadValueIndex == payloadValue.length()-1) && (numQuotes % 2 == 0)) {
                if(payloadValueIndex == payloadValue.length()-1) {
                    payloadValueIndex++;
                }
                String toAdd = payloadValue.substring(startIndex,payloadValueIndex);
                // Remove quotes at beginning/end of string
                if(toAdd.startsWith("\"")) {
                    toAdd = toAdd.substring(1);
                }
                if(toAdd.endsWith("\"")) {
                    toAdd = toAdd.substring(0,toAdd.length()-1);
                }
                payloadList.add(toAdd);
                startIndex = payloadValueIndex+1;
            }
        }
        return payloadList.toArray(new String[0]);
    }

    private static Map<String, Object> getPayloadMap(final String payloadValue) {
        // Parse each entry of map and add to payload map, which inserts all values as strings later validated against actual test
        final Map<String, Object> map = new HashMap<>();
        boolean indexInArray = false;
        int startIndex = 0, numQuotes = 0;
        for (int payloadIdx = 0; payloadIdx < payloadValue.length(); payloadIdx++) {
            // if value is an array/string ignore comma inside array square brackets/quotes
            if (payloadValue.charAt(payloadIdx) == '[') {
                indexInArray = true;
            } else if (payloadValue.charAt(payloadIdx) == ']') {
                indexInArray = false;
            } else if (payloadValue.charAt(payloadIdx) == '"') {
                numQuotes++;
            }
            if ((payloadValue.charAt(payloadIdx) == ',' || payloadIdx == payloadValue.length()-1) && !indexInArray && (numQuotes % 2 == 0)) {
                if(payloadIdx == payloadValue.length()-1) {
                    payloadIdx++;
                }
                // limit split to 2 for the key, value pair
                final String[] keyValuePair = payloadValue
                        .substring(startIndex,payloadIdx)
                        .split(":",2);
                map.put(keyValuePair[0].trim().replace("\"",""), keyValuePair[1]);
                startIndex = payloadIdx+1;
            }
        }

        return map;
    }

    public static String generateForceGroupsString(final ForceGroupsOptions options) {
        final List<String> tokens = new ArrayList<>();

        // options come earlier for better visibility
        options.getDefaultMode().getToken()
                .ifPresent(tokens::add);

        final Map<String, Payload> forcePayloads = options.getForcePayloads();

        options.getForceGroups()
                .forEach((testName, bucketValue) ->
                        tokens.add( forcePayloads.containsKey(testName) ? testName + bucketValue + ";" +
                                forcePayloads.get(testName).toCookieString() : testName + bucketValue));
        return String.join(",", tokens);
    }
}
