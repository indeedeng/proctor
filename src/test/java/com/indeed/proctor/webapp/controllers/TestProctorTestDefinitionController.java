package com.indeed.proctor.webapp.controllers;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.EnvironmentVersion;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestType;
import java.util.Collections;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class TestProctorTestDefinitionController {
    @Test
    public void testIsValidTestName() {
        Assert.assertFalse(ProctorTestDefinitionController.isValidTestName(""));
        Assert.assertTrue(ProctorTestDefinitionController.isValidTestName("a"));
        Assert.assertTrue(ProctorTestDefinitionController.isValidTestName("A"));
        Assert.assertTrue(ProctorTestDefinitionController.isValidTestName("_"));
        Assert.assertFalse(ProctorTestDefinitionController.isValidTestName("0"));
        Assert.assertFalse(ProctorTestDefinitionController.isValidTestName("."));
        Assert.assertTrue(ProctorTestDefinitionController.isValidTestName("_0"));
        Assert.assertTrue(ProctorTestDefinitionController.isValidTestName("valid_test_Name_10"));
        Assert.assertFalse(ProctorTestDefinitionController.isValidTestName("inValid#test#name"));
    }

    @Test
    public void testIsValidBucketName() {
        Assert.assertFalse(ProctorTestDefinitionController.isValidBucketName(""));
        Assert.assertTrue(ProctorTestDefinitionController.isValidBucketName("valid_bucket_Name"));
        Assert.assertFalse(ProctorTestDefinitionController.isValidBucketName("0invalid_bucket_Name"));
    }

    @Test
    public void testAllocationOnlyChangeDetection() {
        {
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.5, .5};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0", rangeOne);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0", rangeTwo);
            Assert.assertTrue(ProctorTestDefinitionController.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
        { // different amounts of buckets
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.5, .5};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0", rangeOne);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0,control:1", rangeTwo);
            Assert.assertFalse(ProctorTestDefinitionController.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
        { // different buckets
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.5, .5};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0,control:2", rangeOne);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0,control:1", rangeTwo);
            Assert.assertFalse(ProctorTestDefinitionController.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
        { //different testtypes
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.5, .5};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0,control:2",TestType.RANDOM, rangeOne);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0,control:2",TestType.EMAIL_ADDRESS, rangeTwo);
            Assert.assertFalse(ProctorTestDefinitionController.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
        { //testing different salts
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.5, .5};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0,control:2",TestType.RANDOM, "salt1",rangeOne);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0,control:2",TestType.RANDOM, "salt2", rangeTwo);
            Assert.assertFalse(ProctorTestDefinitionController.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
        { //testing 0 to greater allocation
            final double[] rangeOne = {0, 1};
            final double[] rangeTwo = {.5, .5};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0,control:2", rangeOne);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0,control:2", rangeTwo);
            Assert.assertFalse(ProctorTestDefinitionController.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
        { //testing non 100% to 100% allocation
            final double[] rangeOne = {.5, .5};
            final double[] rangeTwo = {1, 0};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0,control:2", rangeOne);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0,control:2", rangeTwo);
            Assert.assertTrue(ProctorTestDefinitionController.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
        { //testing different salts
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.5, .5};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0,control:2",TestType.RANDOM, "salt1",rangeOne);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0,control:2",TestType.RANDOM, "salt2", rangeTwo);
            Assert.assertFalse(ProctorTestDefinitionController.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
        { //testing with payloads
            final Payload payloadBucket1Test1 = new Payload();
            payloadBucket1Test1.setDoubleValue(10.1D);
            final Payload payloadBucket2Test1 = new Payload();
            payloadBucket2Test1.setStringValue("p");
            final Payload[] payloadst1 = {payloadBucket1Test1,payloadBucket2Test1};
            final Payload payloadBucket1Test2 = new Payload();
            payloadBucket1Test2.setDoubleValue(10.1D);
            final Payload payloadBucket2Test2 = new Payload();
            payloadBucket2Test2.setStringValue("p");
            final Payload[] payloadst2 = {payloadBucket1Test2,payloadBucket2Test2};
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.5, .5};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0,control:2",TestType.RANDOM, "salt1", rangeOne,payloadst1);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0,control:2",TestType.RANDOM, "salt1", rangeTwo,payloadst2);
            Assert.assertTrue(ProctorTestDefinitionController.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
        { //testing different payloads
            final Payload payloadBucket1Test1 = new Payload();
            payloadBucket1Test1.setStringValue("p2");
            final Payload payloadBucket2Test1 = new Payload();
            payloadBucket2Test1.setStringValue("p");
            final Payload[] payloadst1 = {payloadBucket1Test1,payloadBucket2Test1};
            final Payload payloadBucket1Test2 = new Payload();
            payloadBucket1Test2.setDoubleValue(10.1D);
            final Payload payloadBucket2Test2 = new Payload();
            payloadBucket2Test2.setStringValue("p");
            final Payload[] payloadst2 = {payloadBucket1Test2,payloadBucket2Test2};
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.5, .5};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0,control:2",TestType.RANDOM, "salt1",rangeOne,payloadst1);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0,control:2",TestType.RANDOM, "salt1", rangeTwo,payloadst2);
            Assert.assertFalse(ProctorTestDefinitionController.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
        { //testing null payloads
            final Payload payloadBucket1Test2 = new Payload();
            payloadBucket1Test2.setDoubleValue(10.1D);
            final Payload payloadBucket2Test2 = new Payload();
            payloadBucket2Test2.setStringValue("p");
            final Payload[] payloadst2 = {payloadBucket1Test2,payloadBucket2Test2};
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.5, .5};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0,control:2",TestType.RANDOM, "salt1",rangeOne,null);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0,control:2",TestType.RANDOM, "salt1", rangeTwo,payloadst2);
            Assert.assertFalse(ProctorTestDefinitionController.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
        { //testing map payload autopromote equality
            final Payload payloadBucket1Test2 = new Payload();
            HashMap<String, Object> one = new HashMap<String, Object>();
            one.put("A", new ArrayList(){{add(1);}});
            one.put("B", 2.1);
            payloadBucket1Test2.setMap(one);
            final Payload payloadBucket2Test2 = new Payload();
            payloadBucket2Test2.setMap(ImmutableMap.<String,Object>of("A", "asdf"));
            final Payload[] payloadst2 = {payloadBucket1Test2,payloadBucket2Test2};
            final Payload payloadBucket1Test1 = new Payload();
            HashMap<String, Object> two = new HashMap<String, Object>();
            two.put("B", 2.1);
            two.put("A", new ArrayList(){{add(1);}});
            payloadBucket1Test1.setMap(two);
            final Payload payloadBucket2Test1 = new Payload();
            payloadBucket2Test1.setMap(ImmutableMap.<String,Object>of("A", "asdf"));
            final Payload[] payloadst1 = {payloadBucket1Test1,payloadBucket2Test1};
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.5, .5};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0,control:2",TestType.RANDOM, "salt1",rangeOne,payloadst1);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0,control:2",TestType.RANDOM, "salt1", rangeTwo,payloadst2);
            Assert.assertTrue(ProctorTestDefinitionController.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
        { //testing map payload failed autopromote equality
            final Payload payloadBucket1Test2 = new Payload();
            payloadBucket1Test2.setMap(ImmutableMap.<String,Object>of("A", new ArrayList(){{add("ff");}}));
            final Payload payloadBucket2Test2 = new Payload();
            payloadBucket2Test2.setMap(ImmutableMap.<String,Object>of("A", "asdf"));
            final Payload[] payloadst2 = {payloadBucket1Test2,payloadBucket2Test2};
            final Payload payloadBucket1Test1 = new Payload();
            payloadBucket1Test1.setMap(ImmutableMap.<String,Object>of("A", new ArrayList(){{add(1);}}));
            final Payload payloadBucket2Test1 = new Payload();
            payloadBucket2Test1.setMap(ImmutableMap.<String,Object>of("A", "asdf"));
            final Payload[] payloadst1 = {payloadBucket1Test1,payloadBucket2Test1};
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.5, .5};
            final TestDefinition testDefinitionOne = createTestDefinition("testbuck:0,control:2",TestType.RANDOM, "salt1",rangeOne,payloadst1);
            final TestDefinition testDefinitionTwo = createTestDefinition("testbuck:0,control:2",TestType.RANDOM, "salt1", rangeTwo,payloadst2);
            Assert.assertFalse(ProctorTestDefinitionController.isAllocationOnlyChange(testDefinitionOne, testDefinitionTwo));
        }
    }
    public TestDefinition createTestDefinition(String bucketsString, double[] ranges) {
        return createTestDefinition(bucketsString,TestType.RANDOM,"salt",ranges);
    }
    public TestDefinition createTestDefinition(String bucketsString, TestType testType, double[] ranges) {
        return createTestDefinition(bucketsString,testType,"salt",ranges);
    }
    public TestDefinition createTestDefinition(String bucketsString, TestType testType, String salt, double[] ranges) {
        return createTestDefinition(bucketsString,testType,salt,ranges,null);
    }
    public TestDefinition createTestDefinition(String bucketsString, TestType testType, String salt, double[] ranges, Payload[] payloads) {
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
            final TestBucket tempBucket= new TestBucket(bucket.substring(0,colonInd),Integer.parseInt(bucket.substring(colonInd+1)),"description",(payloads==null)?null:payloads[i]);
            buckList.add(tempBucket);

        }
        final Allocation alloc = new Allocation(null, rangeList);
        final List<Allocation> allocList = new ArrayList<Allocation>();
        allocList.add(alloc);
        return new TestDefinition(EnvironmentVersion.UNKNOWN_REVISION, null, testType, salt, buckList, allocList, Collections.<String, Object>emptyMap(), Collections.<String, Object>emptyMap(), null);
    }
}
