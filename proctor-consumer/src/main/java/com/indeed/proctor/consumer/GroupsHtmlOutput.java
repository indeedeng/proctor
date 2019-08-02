package com.indeed.proctor.consumer;

import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestBucket;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Map;

public class GroupsHtmlOutput {
    private final String output;
    public GroupsHtmlOutput(final AbstractGroups groups) {
        this(groups, "");
    }

    public GroupsHtmlOutput(final AbstractGroups groups, final String extraGroups) {
        final String[] pieces = (groups + "," + extraGroups).split(" *,+ *");
        Arrays.sort(pieces);

        final StringBuilder sb = new StringBuilder("<ul class=\"bucketList\">");
        final ProctorResult proctorResult = groups.getProctorResult();
        final Map<String, ConsumableTestDefinition> testDefinitions = proctorResult.getTestDefinitions();
        final Map<String, TestBucket> buckets = proctorResult.getBuckets();
        for (int i = 0; i < pieces.length; i++) {
            final String group = pieces[i];

            int bucketValueStart = group.length() - 1;
            for (; bucketValueStart >= 0; bucketValueStart--) {
                if (! Character.isDigit(group.charAt(bucketValueStart))) {
                    break;
                }
            }
            final String title;
            if ((bucketValueStart == group.length() - 1) || (bucketValueStart < 1)) {
                title = null;
            } else {
                //  minus sign can only be at the beginning of a run
                if (group.charAt(bucketValueStart) != '-') {
                    bucketValueStart++;
                }
                //  bucketValueStart should now be the index of the minus sign or the first digit in a run of digits going to the end of the word
                final String testName = group.substring(0, bucketValueStart).trim();

                final TestBucket testBucket = buckets.get(testName);
                if (testBucket == null) {
                    title = null;
                } else {
                    final ConsumableTestDefinition testDefinition = testDefinitions.get(testName);
                    if (testDefinition == null) {
                        title = null;
                    } else {
                        final StringBuilder titleBuilder = new StringBuilder(testName)
                                                                    .append(": ")
                                                                    .append(testDefinition.getDescription());
                        for (final TestBucket anotherTestBucket : testDefinition.getBuckets()) {
                            titleBuilder.append("\n")
                                        .append(anotherTestBucket.getValue())
                                        .append(": ")
                                        .append(anotherTestBucket.getName());
                            final String description = anotherTestBucket.getDescription();
                            if (StringUtils.isNotBlank(description)) {
                                titleBuilder.append(" - ")
                                            .append(description);
                            }
                        }
                        title = titleBuilder.toString();
                    }

                }
            }
            sb.append("<li class=\"testBucket\"");
            if (title != null) {
                sb.append(" title=\"").append(StringEscapeUtils.escapeHtml4(title).replaceAll("\"", "\\\"")).append("\"");
            }
            sb.append(">").append(group).append(",</li>");
        }
        sb.append("</ul>");
        this.output = sb.toString();
    }

    @Override
    public String toString() {
        return output;
    }

}
