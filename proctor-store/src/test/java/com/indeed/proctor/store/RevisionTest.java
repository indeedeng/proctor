package com.indeed.proctor.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.proctor.common.Serializers;
import org.junit.Test;

import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class RevisionTest {
    private static ObjectMapper mapper = Serializers.lenient();

    @Test
    public void testDeserialize() throws IOException {
        final String serializedRevision =
                "{"
                        + "\"revision\": \"123\","
                        + "\"author\": \"test-user\","
                        + "\"date\": \"2018-01-01T00:00:00.000+0000\","
                        + "\"message\": \"test\""
                        + "}";
        final Revision revision = mapper.readValue(serializedRevision, Revision.class);
        assertEquals("123", revision.getRevision());
        assertEquals("test-user", revision.getAuthor());
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(revision.getDate());
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        assertEquals(2018, calendar.get(Calendar.YEAR));
        assertEquals(0, calendar.get(Calendar.MONTH));
        assertEquals(1, calendar.get(Calendar.DAY_OF_MONTH));
        assertEquals("test", revision.getMessage());
    }
}
