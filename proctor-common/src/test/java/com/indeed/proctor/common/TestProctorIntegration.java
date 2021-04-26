package com.indeed.proctor.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestDependency;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.proctor.common.model.TestType;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.junit.Assert.assertEquals;

/**
 * More of an integration test than a unit test
 * @author ketan
 *
 */
public class TestProctorIntegration {
    @Test
    public void test() {
        final Proctor proctor = constructProctor();

        assertEquals("",                calcGroups(proctor, "piafdsff", "AR", "en", 9));
        assertEquals("def2",            calcGroups(proctor, "piafdsff", "IO", "en", 9));
        assertEquals("def2",            calcGroups(proctor, "piafdsff", "UY", "en", 9));
        assertEquals("ghi3",            calcGroups(proctor, "piafdsff", "AR", "pt", 9));
        assertEquals("def2,ghi3",       calcGroups(proctor, "piafdsff", "IO", "pt", 9));
        assertEquals("def2,ghi3",       calcGroups(proctor, "piafdsff", "UY", "pt", 9));
        assertEquals("abc0,jkl1",       calcGroups(proctor, "piafdsff", "AR", "en", 4));
        assertEquals("abc0,def2,jkl1",  calcGroups(proctor, "piafdsff", "IO", "en", 4));
        assertEquals("abc0,def2,jkl1",  calcGroups(proctor, "piafdsff", "UY", "en", 4));
        assertEquals("abc0,ghi3",       calcGroups(proctor, "piafdsff", "AR", "pt", 4));
        assertEquals("abc0,def2,ghi3",  calcGroups(proctor, "piafdsff", "IO", "pt", 4));
        assertEquals("abc0,def2,ghi3",  calcGroups(proctor, "piafdsff", "UY", "pt", 4));

        assertEquals("",                calcGroups(proctor, "8719568712", "AR", "en", 9));
        assertEquals("def2",            calcGroups(proctor, "8719568712", "IO", "en", 9));
        assertEquals("def2",            calcGroups(proctor, "8719568712", "UY", "en", 9));
        assertEquals("ghi1",            calcGroups(proctor, "8719568712", "AR", "pt", 9));
        assertEquals("def2,ghi1",       calcGroups(proctor, "8719568712", "IO", "pt", 9));
        assertEquals("def2,ghi1",       calcGroups(proctor, "8719568712", "UY", "pt", 9));
        assertEquals("abc0,jkl2",       calcGroups(proctor, "8719568712", "AR", "en", 4));
        assertEquals("abc0,def2,jkl2",  calcGroups(proctor, "8719568712", "IO", "en", 4));
        assertEquals("abc0,def2,jkl2",  calcGroups(proctor, "8719568712", "UY", "en", 4));
        assertEquals("abc0,ghi1",       calcGroups(proctor, "8719568712", "AR", "pt", 4));
        assertEquals("abc0,def2,ghi1",  calcGroups(proctor, "8719568712", "IO", "pt", 4));
        assertEquals("abc0,def2,ghi1",  calcGroups(proctor, "8719568712", "UY", "pt", 4));

        assertEquals("",                calcGroups(proctor, "aksdh8947jh4807", "AR", "en", 9));
        assertEquals("def0",            calcGroups(proctor, "aksdh8947jh4807", "IO", "en", 9));
        assertEquals("def0",            calcGroups(proctor, "aksdh8947jh4807", "UY", "en", 9));
        assertEquals("",                calcGroups(proctor, "aksdh8947jh4807", "AR", "pt", 9));
        assertEquals("def0",            calcGroups(proctor, "aksdh8947jh4807", "IO", "pt", 9));
        assertEquals("def0",            calcGroups(proctor, "aksdh8947jh4807", "UY", "pt", 9));
        assertEquals("abc0,jkl2",       calcGroups(proctor, "aksdh8947jh4807", "AR", "en", 4));
        assertEquals("abc0,def0,jkl2",  calcGroups(proctor, "aksdh8947jh4807", "IO", "en", 4));
        assertEquals("abc0,def0,jkl2",  calcGroups(proctor, "aksdh8947jh4807", "UY", "en", 4));
        assertEquals("abc0",            calcGroups(proctor, "aksdh8947jh4807", "AR", "pt", 4));
        assertEquals("abc0,def0",       calcGroups(proctor, "aksdh8947jh4807", "IO", "pt", 4));
        assertEquals("abc0,def0",       calcGroups(proctor, "aksdh8947jh4807", "UY", "pt", 4));
    }

