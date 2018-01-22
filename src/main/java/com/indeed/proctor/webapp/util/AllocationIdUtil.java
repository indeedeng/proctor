package com.indeed.proctor.webapp.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.math.DoubleMath;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestDefinition;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author xiaoyun
 */
public class AllocationIdUtil {
    private static final Logger LOGGER = Logger.getLogger(AllocationIdUtil.class);
    private static final Pattern ALLOCATION_ID_PATTERN = Pattern.compile("^#([A-Z]+)(\\d+)$");

    public static String getAllocationName(final String allocId) {
        final Matcher matcher = ALLOCATION_ID_PATTERN.matcher(allocId);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            throw new IllegalStateException("Allocation id format is incorrect " + allocId);
        }
    }

    // Allocation id comparator. Compare allocation ids with format like "#A1"
    public static final Comparator<String> ALLOCATION_ID_COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            // Remove prefix and version from allocation id
            Preconditions.checkArgument((o1.length() > 2 && o2.length() > 2), "Invalid allocation id, id1: %s, id2: %s", o1, o2);
            final String id1Str = getAllocationName(o1);
            final String id2Str = getAllocationName(o2);
            final int id1Int = convertBase26ToDecimal(id1Str.toCharArray());
            final int id2Int = convertBase26ToDecimal(id2Str.toCharArray());
            return id1Int - id2Int;
        }
    };

    private static int getSegmentationChangeStartIndex(final TestDefinition previous, final TestDefinition current) {
        // Currently we can't change the order of allocations, we can only add or delete or update allocations, the following logic is based on this.
        // Test rule change
        if (!Objects.equals(previous.getRule(), current.getRule())) {
            // Update all allocation ids
            return 0;
        }
        // Test salt change
        if (!Objects.equals(previous.getSalt(), current.getSalt())) {
            // Update all allocation ids
            return 0;
        }
        // Allocation rule changed, or added / deleted allocations
        final List<Allocation> previousAllocations = previous.getAllocations();
        final List<Allocation> currentAllocations = current.getAllocations();
        for (int i = 0; i < Math.min(previousAllocations.size(), currentAllocations.size()); i++) {
            if (!currentAllocations.get(i).getId().equals(previousAllocations.get(i).getId()) || // Added / deleted allocations
                    !Objects.equals(currentAllocations.get(i).getRule(), previousAllocations.get(i).getRule())) { // Allocation rule changed
                return i;
            }
        }
        // No change
        return -1;
    }

    public static Set<Allocation> getOutdatedAllocations(final TestDefinition previous, final TestDefinition current) {
        final boolean hasAllocId = current.getAllocations().stream().anyMatch(
                x -> !StringUtils.isEmpty(x.getId())
        );
        final Set<Allocation> outdatedAllocations = new HashSet<>();
        // No existing allocation id, return
        if (!hasAllocId) {
            return outdatedAllocations;
        }

        // Check segmentation change
        final int updateFrom = getSegmentationChangeStartIndex(previous, current);
        if (updateFrom > -1) {
            for (int i = updateFrom; i < current.getAllocations().size(); i++) {
                if (!StringUtils.isEmpty(current.getAllocations().get(i).getId())) {
                    outdatedAllocations.add(current.getAllocations().get(i));
                }
            }
        }
        // Check unbalanced ratio change
        final int ratioCheckTo = updateFrom > -1 ? updateFrom : Math.min(previous.getAllocations().size(), current.getAllocations().size());
        for (int i = 0; i < ratioCheckTo; i++) {
            if (isUnbalancedRatioChange(previous.getAllocations().get(i), current.getAllocations().get(i))) {
                outdatedAllocations.add(current.getAllocations().get(i));
            }
        }
        return outdatedAllocations;
    }

    private static boolean isUnbalancedRatioChange(final Allocation previous, final Allocation current) {
        Map<Integer, Double> previousRatios = getBucketRatios(previous.getRanges());
        Map<Integer, Double> currentRatios = getBucketRatios(current.getRanges());
        final Map<Integer, Double> before = filterEmptyRatios(previousRatios);
        final Map<Integer, Double> after = filterEmptyRatios(currentRatios);
        if (!before.keySet().equals(after.keySet())) {
            return true;
        }
        if (before.isEmpty()) {
            return false;
        }
        final int firstBucket = before.keySet().iterator().next();
        final double firstRatio = after.get(firstBucket) / before.get(firstBucket);
        for (final int bucket : before.keySet()) {
            final double ratio = after.get(bucket) / before.get(bucket);
            if (!DoubleMath.fuzzyEquals(ratio, firstRatio, 1e-6)) {
                return true;
            }
        }
        return false;
    }

    private static Map<Integer, Double> getBucketRatios(final List<Range> ranges) {
        final Map<Integer, Double> result = new HashMap<>();
        for (final Range range : ranges) {
            final int bucket = range.getBucketValue();
            final double length = range.getLength();
            if ((bucket != -1) && !DoubleMath.fuzzyEquals(length, 0.0, 1e-6)) {
                final double total = result.getOrDefault(bucket, 0.0) + length;
                result.put(bucket, total);
            }
        }
        return ImmutableMap.copyOf(result);
    }

    private static Map<Integer, Double> filterEmptyRatios(final Map<Integer, Double> ratios) {
        return ratios.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * @param allocId allocation id e.g. "#A1"
     * @return next version of allocId e.g "#A1" -> "#A2"
     */
    public static String getNextVersionOfAllocationId(final String allocId) {
        final Matcher matcher = ALLOCATION_ID_PATTERN.matcher(allocId);
        if (matcher.matches()) {
            final String formerPart = matcher.group(1);
            final int version = Integer.parseInt(matcher.group(2));
            final int newVersion = version + 1;
            return "#" + formerPart + newVersion;
        } else {
            throw new IllegalStateException("Could not get the next version of allocation id for " + allocId);
        }
    }

    /**
     * @param index   a base 10 integer
     * @param version version of the allocation id
     * @return the full allocaiton id. e.g. (26, 2) returns #BA2
     */
    public static String generateAllocationId(final int index, final int version) {
        return "#" + convertDecimalToBase26(index) + version;
    }

    private static String convertDecimalToBase26(final int n) {
        final StringBuilder res = new StringBuilder();

        int number = n;
        for (; number >= 26; number /= 26) {
            final int current = number % 26;
            res.append(convertDigitToLetter(current));
        }

        return res.append(convertDigitToLetter(number)).reverse().toString();
    }

    public static int convertBase26ToDecimal(final char[] chars) {
        int sum = 0;
        for (int i = 0; i < chars.length; i++) {
            // 26 alphabetic
            sum = sum * 26 + convertLetterToDigit(chars[i]);
        }
        return sum;
    }

    private static char convertDigitToLetter(final int n) {
        Preconditions.checkArgument(n < 26, "Invalid number: %s, you can only convert number between 0 and 26 to letter", n);
        return (char) ('A' + n);
    }

    private static int convertLetterToDigit(final char c) {
        final int n = c - 'A';
        Preconditions.checkArgument((n < 26 && n >= 0), "Invalid letter: %s, the letter should be upper case A -> Z", c);
        return n;
    }
}
