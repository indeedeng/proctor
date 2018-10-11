package com.indeed.proctor.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.proctor.common.Serializers;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class RevisionTest {
    private static ObjectMapper mapper = Serializers.lenient();

    @Test
    public void testDeserialize() throws IOException {
        final String serializedRevision = "{" +
                "\"revision\": \"123\"," +
                "\"author\": \"test-user\"," +
                "\"date\": \"2018-01-01T00:00:00.000+0000\"," +
                "\"message\": \"test\"" +
                "}";
        final Revision revision = mapper.readValue(serializedRevision, Revision.class);
        assertEquals("123", revision.getRevision());
        assertEquals("test-user", revision.getAuthor());
        assertEquals(118, revision.getDate().getYear());
        assertEquals(0, revision.getDate().getMonth());
        assertEquals(1, revision.getDate().getDay());
        assertEquals("test", revision.getMessage());
    }
}