    private Proctor constructProctor() {
        final TestMatrixArtifact matrix = new TestMatrixArtifact();

        final Audit audit = new Audit();
        audit.setUpdated(0);
        audit.setUpdatedBy("nobody");
        audit.setVersion(Audit.EMPTY_VERSION);

        matrix.setAudit(audit);

        final TestBucket abcBucket0 = new TestBucket("always", 0, "always description", null);
        final List<TestBucket> abcBuckets = Lists.newArrayList(abcBucket0);
        final Map<String, Object> abcTestConstants = Maps.newHashMap();
        abcTestConstants.put("ANOTHER_NUM", Integer.valueOf(5));
        abcTestConstants.put("SOME_NUM", Integer.valueOf(1));

        final List<Allocation> abcAllocations = Collections.singletonList(new Allocation(null, Arrays.asList(new Range[] { new Range(abcBucket0.getValue(), 1) })));

        final ConsumableTestDefinition abcTD = new ConsumableTestDefinition("1", "${num > SOME_NUM && num < ANOTHER_NUM}", TestType.ANONYMOUS_USER, "abcsalt", abcBuckets, abcAllocations, abcTestConstants, "zingle boppity zip zop");

        final TestBucket defBucket0 = new TestBucket("control", 0, "control description", null);
        final TestBucket defBucket1 = new TestBucket("test1", 1, "test1 description", null);
        final TestBucket defBucket2 = new TestBucket("test2", 2, "test2 description", null);
        final List<TestBucket> defBuckets = Lists.newArrayList(defBucket0, defBucket1, defBucket2);
        final Map<String, Object> defTestConstants = Maps.newHashMap();
        defTestConstants.put("T", Boolean.TRUE);
        defTestConstants.put("COUNTRIES", Sets.newHashSet("AE", "IO", "UY"));
        final List<Allocation> defAllocations = Collections.singletonList(new Allocation(null, Arrays.asList(new Range[] {
                new Range(defBucket0.getValue(), 1/3f),
                new Range(defBucket1.getValue(), 1/3f),
                new Range(defBucket2.getValue(), 1/3f),
        })));
        final ConsumableTestDefinition defTD = new ConsumableTestDefinition("2", "${proctor:contains(COUNTRIES, country) && T}", TestType.ANONYMOUS_USER, "defsalt", defBuckets, defAllocations, defTestConstants, "finkle fangle foop");

        final TestBucket ghiBucket0 = new TestBucket("inactive", -1, "inactive description", null);
        final TestBucket ghiBucket1 = new TestBucket("control", 0, "control desc", null);
        final TestBucket ghiBucket2 = new TestBucket("optionA", 1, "option A desc", null);
        final TestBucket ghiBucket3 = new TestBucket("optionB", 2, "option B desc", null);
        final TestBucket ghiBucket4 = new TestBucket("optionC", 3, "option C desc", null);
        final List<TestBucket> ghiBuckets = Lists.newArrayList(ghiBucket0, ghiBucket1, ghiBucket2, ghiBucket3, ghiBucket4);
        final Map<String, Object> ghiTestConstants = Maps.newHashMap();
        ghiTestConstants.put("LANGUAGES", Sets.newHashSet("es", "fr", "pt", "nl"));
        ghiTestConstants.put("COUNTRIES", Sets.newHashSet("AE", "IO", "UY"));
        final List<Allocation> ghiAllocations = Collections.singletonList(new Allocation(null,
                Arrays.asList(new Range[] {
                        new Range(ghiBucket0.getValue(), 0.2),
                        new Range(ghiBucket1.getValue(), 0.2),
                        new Range(ghiBucket2.getValue(), 0.2),
                        new Range(ghiBucket3.getValue(), 0.2),
                        new Range(ghiBucket4.getValue(), 0.2),
                })));
        final ConsumableTestDefinition ghiTD = new ConsumableTestDefinition("3", "${proctor:contains(LANGUAGES, language)}", TestType.ANONYMOUS_USER, "ghisalt", ghiBuckets, ghiAllocations, ghiTestConstants, "jangle bing zimple plop");

        final ConsumableTestDefinition jklTD = ConsumableTestDefinition.fromTestDefinition(
                TestDefinition.builder()
                        .setDependency(new TestDependency("abc", 0))
                        .setRule("language == 'en'")
                        .setTestType(TestType.ANONYMOUS_USER)
                        .setSalt("&hash11")
                        .addBuckets(
                                new TestBucket("bucket1", 1, ""),
                                new TestBucket("bucket2", 2, "")
                        )
                        .addAllocations(
                                new Allocation(null, ImmutableList.of(
                                        new Range(1, 0.5),
                                        new Range(2, 0.5)
                                ))
                        )
                        .build()
        );

        final Map<String, ConsumableTestDefinition> tests = Maps.newLinkedHashMap();
        tests.put("abc", abcTD);
        tests.put("def", defTD);
        tests.put("ghi", ghiTD);
        tests.put("jkl", jklTD);

        matrix.setTests(tests);

        return Proctor.construct(matrix, ProctorLoadResult.emptyResult(), RuleEvaluator.defaultFunctionMapperBuilder().build());
    }

    private String calcGroups(final Proctor proctor, final String id, final String country, final String language, final int num) {
        return calcGroups(proctor, id, country, language, num, Maps.<String, Integer>newHashMap());
    }

    private String calcGroups(final Proctor proctor, final String id, final String country, final String language, final int num,
            final Map<String, Integer> forceGroups) {
        final Map<String, Object> context = Maps.newHashMap();
        context.put("num", Integer.valueOf(num));
        context.put("country", country);
        context.put("language", language);

        final Identifiers identifiers = new Identifiers(TestType.ANONYMOUS_USER, id);
        final ProctorResult proctorResult = proctor.determineTestGroups(identifiers, context, forceGroups);
        final StringBuilder buckets = new StringBuilder();
        for (final Iterator<Entry<String, TestBucket>> iterator = proctorResult.getBuckets().entrySet().iterator(); iterator.hasNext(); ) {
            final Entry<String, TestBucket> entry = iterator.next();
            final String testName = entry.getKey();
            final TestBucket testBucket = entry.getValue();

            if (testBucket.getValue() >= 0) {
                if (buckets.length() > 0){
                    buckets.append(',');
                }
                buckets.append(testName)
                        .append(testBucket.getValue());
            }
        }

        return buckets.toString();
    }
}
