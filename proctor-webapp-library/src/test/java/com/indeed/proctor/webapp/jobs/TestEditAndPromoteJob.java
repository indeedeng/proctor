package com.indeed.proctor.webapp.jobs;

import com.google.common.collect.Lists;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.testUtil.Stubs;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.jobs.BackgroundJob.JobType;
import com.indeed.proctor.webapp.model.RevisionDefinition;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.indeed.proctor.webapp.jobs.AbstractJob.validateComment;
import static com.indeed.proctor.webapp.jobs.AbstractJob.validateUsernamePassword;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class TestEditAndPromoteJob {

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
                Stubs.createTestDefinition("control:0,test:1", range))
        );
        Optional<String> maxAllocId = EditAndPromoteJob.getMaxAllocationId(revisionDefinitions);
        assertFalse(maxAllocId.isPresent());
        // Normal allocation ids
        revisionDefinitions.add(new RevisionDefinition(
                new Revision("revision3", "tester", new Date(now + 2000), "change 3"),
                Stubs.createTestDefinition("control:0,test:1", range, Lists.newArrayList("#A1", "#B1")))
        );
        // Different allocation id version for A, deleted allocation B
        revisionDefinitions.add(new RevisionDefinition(
                new Revision("revision4", "tester", new Date(now + 3000), "change 4"),
                Stubs.createTestDefinition("control:0,test:1", range, Lists.newArrayList("#A1234")))
        );
        // Add allocation C, D
        revisionDefinitions.add(new RevisionDefinition(
                new Revision("revision5", "tester", new Date(now + 4000), "change 5"),
                Stubs.createTestDefinition("control:0,test:1", range, Lists.newArrayList("#A1234", "#C1", "#D1")))
        );
        // Delete allocation D
        revisionDefinitions.add(new RevisionDefinition(
                new Revision("revision6", "tester", new Date(now + 5000), "change 6"),
                Stubs.createTestDefinition("control:0,test:1", range, Lists.newArrayList("#A1234", "#C1")))
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

    @Test
    public void testValidateUsernamePassword() {
        assertThatValidateUsernamePasswordThrows("", "");
        assertThatValidateUsernamePasswordThrows("", "password");
        assertThatValidateUsernamePasswordThrows("username", "");

        // Nothing happens for valid username and password
        validateUsernamePassword("username", "password");
    }

    private void assertThatValidateUsernamePasswordThrows(final String username, final String password) {
        assertThatThrownBy(() -> validateUsernamePassword(username, password))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No username or password provided");
    }

    @Test
    public void testValidateComment() {
        assertThatThrownBy(() -> validateComment(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Comment is required.");

        // Nothing happens for valid comment
        validateComment("valid");
    }}
