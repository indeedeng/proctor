package com.indeed.proctor.common.dynamic;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestType;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class TestDynamicFilters {
    @Test
    public void testEmptySetOfFilters() {
        final DynamicFilters filters = new DynamicFilters();
        assertEquals(
                Collections.emptySet(),
                filters.determineTests(
                        Collections.<String, ConsumableTestDefinition>emptyMap(),
                        Collections.<String>emptySet()
                )
        );
        assertEquals(
                Collections.emptySet(),
                filters.determineTests(
                        ImmutableMap.of(
                                "sometest", new ConsumableTestDefinition(),
                                "sometest2", new ConsumableTestDefinition()
                        ),
                        Sets.newHashSet(
                                "sometest"
                        )
                )
        );
    }

    @Test
    public void testRequiredTestsShouldBeIgnored() {
        final DynamicFilters filters = new DynamicFilters(Arrays.asList(new AnyMatchFilter()));
        assertEquals(
                Collections.emptySet(),
                filters.determineTests(
                        Collections.<String, ConsumableTestDefinition>emptyMap(),
                        Collections.<String>emptySet()
                )
        );
        assertEquals(
                Sets.newHashSet("non-required"),
                filters.determineTests(
                        ImmutableMap.of(
                                "non-required", new ConsumableTestDefinition(),
                                "required", new ConsumableTestDefinition()
                        ),
                        Sets.newHashSet(
                                "required"
                        )
                )
        );
    }

    @Test
    public void testPrefixFilters() {
        final DynamicFilters filters = new DynamicFilters(
                Arrays.asList(
                        new TestNamePrefixFilter("abc_"),
                        new TestNamePrefixFilter("def_"),
                        new TestNamePrefixFilter("abc_def_")
                )
        );

        assertEquals(
                Sets.newHashSet("abc_def_ghi", "def_ghi", "abc_"),
                filters.determineTests(
                        ImmutableMap.of(
                                "abc_", new ConsumableTestDefinition(),
                                "def_ghi", new ConsumableTestDefinition(),
                                "abc_def_ghi", new ConsumableTestDefinition(),
                                "abcdef", new ConsumableTestDefinition(),
                                "ghi_abc", new ConsumableTestDefinition()
                        ),
                        Collections.<String>emptySet()
                )
        );
    }

    @Test
    public void testTestTypeFilters() {
        final DynamicFilters filters = new DynamicFilters(
                Arrays.asList(
                        new TestTypeFilter(TestType.RANDOM),
                        new TestTypeFilter(TestType.EMAIL_ADDRESS)
                )
        );

        assertEquals(
                Sets.newHashSet("random", "email"),
                filters.determineTests(
                        ImmutableMap.of(
                                "random", constructTestDefinition(TestType.RANDOM),
                                "email", constructTestDefinition(TestType.EMAIL_ADDRESS),
                                "user", constructTestDefinition(TestType.ANONYMOUS_USER),
                                "account", constructTestDefinition(TestType.AUTHENTICATED_USER)
                        ),
                        Collections.<String>emptySet()
                )
        );
    }

    private ConsumableTestDefinition constructTestDefinition(final TestType testType) {
        final ConsumableTestDefinition testDefinition = new ConsumableTestDefinition();
        testDefinition.setTestType(testType);
        return testDefinition;
    }

    private static class AnyMatchFilter implements DynamicFilter {
        @Override
        public boolean matches(final String testName, final ConsumableTestDefinition testDefinition) {
            return true;
        }
    }

    private static class TestTypeFilter implements DynamicFilter {
        final TestType testType;

        public TestTypeFilter(final TestType testType) {
            this.testType = testType;
        }

        @Override
        public boolean matches(final String testName, final ConsumableTestDefinition testDefinition) {
            return testDefinition.getTestType().equals(testType);
        }
    }
}
