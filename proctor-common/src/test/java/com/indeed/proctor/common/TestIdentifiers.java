package com.indeed.proctor.common;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.model.TestType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;
import java.util.Objects;

public class TestIdentifiers {
    @Test(expected = IllegalArgumentException.class)
    public void testNoRandom() {
        new Identifiers(
                ImmutableMap.of(
                        TestType.RANDOM, "identifier1",
                        TestType.EMAIL_ADDRESS, "test@test.com"),
                true);
    }

    @Test
    public void testEqualsAndHashCodeUsesRandom() {
        final Map<TestType, String> identifierMap =
                ImmutableMap.of(
                        TestType.EMAIL_ADDRESS, "test@test.com",
                        TestType.ANONYMOUS_USER, "0000000000000000");
        final Identifiers a = new Identifiers(identifierMap, true);
        final Identifiers b = new Identifiers(identifierMap, false);

        Assert.assertFalse(Objects.equals(a, b));
        Assert.assertFalse(a.hashCode() == b.hashCode());
    }

    @Test
    public void testEqualsAndHashCodeUsesMap() {
        final Identifiers a =
                new Identifiers(
                        ImmutableMap.of(
                                TestType.EMAIL_ADDRESS, "test@test.com",
                                TestType.ANONYMOUS_USER, "0000000000000000"),
                        true);

        final Identifiers b =
                new Identifiers(
                        ImmutableMap.of(
                                TestType.EMAIL_ADDRESS, "test@test.com",
                                TestType.ANONYMOUS_USER, "1111111111111111"),
                        true);

        Assert.assertFalse(Objects.equals(a, b));
        Assert.assertFalse(a.hashCode() == b.hashCode());
    }

    @Test
    public void testEqualsAndHashCode() {
        final Identifiers a =
                new Identifiers(
                        ImmutableMap.of(
                                TestType.EMAIL_ADDRESS, "test@test.com",
                                TestType.ANONYMOUS_USER, "0000000000000000"),
                        true);

        final Identifiers b =
                new Identifiers(
                        ImmutableMap.of(
                                TestType.EMAIL_ADDRESS, "test@test.com",
                                TestType.ANONYMOUS_USER, "0000000000000000"),
                        true);

        Assert.assertNotSame(a, b);
        Assert.assertTrue(Objects.equals(a, b));
        Assert.assertTrue(a.hashCode() == b.hashCode());
    }
}
