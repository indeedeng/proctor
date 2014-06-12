package com.indeed.proctor.common.model;

import org.junit.Test;

import javax.annotation.Nonnull;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class TestTypeTest {
    @Test
    public void testExtensibility() {
        // Note that something has to trigger the registration of the extended test type
        final TestType ipAddress = TestType.register("IP_ADDRESS");
        assertTrue(TestType.all().contains(ipAddress));
        assertTrue(TestType.all().contains(TestType.ANONYMOUS_USER));
    }
}
