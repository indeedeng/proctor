package com.indeed.proctor.webapp.jobs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.indeed.proctor.common.EnvironmentVersion;
import com.indeed.proctor.common.ProctorPromoter;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.jobs.BackgroundJob.JobType;
import com.indeed.proctor.webapp.model.RevisionDefinition;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Enclosed.class)
public class TestEditAndPromoteJob {

    public static class TestEditAndPromoteJobStaticMethod {
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
            { //testing non 100% to 100% allocation removing bucket with length 0
                final double[] rangeOne = {.5, .5};
                final double[] rangeTwo = {1};
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
        public void testIsAllInactiveTest() {
            { // testing isAllInactiveTest is false
                final double[] range = {.7, .3};
                final TestDefinition testDefinition = createTestDefinition(
                        "control:0,test:1",
                        range,
                        ImmutableList.of("#A1234", "#C1")
                );

                assertFalse(EditAndPromoteJob.isAllInactiveTest(testDefinition));
            }

            { // testing isAllInactiveTest is false
                final double[] range = {.7, .3};
                final TestDefinition testDefinition = createTestDefinition(
                        "inactive:-1,control:0",
                        range,
                        ImmutableList.of("#A1234", "#C1")
                );

                assertFalse(EditAndPromoteJob.isAllInactiveTest(testDefinition));
            }

            { // testing isAllInactiveTest is false
                final double[] range = {1.0};
                final TestDefinition testDefinition = createTestDefinition(
                        "inactive:0",
                        range,
                        ImmutableList.of("#A1234", "#C1")
                );

                assertFalse(EditAndPromoteJob.isAllInactiveTest(testDefinition));
            }

            { // testing isAllInactiveTest is false
                final double[] range = {1.0};
                final TestDefinition testDefinition = createTestDefinition(
                        "evitcani:-1",
                        range,
                        ImmutableList.of("#A1234", "#C1")
                );

                assertFalse(EditAndPromoteJob.isAllInactiveTest(testDefinition));
            }

            { // testing isAllInactiveTest is true
                final double[] range = {1.0};
                final TestDefinition testDefinition = createTestDefinition(
                        "inactive:-1",
                        range,
                        ImmutableList.of("#A1234", "#C1")
                );

                assertTrue(EditAndPromoteJob.isAllInactiveTest(testDefinition));
            }

            { // testing isAllInactiveTest is true
                final double[] range = {1.0};
                final TestDefinition testDefinition = createTestDefinition(
                        "disabled:-1",
                        range,
                        ImmutableList.of("#A1234", "#C1")
                );

                assertTrue(EditAndPromoteJob.isAllInactiveTest(testDefinition));
            }

            { // testing isAllInactiveTest is true
                final double[] range = {1.0, 0.0};
                final TestDefinition testDefinition = createTestDefinition(
                        "inactive:-1,control:0",
                        range,
                        ImmutableList.of("#A1234", "#C1")
                );

                assertTrue(EditAndPromoteJob.isAllInactiveTest(testDefinition));
            }
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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static class TestEditAndPromoteJobInstanceMethod {
        @Mock
        private ProctorStore trunkStore;
        @Mock
        private ProctorStore qaStore;
        @Mock
        private ProctorStore productionStore;
        @Mock
        private ProctorPromoter proctorPromoter;
        @Mock
        private CommentFormatter commentFormatter;
        @Mock
        private MatrixChecker matrixChecker;
        @Mock
        private BackgroundJob backgroundJob;

        private static final String TEST_NAME = "unit_tst";
        private static final String USERNAME = "username";
        private static final String PASSWORD = "PASSWORD";
        private static final String AUTHOR = "AUTHOR";
        private static final Map<String, String[]> REQUEST_PARAMETER_MAP = ImmutableMap.of();
        private static final String PREVIOUS_REVISION = "PREVIOUS_REVISION";
        private static final String TRUNK_REVISION = "TRUNK_REVISION";
        private static final String QA_REVISION = "QA_REVISION";
        private static final String PROD_REVISION = "PROD_REVISION";
        private static final Revision PREVIOUS_VERSION = new Revision(PREVIOUS_REVISION, null, null, null);
        private static final Revision TRUNK_VERSION = new Revision(TRUNK_REVISION, null, null, null);
        private static final Revision QA_VERSION = new Revision(QA_REVISION, null, null, null);
        private static final Revision PROD_VERSION = new Revision(PROD_REVISION, null, null, null);

        private EditAndPromoteJob editAndPromoteJob;

        @Before
        public void setUp() throws StoreException {
            MockitoAnnotations.initMocks(this);
            editAndPromoteJob = spy(new EditAndPromoteJob(
                    trunkStore,
                    qaStore,
                    productionStore,
                    new BackgroundJobManager(),
                    new BackgroundJobFactory(),
                    proctorPromoter,
                    commentFormatter,
                    matrixChecker
            ));

            // for getCurrentVersion
            when(trunkStore.getHistory(TEST_NAME, 0, 1)).thenReturn(ImmutableList.of(TRUNK_VERSION));
            // for getCurrentVersionIfDirectlyFollowing
            when(trunkStore.getHistory(TEST_NAME, 0, 2))
                    .thenReturn(ImmutableList.of(TRUNK_VERSION, PREVIOUS_VERSION));

            when(proctorPromoter.getEnvironmentVersion(TEST_NAME))
                    .thenReturn(new EnvironmentVersion(TEST_NAME, TRUNK_VERSION, TRUNK_REVISION, QA_VERSION, QA_REVISION,
                            PROD_VERSION, PROD_REVISION));
        }

        private void mockDoPromoteInternal(
                final boolean isSuccess,
                final boolean isPromoteToQa,
                final boolean isAutopromote
        ) {
            final Environment destEnvironment = isPromoteToQa ? Environment.QA : Environment.PRODUCTION;
            final String destRevision = isPromoteToQa ? QA_REVISION : PROD_REVISION;

            if (isSuccess) {
                try {
                    Mockito.doReturn(true).when(editAndPromoteJob).doPromoteInternal(
                            TEST_NAME,
                            USERNAME,
                            PASSWORD,
                            AUTHOR,
                            Environment.WORKING,
                            TRUNK_REVISION,
                            destEnvironment,
                            destRevision,
                            REQUEST_PARAMETER_MAP,
                            backgroundJob,
                            isAutopromote
                    );
                } catch (final Exception e) {
                    // do nothing
                }
            } else {
                try {
                    Mockito.doThrow(new IllegalArgumentException("invalid")).when(editAndPromoteJob).doPromoteInternal(
                            TEST_NAME,
                            USERNAME,
                            PASSWORD,
                            AUTHOR,
                            Environment.WORKING,
                            TRUNK_REVISION,
                            destEnvironment,
                            destRevision,
                            REQUEST_PARAMETER_MAP,
                            backgroundJob,
                            isAutopromote
                    );
                } catch (final Exception e) {
                    // do nothing
                }

            }
        }

        @SuppressWarnings("ResultOfMethodCallIgnored")
        @Test
        public void testDoPromoteTestToQaAndProd() throws Exception {
            { // testing existingTestDefinition is null and test is active
                // Arrange
                final double[] range = {1.0};
                final TestDefinition testDefinitionToUpdate = createTestDefinition("testbuck:0", range);
                final TestDefinition existingTestDefinition = null;
                doNothing().when(backgroundJob).log(anyString());

                // Act / Assert
                assertThatThrownBy(() ->
                        editAndPromoteJob.doPromoteTestToQaAndProd(TEST_NAME, USERNAME, PASSWORD, AUTHOR, testDefinitionToUpdate,
                                PREVIOUS_REVISION, REQUEST_PARAMETER_MAP, backgroundJob, trunkStore, QA_REVISION, PROD_REVISION,
                                existingTestDefinition)
                );
                verify(backgroundJob, never()).log(anyString());
                Mockito.reset(editAndPromoteJob);
                Mockito.reset(backgroundJob);
            }

            { // testing existingTestDefinition is null and test is 100% inactive
                // Arrange
                final double[] range = {1.0};
                final TestDefinition testDefinitionToUpdate = createTestDefinition("inactive:-1", range);
                final TestDefinition existingTestDefinition = null;
                doNothing().when(backgroundJob).log(anyString());
                doNothing().when(editAndPromoteJob).doPromoteInactiveTestToQaAndProd(TEST_NAME, USERNAME, PASSWORD, AUTHOR,
                        REQUEST_PARAMETER_MAP, backgroundJob, TRUNK_REVISION, QA_REVISION, PROD_REVISION);

                // Act
                editAndPromoteJob.doPromoteTestToQaAndProd(TEST_NAME, USERNAME, PASSWORD, AUTHOR, testDefinitionToUpdate,
                        PREVIOUS_REVISION, REQUEST_PARAMETER_MAP, backgroundJob, trunkStore, QA_REVISION, PROD_REVISION,
                        existingTestDefinition);

                // Assert
                verify(backgroundJob, never()).log(anyString());
                verify(editAndPromoteJob).doPromoteInactiveTestToQaAndProd(TEST_NAME, USERNAME, PASSWORD, AUTHOR,
                        REQUEST_PARAMETER_MAP, backgroundJob, TRUNK_REVISION, QA_REVISION, PROD_REVISION);
                Mockito.reset(editAndPromoteJob);
                Mockito.reset(backgroundJob);
            }

            { // testing existingTestDefinition is not null
                final double[] rangeOne = {.3, .7};
                final double[] rangeTwo = {.2, .8};
                final TestDefinition testDefinitionToUpdate = createTestDefinition("control:0,active:1", rangeOne);
                final TestDefinition existingTestDefinition = createTestDefinition("control:0,active:1", rangeTwo);
                doNothing().when(backgroundJob).log(anyString());
                doNothing().when(editAndPromoteJob).doPromoteExistingTestToQaAndProd(TEST_NAME, USERNAME, PASSWORD, AUTHOR,
                        testDefinitionToUpdate, REQUEST_PARAMETER_MAP, backgroundJob, TRUNK_REVISION, QA_REVISION,
                        PROD_REVISION, existingTestDefinition);

                // Act
                editAndPromoteJob.doPromoteTestToQaAndProd(TEST_NAME, USERNAME, PASSWORD, AUTHOR, testDefinitionToUpdate,
                        PREVIOUS_REVISION, REQUEST_PARAMETER_MAP, backgroundJob, trunkStore, QA_REVISION, PROD_REVISION,
                        existingTestDefinition);

                // Assert
                verify(backgroundJob, never()).log(anyString());
                verify(editAndPromoteJob).doPromoteExistingTestToQaAndProd(TEST_NAME, USERNAME, PASSWORD, AUTHOR,
                        testDefinitionToUpdate, REQUEST_PARAMETER_MAP, backgroundJob, TRUNK_REVISION, QA_REVISION,
                        PROD_REVISION, existingTestDefinition);
                Mockito.reset(editAndPromoteJob);
                Mockito.reset(backgroundJob);
            }
        }

        @Test
        public void testDoPromoteInactiveTestToQaAndProd() throws Exception {
            { // testing promoting QA fails
                // Arrange
                mockDoPromoteInternal(false, true, true);
                mockDoPromoteInternal(false, false, true);

                // Act / Assert
                assertThatThrownBy(() ->
                        editAndPromoteJob.doPromoteInactiveTestToQaAndProd(TEST_NAME, USERNAME, PASSWORD, AUTHOR,
                                REQUEST_PARAMETER_MAP, backgroundJob, TRUNK_REVISION, QA_REVISION, PROD_REVISION)
                );
                verify(editAndPromoteJob).doPromoteInternal(TEST_NAME, USERNAME, PASSWORD, AUTHOR, Environment.WORKING,
                        TRUNK_REVISION, Environment.QA, QA_REVISION, REQUEST_PARAMETER_MAP, backgroundJob, true);
                verify(editAndPromoteJob, never()).doPromoteInternal(TEST_NAME, USERNAME, PASSWORD, AUTHOR, Environment.WORKING,
                        TRUNK_REVISION, Environment.PRODUCTION, PROD_REVISION, REQUEST_PARAMETER_MAP, backgroundJob, true);
                Mockito.reset(editAndPromoteJob);
            }

            { // testing promoting QA succeeds
                // Arrange
                mockDoPromoteInternal(true, true, true);
                mockDoPromoteInternal(true, false, true);

                // Act
                editAndPromoteJob.doPromoteInactiveTestToQaAndProd(TEST_NAME, USERNAME, PASSWORD, AUTHOR,
                        REQUEST_PARAMETER_MAP, backgroundJob, TRUNK_REVISION, QA_REVISION, PROD_REVISION);

                // Assert
                verify(editAndPromoteJob).doPromoteInternal(TEST_NAME, USERNAME, PASSWORD, AUTHOR, Environment.WORKING,
                        TRUNK_REVISION, Environment.QA, QA_REVISION, REQUEST_PARAMETER_MAP, backgroundJob, true);
                verify(editAndPromoteJob).doPromoteInternal(TEST_NAME, USERNAME, PASSWORD, AUTHOR, Environment.WORKING,
                        TRUNK_REVISION, Environment.PRODUCTION, PROD_REVISION, REQUEST_PARAMETER_MAP, backgroundJob, true);
                Mockito.reset(editAndPromoteJob);
            }
        }

        @Test
        public void testDoPromoteExistingTestToQaAndProd() throws Exception {
            { // testing isAllocationOnlyChange is false
                // Arrange
                final double[] rangeOne = {1.0};
                final double[] rangeTwo = {.5, .5};
                final TestDefinition testDefinitionToUpdate = createTestDefinition("testbuck:0", rangeOne);
                final TestDefinition existingTestDefinition = createTestDefinition("testbuck:0,control:1", rangeTwo);

                mockDoPromoteInternal(false, true, true);
                mockDoPromoteInternal(false, false, true);

                // Act / Assert
                assertThatThrownBy(() ->
                        editAndPromoteJob.doPromoteExistingTestToQaAndProd(TEST_NAME, USERNAME, PASSWORD, AUTHOR, testDefinitionToUpdate,
                                REQUEST_PARAMETER_MAP, backgroundJob, TRUNK_REVISION, QA_REVISION, PROD_REVISION, existingTestDefinition)
                );
                verify(editAndPromoteJob, never()).doPromoteInternal(TEST_NAME, USERNAME, PASSWORD, AUTHOR, Environment.WORKING,
                        TRUNK_REVISION, Environment.QA, QA_REVISION, REQUEST_PARAMETER_MAP, backgroundJob, true);
                verify(editAndPromoteJob, never()).doPromoteInternal(TEST_NAME, USERNAME, PASSWORD, AUTHOR, Environment.WORKING,
                        TRUNK_REVISION, Environment.PRODUCTION, PROD_REVISION, REQUEST_PARAMETER_MAP, backgroundJob, true);
                Mockito.reset(editAndPromoteJob);
            }

            { // testing isQAPromotable is false
                // Arrange
                final double[] rangeOne = {.7, .3};
                final double[] rangeTwo = {.5, .5};
                final double[] rangeThree = {.3, .4, .3};
                final TestDefinition testDefinitionToUpdate = createTestDefinition("testbuck:0,control:1", rangeOne);
                final TestDefinition existingTestDefinition = createTestDefinition("testbuck:0,control:1", rangeTwo);
                final TestDefinition existingQaTestDefinition = createTestDefinition("testbuck:0,control:1,testbuck2:2", rangeThree);

                mockDoPromoteInternal(false, true, true);
                mockDoPromoteInternal(false, false, true);

                when(qaStore.getCurrentTestDefinition(TEST_NAME)).thenReturn(existingQaTestDefinition);

                // Act / Assert
                assertThatThrownBy(() ->
                        editAndPromoteJob.doPromoteExistingTestToQaAndProd(TEST_NAME, USERNAME, PASSWORD, AUTHOR, testDefinitionToUpdate,
                                REQUEST_PARAMETER_MAP, backgroundJob, TRUNK_REVISION, QA_REVISION, PROD_REVISION, existingTestDefinition)
                );
                verify(editAndPromoteJob, never()).doPromoteInternal(TEST_NAME, USERNAME, PASSWORD, AUTHOR, Environment.WORKING,
                        TRUNK_REVISION, Environment.QA, QA_REVISION, REQUEST_PARAMETER_MAP, backgroundJob, true);
                verify(editAndPromoteJob, never()).doPromoteInternal(TEST_NAME, USERNAME, PASSWORD, AUTHOR, Environment.WORKING,
                        TRUNK_REVISION, Environment.PRODUCTION, PROD_REVISION, REQUEST_PARAMETER_MAP, backgroundJob, true);
                Mockito.reset(editAndPromoteJob);
            }

            { // testing promoting QA fails
                // Arrange
                final double[] rangeOne = {.7, .3};
                final double[] rangeTwo = {.5, .5};
                final TestDefinition testDefinitionToUpdate = createTestDefinition("testbuck:0,control:1", rangeOne);
                final TestDefinition existingTestDefinition = createTestDefinition("testbuck:0,control:1", rangeTwo);

                mockDoPromoteInternal(false, true, true);
                mockDoPromoteInternal(false, false,true);

                when(qaStore.getCurrentTestDefinition(TEST_NAME)).thenReturn(existingTestDefinition);

                // Act / Assert
                assertThatThrownBy(() ->
                        editAndPromoteJob.doPromoteExistingTestToQaAndProd(TEST_NAME, USERNAME, PASSWORD, AUTHOR, testDefinitionToUpdate,
                                REQUEST_PARAMETER_MAP, backgroundJob, TRUNK_REVISION, QA_REVISION, PROD_REVISION, existingTestDefinition)
                );
                verify(editAndPromoteJob).doPromoteInternal(TEST_NAME, USERNAME, PASSWORD, AUTHOR, Environment.WORKING,
                        TRUNK_REVISION, Environment.QA, QA_REVISION, REQUEST_PARAMETER_MAP, backgroundJob, true);
                verify(editAndPromoteJob, never()).doPromoteInternal(TEST_NAME, USERNAME, PASSWORD, AUTHOR, Environment.WORKING,
                        TRUNK_REVISION, Environment.PRODUCTION, PROD_REVISION, REQUEST_PARAMETER_MAP, backgroundJob, true);
                Mockito.reset(editAndPromoteJob);
            }

            { // testing promoting QA succeeds
                // Arrange
                final double[] rangeOne = {.7, .3};
                final double[] rangeTwo = {.5, .5};
                final TestDefinition testDefinitionToUpdate = createTestDefinition("testbuck:0,control:1", rangeOne);
                final TestDefinition existingTestDefinition = createTestDefinition("testbuck:0,control:1", rangeTwo);

                mockDoPromoteInternal(true, true, true);
                mockDoPromoteInternal(true, false, true);

                when(qaStore.getCurrentTestDefinition(TEST_NAME)).thenReturn(existingTestDefinition);
                when(productionStore.getCurrentTestDefinition(TEST_NAME)).thenReturn(existingTestDefinition);

                // Act
                editAndPromoteJob.doPromoteExistingTestToQaAndProd(TEST_NAME, USERNAME, PASSWORD, AUTHOR, testDefinitionToUpdate,
                        REQUEST_PARAMETER_MAP, backgroundJob, TRUNK_REVISION, QA_REVISION, PROD_REVISION, existingTestDefinition);

                // Assert
                verify(editAndPromoteJob).doPromoteInternal(TEST_NAME, USERNAME, PASSWORD, AUTHOR, Environment.WORKING,
                        TRUNK_REVISION, Environment.QA, QA_REVISION, REQUEST_PARAMETER_MAP, backgroundJob, true);
                verify(editAndPromoteJob).doPromoteInternal(TEST_NAME, USERNAME, PASSWORD, AUTHOR, Environment.WORKING,
                        TRUNK_REVISION, Environment.PRODUCTION, PROD_REVISION, REQUEST_PARAMETER_MAP, backgroundJob, true);
                Mockito.reset(editAndPromoteJob);
            }
        }

        @Test
        public void testDoPromoteTestToEnvSuccess() throws Exception {
            { // Succeed to promote to QA
                assertDoPromoteTestToEnvironment(Environment.QA, QA_REVISION, qaStore);
            }
            { // Succeed to promote to Prod
                assertDoPromoteTestToEnvironment(Environment.PRODUCTION, PROD_REVISION, productionStore);
            }
        }

        @Test
        public void testDoPromoteTestToEnvironmentWithoutIsAllocationOnlyChangeValidation() throws Exception {
            // Arrange
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.8, .2};
            final TestDefinition testDefinitionToUpdate = createTestDefinition("testbuck:0,control:1", rangeOne);
            final TestDefinition existingTestDefinition = createTestDefinition("testbuck2:0,control:1", rangeTwo);

            doNothing().when(backgroundJob).log(anyString());
            when(qaStore.getCurrentTestDefinition(TEST_NAME)).thenReturn(existingTestDefinition);
            mockDoPromoteInternal(true, true, true);

            // Act
            editAndPromoteJob.doPromoteTestToEnvironment(Environment.QA, TEST_NAME, USERNAME, PASSWORD, AUTHOR,
                    testDefinitionToUpdate, REQUEST_PARAMETER_MAP, backgroundJob, TRUNK_REVISION, QA_REVISION, false);

            // Assert
            verify(backgroundJob).log(anyString());
            verify(editAndPromoteJob).doPromoteInternal(TEST_NAME, USERNAME, PASSWORD, AUTHOR, Environment.WORKING,
                    TRUNK_REVISION, Environment.QA, QA_REVISION, REQUEST_PARAMETER_MAP, backgroundJob, true);
        }

        @Test
        public void testDoPromoteTestToEnvFailToPromoteToTrunk() throws Exception {
            // Arrange
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.8, .2};
            final TestDefinition testDefinitionToUpdate = createTestDefinition("testbuck:0,control:1", rangeOne);
            final TestDefinition existingTestDefinition = createTestDefinition("testbuck:0,control:1", rangeTwo);
            final Environment targetEnv = Environment.WORKING;

            doNothing().when(backgroundJob).log(anyString());
            when(trunkStore.getCurrentTestDefinition(TEST_NAME)).thenReturn(existingTestDefinition);

            // Act / Assert
            assertThatThrownBy(() ->
                    editAndPromoteJob.doPromoteTestToEnvironment(targetEnv, TEST_NAME, USERNAME, PASSWORD, AUTHOR,
                            testDefinitionToUpdate, REQUEST_PARAMETER_MAP, backgroundJob, TRUNK_REVISION, TRUNK_REVISION, true)
            );
            verify(backgroundJob, never()).log(anyString());
            verify(editAndPromoteJob, never()).doPromoteInternal(TEST_NAME, USERNAME, PASSWORD, AUTHOR, Environment.WORKING,
                    TRUNK_REVISION, targetEnv, TRUNK_REVISION, REQUEST_PARAMETER_MAP, backgroundJob, true);
        }

        @Test
        public void testDoPromoteTestToEnvFailToPromoteNonAllocationChange() throws Exception {
            // Arrange
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.8, .2};
            final TestDefinition testDefinitionToUpdate = createTestDefinition("testbuck:0,control:1", rangeOne);
            final TestDefinition existingTestDefinition = createTestDefinition("testbuck2:0,control:1", rangeTwo);

            doNothing().when(backgroundJob).log(anyString());
            when(qaStore.getCurrentTestDefinition(TEST_NAME)).thenReturn(existingTestDefinition);

            // Act / Assert
            assertThatThrownBy(() ->
                    editAndPromoteJob.doPromoteTestToEnvironment(Environment.QA, TEST_NAME, USERNAME, PASSWORD, AUTHOR,
                            testDefinitionToUpdate, REQUEST_PARAMETER_MAP, backgroundJob, TRUNK_REVISION, QA_REVISION, true)
            );
            verify(backgroundJob, never()).log(anyString());
            verify(editAndPromoteJob, never()).doPromoteInternal(TEST_NAME, USERNAME, PASSWORD, AUTHOR, Environment.WORKING,
                    TRUNK_REVISION, Environment.QA, QA_REVISION, REQUEST_PARAMETER_MAP, backgroundJob, true);
        }

