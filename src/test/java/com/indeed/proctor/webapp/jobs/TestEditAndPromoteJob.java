package com.indeed.proctor.webapp.jobs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.indeed.proctor.common.EnvironmentVersion;
import com.indeed.proctor.common.model.*;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.webapp.model.RevisionDefinition;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class TestEditAndPromoteJob {
    @Test
    public void testIsValidTestName() {
        assertFalse(EditAndPromoteJob.isValidTestName(""));
        assertTrue(EditAndPromoteJob.isValidTestName("a"));
        assertTrue(EditAndPromoteJob.isValidTestName("A"));
        assertTrue(EditAndPromoteJob.isValidTestName("_"));
        assertFalse(EditAndPromoteJob.isValidTestName("0"));
        assertFalse(EditAndPromoteJob.isValidTestName("."));
        assertFalse(EditAndPromoteJob.isValidTestName("_0"));
        assertFalse(EditAndPromoteJob.isValidTestName("inValid_test_Name_10"));
        assertFalse(EditAndPromoteJob.isValidTestName("inValid#test#name"));
    }

    @Test
    public void testIsValidBucketName() {
        assertFalse(EditAndPromoteJob.isValidBucketName(""));
        assertTrue(EditAndPromoteJob.isValidBucketName("valid_bucket_Name"));
        assertTrue(EditAndPromoteJob.isValidBucketName("valid_bucket_Name0"));
        assertFalse(EditAndPromoteJob.isValidBucketName("0invalid_bucket_Name"));
    }

    @Test
    public void testAllocationOnlyChangeDetection() {
        {
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.5, .5};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0", rangeOne);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0", rangeTwo);
            assertTrue(EditAndPromoteJob.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
        { // different amounts of buckets
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.5, .5};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0", rangeOne);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0,control:1", rangeTwo);
            assertFalse(EditAndPromoteJob.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
        { // different buckets
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.5, .5};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0,control:2", rangeOne);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0,control:1", rangeTwo);
            assertFalse(EditAndPromoteJob.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
        { //different testtypes
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.5, .5};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0,control:2", TestType.RANDOM, rangeOne);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0,control:2", TestType.EMAIL_ADDRESS, rangeTwo);
            assertFalse(EditAndPromoteJob.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
        { //testing different salts
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.5, .5};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0,control:2", TestType.RANDOM, "salt1", rangeOne);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0,control:2", TestType.RANDOM, "salt2", rangeTwo);
            assertFalse(EditAndPromoteJob.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
        { //testing 0 to greater allocation
            final double[] rangeOne = {0, 1};
            final double[] rangeTwo = {.5, .5};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0,control:2", rangeOne);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0,control:2", rangeTwo);
            assertFalse(EditAndPromoteJob.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
        { //testing non 100% to 100% allocation
            final double[] rangeOne = {.5, .5};
            final double[] rangeTwo = {1, 0};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0,control:2", rangeOne);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0,control:2", rangeTwo);
            assertTrue(EditAndPromoteJob.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
        { //testing different salts
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.5, .5};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0,control:2", TestType.RANDOM, "salt1", rangeOne);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0,control:2", TestType.RANDOM, "salt2", rangeTwo);
            assertFalse(EditAndPromoteJob.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
        { //testing with payloads
            final Payload payloadBucket1Test1 = new Payload();
            payloadBucket1Test1.setDoubleValue(10.1D);
            final Payload payloadBucket2Test1 = new Payload();
            payloadBucket2Test1.setStringValue("p");
            final Payload[] payloadst1 = {payloadBucket1Test1, payloadBucket2Test1};
            final Payload payloadBucket1Test2 = new Payload();
            payloadBucket1Test2.setDoubleValue(10.1D);
            final Payload payloadBucket2Test2 = new Payload();
            payloadBucket2Test2.setStringValue("p");
            final Payload[] payloadst2 = {payloadBucket1Test2, payloadBucket2Test2};
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.5, .5};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0,control:2", TestType.RANDOM, "salt1", rangeOne, payloadst1);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0,control:2", TestType.RANDOM, "salt1", rangeTwo, payloadst2);
            assertTrue(EditAndPromoteJob.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
        { //testing different payloads
            final Payload payloadBucket1Test1 = new Payload();
            payloadBucket1Test1.setStringValue("p2");
            final Payload payloadBucket2Test1 = new Payload();
            payloadBucket2Test1.setStringValue("p");
            final Payload[] payloadst1 = {payloadBucket1Test1, payloadBucket2Test1};
            final Payload payloadBucket1Test2 = new Payload();
            payloadBucket1Test2.setDoubleValue(10.1D);
            final Payload payloadBucket2Test2 = new Payload();
            payloadBucket2Test2.setStringValue("p");
            final Payload[] payloadst2 = {payloadBucket1Test2, payloadBucket2Test2};
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.5, .5};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0,control:2", TestType.RANDOM, "salt1", rangeOne, payloadst1);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0,control:2", TestType.RANDOM, "salt1", rangeTwo, payloadst2);
            assertFalse(EditAndPromoteJob.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
        { //testing null payloads
            final Payload payloadBucket1Test2 = new Payload();
            payloadBucket1Test2.setDoubleValue(10.1D);
            final Payload payloadBucket2Test2 = new Payload();
            payloadBucket2Test2.setStringValue("p");
            final Payload[] payloadst2 = {payloadBucket1Test2, payloadBucket2Test2};
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.5, .5};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0,control:2", TestType.RANDOM, "salt1", rangeOne, null);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0,control:2", TestType.RANDOM, "salt1", rangeTwo, payloadst2);
            assertFalse(EditAndPromoteJob.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
        { //testing map payload autopromote equality
            final Payload payloadBucket1Test2 = new Payload();
            HashMap<String, Object> one = new HashMap<String, Object>();
            one.put("A", new ArrayList() {{
                add(1);
            }});
            one.put("B", 2.1);
            payloadBucket1Test2.setMap(one);
            final Payload payloadBucket2Test2 = new Payload();
            payloadBucket2Test2.setMap(ImmutableMap.<String, Object>of("A", "asdf"));
            final Payload[] payloadst2 = {payloadBucket1Test2, payloadBucket2Test2};
            final Payload payloadBucket1Test1 = new Payload();
            HashMap<String, Object> two = new HashMap<String, Object>();
            two.put("B", 2.1);
            two.put("A", new ArrayList() {{
                add(1);
            }});
            payloadBucket1Test1.setMap(two);
            final Payload payloadBucket2Test1 = new Payload();
            payloadBucket2Test1.setMap(ImmutableMap.<String, Object>of("A", "asdf"));
            final Payload[] payloadst1 = {payloadBucket1Test1, payloadBucket2Test1};
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.5, .5};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0,control:2", TestType.RANDOM, "salt1", rangeOne, payloadst1);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0,control:2", TestType.RANDOM, "salt1", rangeTwo, payloadst2);
            testDefinitionTwo.setDescription("updated description");
            assertTrue(EditAndPromoteJob.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
        { //testing map payload failed autopromote equality
            final Payload payloadBucket1Test2 = new Payload();
            payloadBucket1Test2.setMap(ImmutableMap.<String, Object>of("A", new ArrayList() {{
                add("ff");
            }}));
            final Payload payloadBucket2Test2 = new Payload();
            payloadBucket2Test2.setMap(ImmutableMap.<String, Object>of("A", "asdf"));
            final Payload[] payloadst2 = {payloadBucket1Test2, payloadBucket2Test2};
            final Payload payloadBucket1Test1 = new Payload();
            payloadBucket1Test1.setMap(ImmutableMap.<String, Object>of("A", new ArrayList() {{
                add(1);
            }}));
            final Payload payloadBucket2Test1 = new Payload();
            payloadBucket2Test1.setMap(ImmutableMap.<String, Object>of("A", "asdf"));
            final Payload[] payloadst1 = {payloadBucket1Test1, payloadBucket2Test1};
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.5, .5};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0,control:2", TestType.RANDOM, "salt1", rangeOne, payloadst1);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0,control:2", TestType.RANDOM, "salt1", rangeTwo, payloadst2);
            assertFalse(EditAndPromoteJob.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
    }

    public TestDefinition createTestDefinition(String bucketsString, double[] ranges) {
        return createTestDefinition(bucketsString, TestType.RANDOM, "salt", ranges);
    }

    public TestDefinition createTestDefinition(String bucketsString, TestType testType, double[] ranges) {
        return createTestDefinition(bucketsString, testType, "salt", ranges);
    }

    public TestDefinition createTestDefinition(String bucketsString, TestType testType, String salt, double[] ranges) {
        return createTestDefinition(bucketsString, testType, salt, ranges, null);
    }

    public TestDefinition createTestDefinition(String bucketsString, TestType testType, String salt, double[] ranges, Payload[] payloads) {
        return createTestDefinition(bucketsString, testType, salt, ranges, payloads, null);
    }

    public TestDefinition createTestDefinition(String bucketsString, double[] ranges, List<String> allocationIds) {
        return createTestDefinition(bucketsString, TestType.RANDOM, "salt", ranges, null, allocationIds);
    }

    public TestDefinition createTestDefinition(String bucketsString, TestType testType, String salt, double[] ranges, Payload[] payloads, List<String> allocationIds) {
        final List<Range> rangeList = new ArrayList<Range>();
        for (int i = 0; i < ranges.length; i++) {
            final Range newRange = new Range(i, ranges[i]);
            rangeList.add(newRange);
        }
        String[] buckets = bucketsString.split(",");
        List<TestBucket> buckList = new ArrayList<TestBucket>();
        for (int i = 0; i < buckets.length; i++) {
            String bucket = buckets[i];
            final int colonInd = bucket.indexOf(':');
            final TestBucket tempBucket = new TestBucket(bucket.substring(0, colonInd), Integer.parseInt(bucket.substring(colonInd + 1)), "description", (payloads == null) ? null : payloads[i]);
            buckList.add(tempBucket);

        }
        final Allocation alloc = new Allocation(null, rangeList);
        final List<Allocation> allocList = new ArrayList<Allocation>();
        if (allocationIds == null) {
            allocList.add(new Allocation(null, rangeList));
        } else {
            for (final String allocationId : allocationIds) {
                allocList.add(new Allocation(null, rangeList, allocationId));
            }
        }
        return new TestDefinition(EnvironmentVersion.UNKNOWN_REVISION, null, testType, salt, buckList, allocList, Collections.<String, Object>emptyMap(), Collections.<String, Object>emptyMap(), null);
    }

    @Test
    public void testGetMaxAllocationId() {
        final double[] range = {.7, .3};
        final List<RevisionDefinition> revisionDefinitions = new ArrayList<>();
        final long now = System.currentTimeMillis();
        // Null TestDefinition
        revisionDefinitions.add(new RevisionDefinition(
                new Revision("revision1", "tester", new Date(now), "change 1"),
                null)
        );
        // Empty allocation id
        revisionDefinitions.add(new RevisionDefinition(
                new Revision("revision2", "tester", new Date(now + 1000), "change 2"),
                createTestDefinition("control:0,test:1", range))
        );
        Optional<String> maxAllocId = EditAndPromoteJob.getMaxAllocationId(revisionDefinitions);
        assertFalse(maxAllocId.isPresent());
        // Normal allocation ids
        revisionDefinitions.add(new RevisionDefinition(
                new Revision("revision3", "tester", new Date(now + 2000), "change 3"),
                createTestDefinition("control:0,test:1", range, Lists.newArrayList("#A1", "#B1")))
        );
        // Different allocation id version for A, deleted allocation B
        revisionDefinitions.add(new RevisionDefinition(
                new Revision("revision4", "tester", new Date(now + 3000), "change 4"),
                createTestDefinition("control:0,test:1", range, Lists.newArrayList("#A1234")))
        );
        // Add allocation C, D
        revisionDefinitions.add(new RevisionDefinition(
                new Revision("revision5", "tester", new Date(now + 4000), "change 5"),
                createTestDefinition("control:0,test:1", range, Lists.newArrayList("#A1234", "#C1", "#D1")))
        );
        // Delete allocation D
        revisionDefinitions.add(new RevisionDefinition(
                new Revision("revision6", "tester", new Date(now + 5000), "change 6"),
                createTestDefinition("control:0,test:1", range, Lists.newArrayList("#A1234", "#C1")))
        );
        maxAllocId = EditAndPromoteJob.getMaxAllocationId(revisionDefinitions);
        assertEquals("#D1", maxAllocId.get());
    }
}
