package com.indeed.proctor.store;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GitProctorCoreTest {

    @Test
    public void testParseTestName() {
        assertEquals(
                "testname",
                GitProctorCore.parseTestName("matrices/test-definitions", "matrices/test-definitions/testname/definition.json")
        );
        assertEquals(
                "testname",
                GitProctorCore.parseTestName("matrices/test-definitions", "matrices/test-definitions/testname/metadata.json")
        );
        assertEquals(
                "testname",
                GitProctorCore.parseTestName("test-definitions", "test-definitions/testname/definition.json")
        );
        assertEquals(
                "testname",
                GitProctorCore.parseTestName("", "testname/definition.json")
        );
        assertNull(GitProctorCore.parseTestName("matrices/test-definitions", ""));
        assertNull(GitProctorCore.parseTestName("matrices/test-definitions", "some_file.txt"));
        assertEquals(
                "testname",
                GitProctorCore.parseTestName("matrices/test-definitions", "matrices/test-definitions/testname/some/file")
        );
        assertEquals(
                null,
                GitProctorCore.parseTestName("matrices/test-definitions", "matrices/test-definitions/filename")
        );
    }
}