package com.indeed.proctor.webapp.jobs;

import com.google.common.collect.Lists;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.jobs.BackgroundJob.JobType;
import com.indeed.proctor.webapp.model.RevisionDefinition;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.indeed.proctor.testUtil.Stubs.createTestDefinition;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
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

    @Test
    public void testCreateJobType() {
        assertEquals(JobType.TEST_CREATION, EditAndPromoteJob.createJobType(true, Environment.WORKING));
        assertEquals(JobType.TEST_CREATION_PROMOTION, EditAndPromoteJob.createJobType(true, Environment.PRODUCTION));
        assertEquals(JobType.TEST_CREATION_PROMOTION_QA, EditAndPromoteJob.createJobType(true, Environment.QA));
        assertEquals(JobType.TEST_EDIT, EditAndPromoteJob.createJobType(false, Environment.WORKING));
        assertEquals(JobType.TEST_EDIT_PROMOTION, EditAndPromoteJob.createJobType(false, Environment.PRODUCTION));
        assertEquals(JobType.TEST_EDIT_PROMOTION_QA, EditAndPromoteJob.createJobType(false, Environment.QA));
    }


}
