package com.indeed.proctor.webapp.jobs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.testUtil.Stubs;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.jobs.BackgroundJob.JobType;
import com.indeed.proctor.webapp.model.RevisionDefinition;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.indeed.proctor.webapp.jobs.EditAndPromoteJob.isValidMetaTag;
import static com.indeed.proctor.webapp.jobs.EditAndPromoteJob.validateMetaTags;
import static com.indeed.proctor.webapp.jobs.EditAndPromoteJob.validateTestName;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestEditAndPromoteJob {

    @Test
    public void testValidateTestName() {
        final String longName = StringUtils.repeat("a", 101);

        for (final String input : new String[]{"", "0", "_", "__", ".", "_0", "a#b"}) {
            assertThatThrownBy(() -> validateTestName(input))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Test Name must be alpha-numeric underscore and not start/end with a number");
        }
        assertThatThrownBy(() -> validateTestName(longName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Test Name length can't be longer than 100");

        // Nothing happens for valid test names
        validateTestName("a");
        validateTestName("A");
        validateTestName("a_a");
        validateTestName("a1a");
        validateTestName("_0_");
    }

    @Test
    public void testIsValidMetaTag() {
        final String longName = StringUtils.repeat("a", 101);

        assertTrue(isValidMetaTag("a"));
        assertTrue(isValidMetaTag("A"));
        assertTrue(isValidMetaTag("a_a"));
        assertTrue(isValidMetaTag("a1a"));
        assertTrue(isValidMetaTag("_0_"));

        assertFalse(isValidMetaTag(""));
        assertFalse(isValidMetaTag("0"));
        assertFalse(isValidMetaTag("_"));
        assertFalse(isValidMetaTag("__"));
        assertFalse(isValidMetaTag("."));
        assertFalse(isValidMetaTag("_0"));
        assertFalse(isValidMetaTag("a#b"));
        assertFalse(isValidMetaTag(longName));
    }

    @Test
    public void testIsValidMetaTags() {
        final TestDefinition invalidMetaTagDefinition = createTestDefinition(ImmutableList.of("0", "_", "a", "__"));
        final TestDefinition emptyMetaTagDefinition = createTestDefinition(ImmutableList.of());
        final TestDefinition validMetaTagDefinition = createTestDefinition(ImmutableList.of("a", "a_a"));

        assertThatThrownBy(() -> validateMetaTags(invalidMetaTagDefinition))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Meta Tag must \n" +
                        " - be alpha-numeric underscore\n" +
                        " - not start/end with a number\n" +
                        " - not be longer than 100\n" +
                        " but found: [0, _, __]");

        // Nothing happens for valid meta tags
        validateMetaTags(emptyMetaTagDefinition);
        validateMetaTags(validMetaTagDefinition);
    }

    private TestDefinition createTestDefinition(final List<String> metaTags) {
        return new TestDefinition(
                "",
                "",
                TestType.EMAIL_ADDRESS,
                "",
                Collections.emptyList(),
                Collections.emptyList(),
                false,
                Collections.emptyMap(),
                Collections.emptyMap(),
                "",
                metaTags
        );
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
}
