package com.indeed.proctor.common;

import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.util.core.DataLoadTimer;
import org.apache.el.lang.FunctionMapperImpl;
import org.easymock.classextension.EasyMock;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AbstractProctorLoaderTest {

    private final DataLoadTimer dataLoaderTimerMock = EasyMock.createMock(DataLoadTimer.class);
    private final TestProctorLoader loader = createTestProctorLoader(dataLoaderTimerMock);

    @Test
    public void testLoaderTimerFunctionsNoLoad() {
        EasyMock.expect(dataLoaderTimerMock.getSecondsSinceLastLoadCheck())
                .andReturn(null)
                .once();
        EasyMock.expect(dataLoaderTimerMock.isLoadedDataSuccessfullyRecently())
                .andReturn(false)
                .once();
        EasyMock.replay(dataLoaderTimerMock);

        assertNull(loader.secondsSinceLastReloadAttempt());
        assertFalse(loader.isLoadedDataSuccessfullyRecently());
    }

    @Test
    public void testLoaderTimerFunctionsLoadSuccess() {
        EasyMock.expect(dataLoaderTimerMock.isLoadedDataSuccessfullyRecently())
                .andReturn(true)
                .once();
        EasyMock.expect(dataLoaderTimerMock.getSecondsSinceLastLoadCheck())
                .andReturn(42)
                .once();
        EasyMock.replay(dataLoaderTimerMock);

        assertEquals(42, (int) loader.secondsSinceLastReloadAttempt());
        assertTrue(loader.isLoadedDataSuccessfullyRecently());
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