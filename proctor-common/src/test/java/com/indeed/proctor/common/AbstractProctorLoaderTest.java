package com.indeed.proctor.common;

import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.util.core.DataLoadTimer;
import org.apache.el.lang.FunctionMapperImpl;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
            super(TestProctorLoader.class, new ProctorSpecification(), new FunctionMapperImpl());
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
