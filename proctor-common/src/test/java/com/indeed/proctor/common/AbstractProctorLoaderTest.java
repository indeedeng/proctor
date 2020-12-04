package com.indeed.proctor.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.dynamic.DynamicFilters;
import com.indeed.proctor.common.dynamic.MetaTagsFilter;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.proctor.common.model.TestType;
import com.indeed.util.core.DataLoadTimer;
import org.apache.el.lang.FunctionMapperImpl;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractProctorLoaderTest {

    private DataLoadTimer dataLoaderTimerMock;

    @Before
    public void setUp() {
        dataLoaderTimerMock = mock(DataLoadTimer.class);
    }

    @Test
    public void testLoaderTimerFunctionsNoLoad() {
        when(dataLoaderTimerMock.getSecondsSinceLastLoadCheck())
                .thenReturn(null);
        when(dataLoaderTimerMock.isLoadedDataSuccessfullyRecently())
                .thenReturn(false);

        final TestProctorLoader loader = createTestProctorLoader(dataLoaderTimerMock);
        assertThat(loader.getSecondsSinceLastLoadCheck()).isNull();
        assertThat(loader.isLoadedDataSuccessfullyRecently()).isFalse();
    }

    @Test
    public void testLoaderTimerFunctionsLoadSuccess() {
        when(dataLoaderTimerMock.isLoadedDataSuccessfullyRecently())
                .thenReturn(true);
        when(dataLoaderTimerMock.getSecondsSinceLastLoadCheck())
                .thenReturn(42);

        final TestProctorLoader loader = createTestProctorLoader(dataLoaderTimerMock);
        assertThat(loader.getSecondsSinceLastLoadCheck()).isEqualTo(42);
        assertThat(loader.isLoadedDataSuccessfullyRecently()).isTrue();
    }

    @Test
    public void testLoaderLoadWithChange() {
        final Proctor proctorMock = mock(Proctor.class);
        final Audit audit = getAuditMockForLoad();
        final TestProctorLoader loader = new TestProctorLoader(dataLoaderTimerMock) {
            @Nullable
            @Override
            public Proctor doLoad() {
                setLastAudit(audit);
                return proctorMock;
            }
        };
        assertThat(loader.load()).isTrue();
        assertThat(loader.get()).isEqualTo(proctorMock);
    }

    @Test
    public void testLoaderLoadNoChange() {
        final Audit audit = getAuditMockForLoad();
        final TestProctorLoader loader = new TestProctorLoader(dataLoaderTimerMock) {
            @Nullable
            @Override
            public Proctor doLoad() {
                setLastAudit(audit);
                return null;
            }
        };
        assertThat(loader.load()).isTrue();
    }

    @Test
    public void testLoaderLoadException() {
        final RuntimeException exceptionStub = new RuntimeException("test exception");
        final TestProctorLoader loader = new TestProctorLoader(dataLoaderTimerMock) {
            @Nullable
            @Override
            public Proctor doLoad() {

                throw exceptionStub;
            }
        };
        try {
            loader.load();
            fail("Expected RTE");
        } catch (final RuntimeException rte) {
            assertThat(rte.getCause()).isEqualTo(exceptionStub);
        }
    }

    @Test
    public void testDoLoad() throws IOException {
        // prepare
        final TestMatrixArtifact matrix = new TestMatrixArtifact();
        final Audit audit = new Audit();
        matrix.setAudit(audit);
        final String requiredTestname = "required";
        final String dynamicAddedTestname = "dynamic";
        final String tagname = "fooTag";
        final ConsumableTestDefinition dynamicIncludedDefinition = createStubDefinition();
        dynamicIncludedDefinition.setMetaTags(ImmutableList.of(tagname));
        matrix.setTests(ImmutableMap.<String, ConsumableTestDefinition>builder()
                .put(requiredTestname, createStubDefinition())
                .put(dynamicAddedTestname, dynamicIncludedDefinition)
                .build());
        final TestSpecification specification = new TestSpecification();
        final Map<String, TestSpecification> tests = ImmutableMap.<String, TestSpecification>builder()
                .put(requiredTestname, specification)
                .build();
        final DynamicFilters filters = new DynamicFilters(ImmutableList.of(new MetaTagsFilter(singleton(tagname))));
        final ProctorSpecification proctorSpecification = new ProctorSpecification(Collections.emptyMap(), tests, filters);
        final TestProctorLoader loader = new TestProctorLoader(dataLoaderTimerMock, proctorSpecification) {
            @Nullable
            @Override
            TestMatrixArtifact loadTestMatrix() {
                return matrix;
            }
        };

        // execute
        final Proctor proctor = loader.doLoad();

        // verify
        final ProctorResult proctorResult = proctor.determineTestGroups(
                Identifiers.of(TestType.ANONYMOUS_USER, "foo"),
                Collections.emptyMap(),
                Collections.emptyMap());
        assertThat(proctor.getLoadResult().getTestsWithErrors()).isEmpty();
        assertThat(proctor.getLoadResult().getMissingTests()).isEmpty();
        assertThat(proctor.getLoadResult().getDynamicTestErrorMap()).isEmpty();
        assertThat(proctorResult.getTestDefinitions()).containsOnlyKeys(requiredTestname, dynamicAddedTestname);
        assertThat(proctorResult.getDynamicallyLoadedTests()).containsExactly(dynamicAddedTestname);
    }

    private ConsumableTestDefinition createStubDefinition() {
        final ConsumableTestDefinition consumableTestDefinition = new ConsumableTestDefinition();
        consumableTestDefinition.setTestType(TestType.ANONYMOUS_USER);
        consumableTestDefinition.setBuckets(ImmutableList.of(new TestBucket("inactive", -1, "")));
        consumableTestDefinition.setAllocations(ImmutableList.of(new Allocation("", ImmutableList.of(new Range(-1, 1.0)))));
        return consumableTestDefinition;
    }

    private static Audit getAuditMockForLoad() {
        final Audit audit = mock(Audit.class);
        when(audit.getUpdated()).thenReturn(1234L);
        when(audit.getUpdatedBy()).thenReturn("testuser");
        when(audit.getVersion()).thenReturn("v1");
        when(audit.getUpdatedDate()).thenReturn("9:30");
        return audit;
    }

    private static TestProctorLoader createTestProctorLoader(final DataLoadTimer dataLoaderTimer) {
        return new TestProctorLoader(dataLoaderTimer);
    }

    private static class TestProctorLoader extends AbstractProctorLoader {

        public TestProctorLoader(final DataLoadTimer dataLoaderTimer) {
            this(dataLoaderTimer, new ProctorSpecification());
        }

        public TestProctorLoader(final DataLoadTimer dataLoaderTimer, final ProctorSpecification specification) {
            super(TestProctorLoader.class, specification, new FunctionMapperImpl());
            this.dataLoadTimer = dataLoaderTimer;
        }

        @Nullable
        @Override
        TestMatrixArtifact loadTestMatrix() {
            return null;
        }

        @Nonnull
        @Override
        String getSource() {
            return "dummy";
        }
    }
}