        @Test
        public void testDoPromoteTestToEnvFailToPromoteToUnknownRevision() throws Exception {
            // Arrange
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.8, .2};
            final TestDefinition testDefinitionToUpdate = createTestDefinition("testbuck:0,control:1", rangeOne);
            final TestDefinition existingTestDefinition = createTestDefinition("testbuck:0,control:1", rangeTwo);

            doNothing().when(backgroundJob).log(anyString());
            when(qaStore.getCurrentTestDefinition(TEST_NAME)).thenReturn(existingTestDefinition);

            // Act / Assert
            assertThatThrownBy(() ->
                    editAndPromoteJob.doPromoteTestToEnvironment(Environment.QA, TEST_NAME, USERNAME, PASSWORD, AUTHOR,
                            testDefinitionToUpdate, REQUEST_PARAMETER_MAP, backgroundJob, TRUNK_REVISION,
                            EnvironmentVersion.UNKNOWN_REVISION, true)
            );
            verify(backgroundJob, never()).log(anyString());
            verify(editAndPromoteJob, never()).doPromoteInternal(TEST_NAME, USERNAME, PASSWORD, AUTHOR, Environment.WORKING,
                        TRUNK_REVISION, Environment.QA, EnvironmentVersion.UNKNOWN_REVISION, REQUEST_PARAMETER_MAP, backgroundJob, true);
        }

