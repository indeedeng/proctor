package com.indeed.proctor.webapp.jobs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.indeed.proctor.common.IncompatibleTestMatrixException;
import com.indeed.proctor.common.ProctorLoadResult;
import com.indeed.proctor.webapp.model.AppVersion;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class MatrixCheckerTest {
    @Test
    public void testGetErrorMessage_loadError() {
        final AppVersion appVersion = new AppVersion("TestApp", "a5efcc49c813e56aa7e35333aa8590515f5c061e");
        final ProctorLoadResult result = new ProctorLoadResult(
                ImmutableMap.of(
                        "check_tst",
                        new IncompatibleTestMatrixException("Invalid test rule ${abc}")
                ),
                Collections.emptySet(),
                true
        );
        assertEquals(
                "TestApp@a5efcc49c813e56aa7e35333aa8590515f5c061e cannot load test 'check_tst': Invalid test rule ${abc}",
                MatrixChecker.getErrorMessage(appVersion, result)
        );
    }

    @Test
    public void testGetErrorMessage_missingTest() {
        final AppVersion appVersion = new AppVersion("TestApp", "a5efcc49c813e56aa7e35333aa8590515f5c061e");
        final ProctorLoadResult result = new ProctorLoadResult(
                Collections.emptyMap(),
                ImmutableSet.of("check_tst"),
                true
        );
        assertEquals(
                "TestApp@a5efcc49c813e56aa7e35333aa8590515f5c061e requires test 'check_tst'",
                MatrixChecker.getErrorMessage(appVersion, result)
        );
    }

    @Test
    public void testGetErrorMessage_success() {
        final AppVersion appVersion = new AppVersion("TestApp", "a5efcc49c813e56aa7e35333aa8590515f5c061e");
        final ProctorLoadResult result = new ProctorLoadResult(
                Collections.emptyMap(),
                Collections.emptySet(),
                true
        );
        assertEquals(
                "",
                MatrixChecker.getErrorMessage(appVersion, result)
        );
    }
}