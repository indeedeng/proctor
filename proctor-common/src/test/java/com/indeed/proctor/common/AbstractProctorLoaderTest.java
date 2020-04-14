package com.indeed.proctor.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.util.core.DataLoadTimer;
import org.apache.el.lang.FunctionMapperImpl;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.expect;
import static org.easymock.classextension.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AbstractProctorLoaderTest {

    private DataLoadTimer dataLoaderTimerMock;

    @Before
    public void setUp() {
        dataLoaderTimerMock = createMock(DataLoadTimer.class);

    }

    @Test
    public void testLoaderTimerFunctionsNoLoad() {
        expect(dataLoaderTimerMock.getSecondsSinceLastLoadCheck())
                .andReturn(null)
                .once();
        expect(dataLoaderTimerMock.isLoadedDataSuccessfullyRecently())
                .andReturn(false)
                .once();
        replay(dataLoaderTimerMock);

        final TestProctorLoader loader = createTestProctorLoader(dataLoaderTimerMock);
        assertNull(loader.getSecondsSinceLastLoadCheck());
        assertFalse(loader.isLoadedDataSuccessfullyRecently());
    }

    @Test
    public void testLoaderTimerFunctionsLoadSuccess() {
        expect(dataLoaderTimerMock.isLoadedDataSuccessfullyRecently())
                .andReturn(true)
                .once();
        expect(dataLoaderTimerMock.getSecondsSinceLastLoadCheck())
                .andReturn(42)
                .once();
        replay(dataLoaderTimerMock);

        final TestProctorLoader loader = createTestProctorLoader(dataLoaderTimerMock);
        assertEquals(42, (int) loader.getSecondsSinceLastLoadCheck());
        assertTrue(loader.isLoadedDataSuccessfullyRecently());
    }

    @Test
    public void testLoaderLoadWithChange() {
        final Proctor proctorMock = createMock(Proctor.class);
        final Audit audit = getAuditMockForLoad();
        final TestProctorLoader loader = new TestProctorLoader(dataLoaderTimerMock) {
            @Nullable
            @Override
            public Proctor doLoad() {
                setLastAudit(audit);
                return proctorMock;
            }
        };
        assertTrue(loader.load());
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
        assertTrue(loader.load());
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
            assertEquals(exceptionStub, rte.getCause());
        }
    }

    static class Sample {
        final int value;
        final List<Integer> array;

        @JsonCreator
        public Sample(
                @JsonProperty("value") final int value,
                @JsonProperty("array") final List<Integer> array
        ) {
            this.value = value;
            this.array = array;
        }

        public int getValue() {
            return value;
        }

        public List<Integer> getArray() {
            return array;
        }

        @Override
        public String toString() {
            return "Sample{" +
                    "value=" + value +
                    ", array=" + array +
                    '}';
        }
    }

    @Test
    public void testParsing() throws IOException {
        final String json = "{\"A\": { \"value\": 1, \"array\": [1, 2, 3] }, \"B\": { \"value\": 3, \"array\": [3, 4, 5]}}";

        final JsonFactory factory = new JsonFactory();
        final JsonParser parser = factory.createParser(json);
        final ObjectMapper objectMapper = new ObjectMapper();

        System.out.print(parser.currentToken());
        if (parser.nextToken() != JsonToken.START_OBJECT) {
            throw new IllegalStateException("hogehoge");
        }

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (parser.currentToken() != JsonToken.FIELD_NAME) {
                throw new IllegalStateException("gehogho");
            }

            final String key = parser.currentName();
            parser.nextToken();
            System.out.print(parser.currentToken());
            if (key.equals("B")) {
                parser.skipChildren();
                continue;
            }
            final Sample sample = objectMapper.readValue(parser, Sample.class);

            System.out.print(key);
            System.out.print(sample);
        }
    }

    private static Audit getAuditMockForLoad() {
        final Audit audit = createMock(Audit.class);
        expect(audit.getUpdated()).andReturn(1234L).times(2);
        expect(audit.getUpdatedBy()).andReturn("testuser").times(2);
        expect(audit.getVersion()).andReturn("v1").times(2);
        expect(audit.getUpdatedDate()).andReturn("9:30");
        replay(audit);
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
