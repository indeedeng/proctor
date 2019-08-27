package com.indeed.proctor.webapp.controllers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.indeed.proctor.common.PayloadSpecification;
import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.common.TestSpecification;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.model.AppVersion;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestProctorController {
    private static final String TEST_NAME = "atest";

    private TestMatrixArtifact constructArtifact() {
        final TestMatrixArtifact artifact = new TestMatrixArtifact();
        final Audit audit = new Audit();
        audit.setUpdated(0);
        audit.setUpdatedBy("junit");
        audit.setVersion(Audit.EMPTY_VERSION);
        artifact.setAudit(audit);

        final Payload payloadInactive = new Payload();
        payloadInactive.setLongValue(-1L);
        final Payload payloadControl = new Payload();
        payloadControl.setLongValue(0L);

        final ConsumableTestDefinition testDefinition = ProctorUtils.convertToConsumableTestDefinition(
                new TestDefinition(
                        "-1",
                        "lang == 'en'",
                        TestType.ANONYMOUS_USER,
                        "&" + TEST_NAME,
                        Lists.newArrayList(
                                new TestBucket(
                                        "inactive",
                                        -1,
                                        "inactive",
                                        payloadInactive
                                ),
                                new TestBucket(
                                        "control",
                                        0,
                                        "control",
                                        payloadControl
                                )
                        ),
                        Lists.newArrayList(
                                new Allocation(
                                        null,
                                        Lists.newArrayList(
                                                new Range(
                                                        -1,
                                                        1.0
                                                )
                                        ),
                                        "#A1"
                                )
                        ),
                        false,
                        Collections.emptyMap(),
                        Collections.emptyMap(),
                        "test for a unit test",
                        Lists.newArrayList(TEST_NAME + "_tag")
                )
        );

        artifact.setTests(ImmutableMap.of(TEST_NAME, testDefinition));

        return artifact;
    }

    @Test
    public void testCompatibleSpecificationResultFromRequiredTest() {
        final TestSpecification testSpecification = new TestSpecification();
        testSpecification.setFallbackValue(-1);
        final PayloadSpecification payloadSpecification = new PayloadSpecification();
        payloadSpecification.setType("longValue");
        testSpecification.setPayload(payloadSpecification);
        testSpecification.setBuckets(
                ImmutableMap.of(
                        "inactive", -1,
                        "control", 0
                )
        );
        final AppVersion appVersion = new AppVersion("sample application", "v1");
        final ProctorController.CompatibleSpecificationResult result =
                ProctorController.CompatibleSpecificationResult.fromRequiredTest(
                        Environment.PRODUCTION,
                        appVersion,
                        constructArtifact(),
                        TEST_NAME,
                        testSpecification
                );
        assertTrue(result.isCompatible());
        assertFalse(result.isDynamicTest(TEST_NAME));
        assertTrue(StringUtils.isEmpty(result.getError()));
        assertEquals(appVersion, result.getAppVersion());
    }

    @Test
    public void testCompatibleSpecificationResultFromDynamicTest() {
        final AppVersion appVersion = new AppVersion("sample application", "v1");
        final ProctorController.CompatibleSpecificationResult result =
                ProctorController.CompatibleSpecificationResult.fromDynamicTest(
                        Environment.PRODUCTION,
                        appVersion,
                        constructArtifact(),
                        TEST_NAME
                );
        assertTrue(result.isCompatible());
        assertTrue(result.isDynamicTest(TEST_NAME));
        assertTrue(StringUtils.isEmpty(result.getError()));
        assertEquals(appVersion, result.getAppVersion());
    }
}
