package com.indeed.proctor.webapp.jobs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.indeed.proctor.common.IncompatibleTestMatrixException;
import com.indeed.proctor.common.ProctorLoadResult;
import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.common.TestSpecification;
import com.indeed.proctor.common.dynamic.DynamicFilters;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.webapp.ProctorSpecificationSource;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.model.AppVersion;
import com.indeed.proctor.webapp.model.ProctorSpecifications;
import com.indeed.proctor.webapp.model.RemoteSpecificationResult;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MatrixCheckerTest {
    @Test
    public void testCheckMatrix() {
        final Environment environment = Environment.PRODUCTION;
        final String targetTestName = "sample_tst";
        final String unrelatedTestName = "another_test";
        final AppVersion app = new AppVersion("app", "v1");
        final int expectedBucketValue = 0;
        final int unknownBucketValue = 1;
        final ProctorSpecification targetSpec =
                stubSpecification(
                        targetTestName,
                        stubTestSpecification(expectedBucketValue)
                );
        final ProctorSpecification unrelatedSpec =
                stubSpecification(
                        unrelatedTestName,
                        stubTestSpecification(expectedBucketValue)
                );

        final ProctorSpecificationSource source =
                mock(ProctorSpecificationSource.class);
        when(source.activeClients(eq(environment), eq(targetTestName)))
                .thenReturn(Collections.singleton(app));

        // adding same unrelated spec many times
        // to reproduce possible bug that checks only one of them.
        final RemoteSpecificationResult result = stubResult(
                unrelatedSpec, targetSpec, unrelatedSpec, unrelatedSpec, unrelatedSpec, unrelatedSpec
        );
        when(source.getRemoteResult(eq(environment), eq(app)))
                .thenReturn(result);

        final MatrixChecker checker = new MatrixChecker(source);
        assertThat(
                checker.checkMatrix(
                        environment,
                        targetTestName,
                        stubDefinition(unknownBucketValue)
                ).getErrors()
        ).containsExactly(
                "app@v1 cannot load test 'sample_tst':"
                        + " Allocation range in sample_tst from app@v1 refers to"
                        + " unknown bucket value(s) [1] with length > 0"
        );

        assertThat(
                checker.checkMatrix(
                        environment,
                        targetTestName,
                        stubDefinition(expectedBucketValue)
                ).getErrors()
        ).isEmpty();

        assertThat(
                checker.checkMatrix(
                        environment,
                        targetTestName,
                        null
                ).getErrors()
        ).containsExactly(
                "app@v1 requires test 'sample_tst'"
        );
    }

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

    private static RemoteSpecificationResult stubResult(
            final ProctorSpecification... specifications
    ) {
        final RemoteSpecificationResult result = mock(RemoteSpecificationResult.class);
        when(result.getSpecifications()).thenReturn(
                new ProctorSpecifications(Arrays.asList(specifications))
        );
        return result;
    }

    private static ProctorSpecification stubSpecification(
            final String testName,
            final TestSpecification testSpecification
    ) {
        return new ProctorSpecification(
                Collections.emptyMap(),
                ImmutableMap.of(testName, testSpecification),
                new DynamicFilters()
        );
    }

    private static TestSpecification stubTestSpecification(
            final int... bucketValues
    ) {
        final Map<String, Integer> buckets = new HashMap<>();
        for (final int value : bucketValues) {
            buckets.put("grp" + value, value);
        }
        final TestSpecification specification = new TestSpecification();
        specification.setBuckets(buckets);
        return specification;
    }

    private static TestDefinition stubDefinition(
            final int bucketValue
    ) {
        final TestDefinition testDefinition = new TestDefinition();
        testDefinition.setTestType(TestType.USER);
        final Allocation allocation = new Allocation();
        final List<TestBucket> buckets = ImmutableList.of(
                new TestBucket("grp" + bucketValue, bucketValue, "grp")
        );
        final List<Range> ranges = ImmutableList.of(
                new Range(bucketValue, 1.0)
        );
        allocation.setRanges(ImmutableList.copyOf(ranges));
        testDefinition.setBuckets(buckets);
        testDefinition.setAllocations(ImmutableList.of(allocation));
        return testDefinition;
    }
}
