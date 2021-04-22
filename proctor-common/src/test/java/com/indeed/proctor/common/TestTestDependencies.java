package com.indeed.proctor.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestDependency;
import com.indeed.proctor.common.model.TestType;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestTestDependencies {
    private static final String TEST_NAME = "example_tst";
    private static final String PARENT_TEST_NAME = "parent_tst";
    private static final int BUCKET_VALUE = 1;

    @Test
    public void testValidateDependencyAndReturnErrorReason_valid() {
        final ConsumableTestDefinition definition = ConsumableTestDefinition.fromTestDefinition(
                stubTestDefinition(TEST_NAME)
                        .setDependency(new TestDependency(PARENT_TEST_NAME, BUCKET_VALUE))
                        .build()
        );
        final ConsumableTestDefinition parentDefinition = ConsumableTestDefinition.fromTestDefinition(
                stubTestDefinition(PARENT_TEST_NAME).build()
        );
        assertThat(TestDependencies.validateDependencyAndReturnReason(
                TEST_NAME,
                definition,
                ImmutableMap.of(TEST_NAME, definition, PARENT_TEST_NAME, parentDefinition)
        )).isEmpty();
    }

    @Test
    public void testValidateDependencyAndReturnErrorReason_noDependency() {
        final ConsumableTestDefinition definition = ConsumableTestDefinition.fromTestDefinition(
                stubTestDefinition(TEST_NAME)
                        .build()
        );
        assertThat(TestDependencies.validateDependencyAndReturnReason(
                TEST_NAME,
                definition,
                ImmutableMap.of(TEST_NAME, definition)
        )).isEmpty();
    }

    @Test
    public void testValidateDependencyAndReturnErrorReason_unknownTest() {
        final ConsumableTestDefinition definition = ConsumableTestDefinition.fromTestDefinition(
                stubTestDefinition(TEST_NAME)
                        .setDependency(new TestDependency("___dummy", BUCKET_VALUE))
                        .build()
        );
        final ConsumableTestDefinition parentDefinition = ConsumableTestDefinition.fromTestDefinition(
                stubTestDefinition(PARENT_TEST_NAME).build()
        );
        assertThat(TestDependencies.validateDependencyAndReturnReason(
                TEST_NAME,
                definition,
                ImmutableMap.of(TEST_NAME, definition, PARENT_TEST_NAME, parentDefinition)
        )).hasValue("A test example_tst depends on an unknown or incompatible test ___dummy");
    }

    @Test
    public void testValidateDependencyAndReturnErrorReason_unknownBucket() {
        final int unknownBucketValue = 101;
        final ConsumableTestDefinition definition = ConsumableTestDefinition.fromTestDefinition(
                stubTestDefinition(TEST_NAME)
                        .setDependency(new TestDependency(PARENT_TEST_NAME, unknownBucketValue))
                        .build()
        );
        final ConsumableTestDefinition parentDefinition = ConsumableTestDefinition.fromTestDefinition(
                stubTestDefinition(PARENT_TEST_NAME).build()
        );
        assertThat(TestDependencies.validateDependencyAndReturnReason(
                TEST_NAME,
                definition,
                ImmutableMap.of(TEST_NAME, definition, PARENT_TEST_NAME, parentDefinition)
        )).hasValue("A test example_tst depends on an undefined bucket 101");
    }

    @Test
    public void testValidateDependencyAndReturnErrorReason_differentTestType() {
        final ConsumableTestDefinition definition = ConsumableTestDefinition.fromTestDefinition(
                stubTestDefinition(TEST_NAME)
                        .setTestType(TestType.ANONYMOUS_USER)
                        .setDependency(new TestDependency(PARENT_TEST_NAME, BUCKET_VALUE))
                        .build()
        );
        final ConsumableTestDefinition parentDefinition = ConsumableTestDefinition.fromTestDefinition(
                stubTestDefinition(PARENT_TEST_NAME)
                        .setTestType(TestType.AUTHENTICATED_USER)
                        .build()
        );
        assertThat(TestDependencies.validateDependencyAndReturnReason(
                TEST_NAME,
                definition,
                ImmutableMap.of(TEST_NAME, definition, PARENT_TEST_NAME, parentDefinition)
        )).hasValue("A test example_tst depends on parent_tst with different test type: expected USER but ACCOUNT");
    }

    @Test
    public void testValidateDependencyAndReturnErrorReason_sameSalt() {
        final String sharedSalt = "&shared_salt";
        final ConsumableTestDefinition definition = ConsumableTestDefinition.fromTestDefinition(
                stubTestDefinition(TEST_NAME)
                        .setSalt(sharedSalt)
                        .setDependency(new TestDependency(PARENT_TEST_NAME, BUCKET_VALUE))
                        .build()
        );
        final ConsumableTestDefinition parentDefinition = ConsumableTestDefinition.fromTestDefinition(
                stubTestDefinition(PARENT_TEST_NAME)
                        .setSalt(sharedSalt)
                        .build()
        );
        assertThat(TestDependencies.validateDependencyAndReturnReason(
                TEST_NAME,
                definition,
                ImmutableMap.of(TEST_NAME, definition, PARENT_TEST_NAME, parentDefinition)
        )).hasValue("A test example_tst depends on parent_tst with the same salt: &shared_salt");
    }

    @Test
    public void testValidateDependencyAndReturnErrorReason_negativeBucket() {
        final ConsumableTestDefinition definition = ConsumableTestDefinition.fromTestDefinition(
                stubTestDefinition(TEST_NAME)
                        .setDependency(new TestDependency(PARENT_TEST_NAME, -1))
                        .build()
        );
        final ConsumableTestDefinition parentDefinition = ConsumableTestDefinition.fromTestDefinition(
                stubTestDefinition(PARENT_TEST_NAME)
                        .setBuckets(ImmutableList.of(new TestBucket("inactive", -1, ""), new TestBucket("active", BUCKET_VALUE, "")))
                        .build()
        );
        assertThat(TestDependencies.validateDependencyAndReturnReason(
                TEST_NAME,
                definition,
                ImmutableMap.of(TEST_NAME, definition, PARENT_TEST_NAME, parentDefinition)
        )).hasValue("A test example_tst depends on negative bucket value -1 of parent_tst");
    }

    @Test
    public void testDetermineEvaluationOrder() {
        // C -> B -> A
        //      ^
        //      |
        // E -> D
        // G -> F
        final List<String> testNames = ImmutableList.of("A", "B", "C", "D", "E", "F", "G");
        final Map<String, String> parentMap = ImmutableMap.of(
                "B", "A",
                "C", "B",
                "D", "B",
                "E", "D",
                "G", "F"
        );

        final Map<String, ConsumableTestDefinition> tests = constructTests(testNames, parentMap);

        final List<String> order = TestDependencies.determineEvaluationOrder(tests);

        // assert X is earlier than Y if Y depends on X
        assertThat(order.indexOf("A")).isLessThan(order.indexOf("B"));
        assertThat(order.indexOf("B")).isLessThan(order.indexOf("C"));
        assertThat(order.indexOf("B")).isLessThan(order.indexOf("D"));
        assertThat(order.indexOf("D")).isLessThan(order.indexOf("E"));
        assertThat(order.indexOf("F")).isLessThan(order.indexOf("G"));
    }

    @Test
    public void testDetermineEvaluationOrder_unknownDependency() {
        final List<String> testNames = ImmutableList.of("A", "B");
        final Map<String, String> parentMap = ImmutableMap.of(
                "A", "B",
                "B", "__dummy"
        );

        final Map<String, ConsumableTestDefinition> tests = constructTests(testNames, parentMap);

        assertThatThrownBy(() -> TestDependencies.determineEvaluationOrder(tests))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Detected invalid dependency links. Unable to determine order");
    }

    @Test
    public void testDetermineEvaluationOrder_circularDependency() {
        final List<String> testNames = ImmutableList.of("A", "B");
        final Map<String, String> parentMap = ImmutableMap.of(
                "A", "B",
                "B", "A"
        );

        final Map<String, ConsumableTestDefinition> tests = constructTests(testNames, parentMap);

        assertThatThrownBy(() -> TestDependencies.determineEvaluationOrder(tests))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Detected invalid dependency links. Unable to determine order");
    }

    @Test
    public void testComputeTransitiveDependencies() {
        // C -> B -> A
        //      ^
        //      |
        // E -> D
        // G -> F
        final List<String> testNames = ImmutableList.of("A", "B", "C", "D", "E", "F", "G");
        final Map<String, String> parentMap = ImmutableMap.of(
                "B", "A",
                "C", "B",
                "D", "B",
                "E", "D",
                "G", "F"
        );

        final Map<String, ConsumableTestDefinition> tests = constructTests(testNames, parentMap);

        assertThat(TestDependencies.computeTransitiveDependencies(tests, ImmutableSet.of("A")))
                .containsExactlyInAnyOrder("A");
        assertThat(TestDependencies.computeTransitiveDependencies(tests, ImmutableSet.of("B")))
                .containsExactlyInAnyOrder("B", "A");
        assertThat(TestDependencies.computeTransitiveDependencies(tests, ImmutableSet.of("C")))
                .containsExactlyInAnyOrder("C", "B", "A");
        assertThat(TestDependencies.computeTransitiveDependencies(tests, ImmutableSet.of("D")))
                .containsExactlyInAnyOrder("D", "B", "A");
        assertThat(TestDependencies.computeTransitiveDependencies(tests, ImmutableSet.of("E")))
                .containsExactlyInAnyOrder("E", "D", "B", "A");
        assertThat(TestDependencies.computeTransitiveDependencies(tests, ImmutableSet.of("F")))
                .containsExactlyInAnyOrder("F");
        assertThat(TestDependencies.computeTransitiveDependencies(tests, ImmutableSet.of("G")))
                .containsExactlyInAnyOrder("G", "F");
        assertThat(TestDependencies.computeTransitiveDependencies(tests, ImmutableSet.of("G", "C")))
                .containsExactlyInAnyOrder("G", "F", "C", "B", "A");
        assertThat(TestDependencies.computeTransitiveDependencies(tests, ImmutableSet.of("G", "D", "C")))
                .containsExactlyInAnyOrder("G", "F", "D", "C", "B", "A");
    }

    @Test
    public void testComputeTransitiveDependencies_unknownDependency() {
        final List<String> testNames = ImmutableList.of("A", "B");
        final Map<String, String> parentMap = ImmutableMap.of(
                "A", "B",
                "B", "__dummy"
        );

        final Map<String, ConsumableTestDefinition> tests = constructTests(testNames, parentMap);

        assertThatThrownBy(() -> TestDependencies.computeTransitiveDependencies(tests, ImmutableSet.of("B")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Detected dependency to an unknown test __dummy");
    }

    @Test
    public void testComputeTransitiveDependencies_circularDependency() {
        final List<String> testNames = ImmutableList.of("A", "B");
        final Map<String, String> parentMap = ImmutableMap.of(
                "A", "B",
                "B", "A"
        );

        final Map<String, ConsumableTestDefinition> tests = constructTests(testNames, parentMap);

        // expects no infinite loop.
        assertThat(TestDependencies.computeTransitiveDependencies(tests, ImmutableSet.of("B")))
                .containsExactlyInAnyOrder("B", "A");
    }

    @Test
    public void testComputeMaximumDependencyChains() {
        // C -> B -> A
        //      ^
        //      |
        // E -> D
        // G -> F
        // H
        final List<String> testNames = ImmutableList.of("A", "B", "C", "D", "E", "F", "G", "H");
        final Map<String, String> parentMap = ImmutableMap.of(
                "B", "A",
                "C", "B",
                "D", "B",
                "E", "D",
                "G", "F"
        );

        final Map<String, ConsumableTestDefinition> tests = constructTests(testNames, parentMap);

        assertThat(TestDependencies.computeMaximumDependencyChains(tests, "A")).isEqualTo(3);
        assertThat(TestDependencies.computeMaximumDependencyChains(tests, "B")).isEqualTo(3);
        assertThat(TestDependencies.computeMaximumDependencyChains(tests, "D")).isEqualTo(3);
        assertThat(TestDependencies.computeMaximumDependencyChains(tests, "E")).isEqualTo(3);

        assertThat(TestDependencies.computeMaximumDependencyChains(tests, "C")).isEqualTo(2);

        assertThat(TestDependencies.computeMaximumDependencyChains(tests, "F")).isEqualTo(1);
        assertThat(TestDependencies.computeMaximumDependencyChains(tests, "G")).isEqualTo(1);

        assertThat(TestDependencies.computeMaximumDependencyChains(tests, "H")).isEqualTo(0);
    }

    @Test
    public void testValidateDependenciesAndReturnReasons() {
        final ConsumableTestDefinition definitionA = ConsumableTestDefinition.fromTestDefinition(
                stubTestDefinition("A")
                        .setDependency(new TestDependency("__dummy", BUCKET_VALUE))
                        .build()
        );
        final ConsumableTestDefinition definitionB = ConsumableTestDefinition.fromTestDefinition(
                stubTestDefinition("B")
                        .setDependency(new TestDependency("A", BUCKET_VALUE))
                        .build()
        );
        final ConsumableTestDefinition definitionC = ConsumableTestDefinition.fromTestDefinition(
                stubTestDefinition("C")
                        .setDependency(new TestDependency("C", BUCKET_VALUE))
                        .build()
        );
        final ConsumableTestDefinition definitionD = ConsumableTestDefinition.fromTestDefinition(
                stubTestDefinition("D")
                        .setDependency(new TestDependency("B", BUCKET_VALUE))
                        .build()
        );
        final Map<String, ConsumableTestDefinition> tests = ImmutableMap.of(
                "A", definitionA,
                "B", definitionB,
                "C", definitionC,
                "D", definitionD
        );
        assertThat(TestDependencies.validateDependenciesAndReturnReasons(tests))
                .containsOnlyKeys("A", "B", "C", "D")
                .containsEntry("A", "A test A depends on an unknown or incompatible test __dummy")
                .containsEntry("B", "A test B directly or indirectly depends on an invalid test")
                .containsEntry("C", "A test C depends on a test with circular dependency")
                .containsEntry("D", "A test D directly or indirectly depends on an invalid test");
    }

    private static TestDefinition.Builder stubTestDefinition(final String testName) {
        return TestDefinition.builder()
                .setTestType(TestType.ANONYMOUS_USER)
                .setSalt("&" + testName)
                .setBuckets(ImmutableList.of(new TestBucket("active", BUCKET_VALUE, "")))
                .setAllocations(ImmutableList.of(new Allocation("",
                        ImmutableList.of(new Range(BUCKET_VALUE, 1.0)),
                        "#A1"
                )));
    }

    private static Map<String, ConsumableTestDefinition> constructTests(
            final List<String> testNames,
            final Map<String, String> parentMap
    ) {
        final Map<String, ConsumableTestDefinition> tests = new HashMap<>();
        for (final String testName : testNames) {
            final TestDefinition.Builder builder = stubTestDefinition(testName);
            final String parentTest = parentMap.get(testName);
            if (parentTest != null) {
                builder.setDependency(new TestDependency(parentTest, BUCKET_VALUE));
            }
            tests.put(testName, ConsumableTestDefinition.fromTestDefinition(builder.build()));
        }
        return ImmutableMap.copyOf(tests);
    }
}