        private void assertDoPromoteTestToEnvironment(
                final Environment targetEnv,
                final String targetRevision,
                final ProctorStore targetStore
        ) throws Exception {
            // Arrange
            final boolean isPromoteToQa = targetEnv == Environment.QA;
            final double[] rangeOne = {.7, .3};
            final double[] rangeTwo = {.8, .2};
            final TestDefinition testDefinitionToUpdate = createTestDefinition("testbuck:0,control:1", rangeOne);
            final TestDefinition existingTestDefinition = createTestDefinition("testbuck:0,control:1", rangeTwo);

            doNothing().when(backgroundJob).log(anyString());
            when(targetStore.getCurrentTestDefinition(TEST_NAME)).thenReturn(existingTestDefinition);
            mockDoPromoteInternal(true, isPromoteToQa, true);

            // Act
            editAndPromoteJob.doPromoteTestToEnvironment(targetEnv, TEST_NAME, USERNAME, PASSWORD, AUTHOR,
                    testDefinitionToUpdate, REQUEST_PARAMETER_MAP, backgroundJob, TRUNK_REVISION, targetRevision, true);

            // Assert
            verify(backgroundJob).log(anyString());
            verify(editAndPromoteJob).doPromoteInternal(TEST_NAME, USERNAME, PASSWORD, AUTHOR, Environment.WORKING,
                    TRUNK_REVISION, targetEnv, targetRevision, REQUEST_PARAMETER_MAP, backgroundJob, true);
            Mockito.reset(backgroundJob);
            Mockito.reset(editAndPromoteJob);
        }
    }

    private static TestDefinition createTestDefinition(final String bucketsString, final double[] ranges) {
        return createTestDefinition(bucketsString, TestType.RANDOM, "salt", ranges);
    }

    private static TestDefinition createTestDefinition(
            final String bucketsString,
            final TestType testType,
            final double[] ranges
    ) {
        return createTestDefinition(bucketsString, testType, "salt", ranges);
    }

    private static TestDefinition createTestDefinition(
            final String bucketsString,
            final TestType testType,
            final String salt,
            final double[] ranges
    ) {
        return createTestDefinition(bucketsString, testType, salt, ranges, null);
    }

    private static TestDefinition createTestDefinition(
            final String bucketsString,
            final TestType testType,
            final String salt,
            final double[] ranges,
            final Payload[] payloads
    ) {
        return createTestDefinition(bucketsString, testType, salt, ranges, payloads, null);
    }

    private static TestDefinition createTestDefinition(
            final String bucketsString,
            final double[] ranges,
            final List<String> allocationIds
    ) {
        return createTestDefinition(bucketsString, TestType.RANDOM, "salt", ranges, null, allocationIds);
    }

    private static TestDefinition createTestDefinition(
            final String bucketsString,
            final TestType testType,
            final String salt,
            final double[] ranges,
            final Payload[] payloads,
            final List<String> allocationIds
    ) {
        final List<Range> rangeList = new ArrayList<Range>();
        final String[] buckets = bucketsString.split(",");
        final List<TestBucket> buckList = new ArrayList<TestBucket>();

        for (int i = 0; i < buckets.length; i++) {
            final String bucket = buckets[i];
            final int colonInd = bucket.indexOf(':');
            final int bucketValue = Integer.parseInt(bucket.substring(colonInd + 1));
            final TestBucket tempBucket = new TestBucket(
                    bucket.substring(0, colonInd),
                    bucketValue,
                    "description",
                    (payloads == null) ? null : payloads[i]
            );
            buckList.add(tempBucket);
            final double range = i >= ranges.length ? 0 : ranges[i];
            rangeList.add(new Range(bucketValue, range));
        }

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
}
