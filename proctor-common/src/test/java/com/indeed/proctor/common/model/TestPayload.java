package com.indeed.proctor.common.model;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author pwp
 */
public class TestPayload {
    // Set up
    private Payload[] getTestPayloads() {
        Payload[] payloads = new Payload[8];
        for (int i = 0; i < payloads.length; i++)
            payloads[i] = new Payload();
        // payload[0] is empty.
        payloads[1].setDoubleValue(47.0D);
        payloads[2].setDoubleArray(new Double[]{98.0D, -137D});
        payloads[3].setLongValue(42L);
        payloads[4].setLongArray(new Long[]{9L, 8L, 7L, 6L, 5L, 4L});
        payloads[5].setStringValue("foobar");
        payloads[6].setStringArray(new String[]{"foo", "bar", "baz"});
        payloads[7].setMap(ImmutableMap.of("a", new Double(1.0D), "b", new Double[]{57.0D, -8.0D, 79.97D}, "c", (Object) "somevals"));
        return payloads;
    }

    /**
     * Verify that the fields for a Payload are effectively jointly static:
     * only one of them may be set once ever.  If a setter is called on
     * a Payload that already contains a value, the Payload will throw
     * an IllegalStateException.
     */
    @Test
    public void verifyPayloadSetters() {
        final Payload[] payloads = getTestPayloads();

        try {
            payloads[1].setStringArray(new String[]{"foo", "bar", "baz"});
            assertFalse("should have thrown IllegalStateException on setStringArray()", true);
        } catch (IllegalStateException e) {
            // expected.
        }
        try {
            payloads[2].setStringValue("foobar");
            assertFalse("should have thrown IllegalStateException on setStringValue()", true);
        } catch (IllegalStateException e) {
            // expected.
        }
        try {
            payloads[3].setLongArray(new Long[]{9L, 8L, 7L, 6L, 5L, 4L});
            assertFalse("should have thrown IllegalStateException on setLongArray()", true);
        } catch (IllegalStateException e) {
            // expected.
        }
        try {
            payloads[4].setLongValue(42L);
            assertFalse("should have thrown IllegalStateException on setLongValue()", true);
        } catch (IllegalStateException e) {
            // expected.
        }
        try {
            payloads[5].setDoubleArray(new Double[]{98.0D, -137D});
            assertFalse("should have thrown IllegalStateException on setDoubleArray()", true);
        } catch (IllegalStateException e) {
            // expected.
        }
        try {
            payloads[6].setDoubleValue(47.0D);
            assertFalse("should have thrown IllegalStateException on setDoubleValue()", true);
        } catch (IllegalStateException e) {
            // expected.
        }
        try {
            payloads[7].setMap(Collections.<String, Object>emptyMap());
            assertFalse("should have thrown IllegalStateException on setMap()", true);
        } catch (IllegalStateException e) {
            // expected.
        }
    }

    private Payload[] getDifferentTestPayloads() {
        Payload[] payloads = new Payload[8];
        for (int i = 0; i < payloads.length; i++)
            payloads[i] = new Payload();
        // payload[0] is empty.
        payloads[1].setDoubleValue(12.0D);
        payloads[2].setDoubleArray(new Double[]{57.0D, -8.0D, 79.97D});
        payloads[3].setLongValue(17L);
        payloads[4].setLongArray(new Long[]{1L, 3L, 5L, 7L});
        payloads[5].setStringValue("xyzzy");
        payloads[6].setStringArray(new String[]{"quux", "toad"});
        payloads[7].setMap(ImmutableMap.of("a", new Double(2.0D), "b", new Double[]{37.0D, -4.0D, 97D}, "c", (Object) "somevals2"));
        return payloads;
    }

    // Test
    private void verifyPayload(Payload[] payloads, int k) {
        if (k == 0) {
            // payloads[0] is empty so all null.
            assertEquals(0, payloads[k].numFieldsDefined());
            assertTrue(payloads[k].fetchAValue() == null);
        } else {
            // Only one field defined.
            assertEquals(1, payloads[k].numFieldsDefined());
        }

        if (k == 1) {
            assertNotNull(payloads[k].getDoubleValue());
            assertTrue(payloads[k].fetchAValue() == payloads[k].getDoubleValue());
        } else {
            assertNull(payloads[k].getDoubleValue());
        }

        if (k == 2) {
            assertNotNull(payloads[k].getDoubleArray());
            assertTrue(payloads[k].fetchAValue() == payloads[k].getDoubleArray());
        } else {
            assertNull(payloads[k].getDoubleArray());
        }

        if (k == 3) {
            assertNotNull(payloads[k].getLongValue());
            assertTrue(payloads[k].fetchAValue() == payloads[k].getLongValue());
        } else {
            assertNull(payloads[k].getLongValue());
        }

        if (k == 4) {
            assertNotNull(payloads[k].getLongArray());
            assertTrue(payloads[k].fetchAValue() == payloads[k].getLongArray());
        } else {
            assertNull(payloads[k].getLongArray());
        }

        if (k == 5) {
            assertNotNull(payloads[k].getStringValue());
            assertTrue(payloads[k].fetchAValue() == payloads[k].getStringValue());
        } else {
            assertNull(payloads[k].getStringValue());
        }

        if (k == 6) {
            assertNotNull(payloads[k].getStringArray());
            assertTrue(payloads[k].fetchAValue() == payloads[k].getStringArray());
        } else {
            assertNull(payloads[k].getStringArray());
        }

        if (k == 7) {
            assertNotNull(payloads[k].getMap());
            assertTrue(payloads[k].fetchAValue() == payloads[k].getMap());
        } else {
            assertNull(payloads[k].getMap());
        }

        switch(k) {
          case 0:
              // Nothing to test: payloads[0] is all empty: completely tested
              // by code above.
            break;
          case 1:
            assertTrue(47D == payloads[k].getDoubleValue()); // should auto-unbox
            break;
          case 2:
            Double[] da = payloads[k].getDoubleArray();
            assertNotNull(da);
            assertEquals(2, da.length);
            assertTrue(98D == da[0]);      // auto-unbox
            assertTrue(-137D == da[1]);    // auto-unbox
            break;
          case 3:
            assertTrue(42L == payloads[k].getLongValue()); // should auto-unbox
            break;
          case 4:
            Long[] la = payloads[k].getLongArray();
            assertNotNull(la);
            assertEquals(6, la.length);
            assertTrue(9L == la[0]);      // auto-unbox
            assertTrue(8L == la[1]);      // auto-unbox
            assertTrue(7L == la[2]);      // auto-unbox
            assertTrue(6L == la[3]);      // auto-unbox
            assertTrue(5L == la[4]);      // auto-unbox
            assertTrue(4L == la[5]);      // auto-unbox
            break;
          case 5:
            assertTrue("foobar".equals(payloads[k].getStringValue()));
            break;
          case 6:
            String[] sa = payloads[k].getStringArray();
            assertNotNull(sa);
            assertEquals(3, sa.length);
            assertTrue("foo".equals(sa[0]));
            assertTrue("bar".equals(sa[1]));
            assertTrue("baz".equals(sa[2]));
            break;
          case 7:
            final Map<String,Object> ma = payloads[k].getMap();
            assertNotNull(ma);
            assertTrue(Double.compare(1.0D, (Double)(ma.get("a")))==0);
            assertTrue(Arrays.equals(new Double[]{57.0D, -8.0D, 79.97D}, (Double[]) ma.get("b")));
            assertTrue("somevals".equals(ma.get("c")));
            break;
          default:
            assertTrue(false);  // default case should never be reached.
        }
    }

    /**
     * Given two vectors of test Payloads, confirms that:
     *  type(payloadsA[i]) == type(payloadsA[j]) iff i==j
     */
    private void verifyPayloadTypeComparisons(Payload[] payloadsA, Payload[] payloadsB) {
        for (int i = 0; i < payloadsA.length; i++) {
            for (int j = 0; j < payloadsB.length; j++) {
                if (i == j) {
                    assertTrue("should be true: payloadsA["+i+"].sameType(payloadsB["+j+"])", payloadsA[i].sameType(payloadsB[j]));
                } else {
                    assertFalse("should be false: payloadsA["+i+"].sameType(payloadsB["+j+"])", payloadsA[i].sameType(payloadsB[j]));
                }
            }
        }
    }

    @Test
    public void testPayloadFields() {
        final Payload[] payloads = getTestPayloads();

        for (int i = 0; i < payloads.length; i++) {
            verifyPayload(payloads, i);
        }
    }

    @Test
    public void testPayloadTypeComparisons() {
        final Payload[] payloadsA = getTestPayloads();
        final Payload[] payloadsB = getDifferentTestPayloads();

        // Verify payloads are the same type as themselves.
        verifyPayloadTypeComparisons(payloadsA, payloadsA);

        // Verify same type as set the same way
        verifyPayloadTypeComparisons(payloadsA, payloadsB);
        // Verify symmetric
        verifyPayloadTypeComparisons(payloadsB, payloadsA);
    }

    @Test
    public void testSetMapWithNullValue(){
        Payload payload = new Payload();

        final Map<String, Object> mapWithNullValues = new HashMap<String, Object>() {{
            put("validVal1", 0);
            put("validVal2", "");
            put("invalidVal", null);
        }};

        assertThatThrownBy(() -> payload.setMap(mapWithNullValues))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(Payload.PAYLOAD_NULL_VALUE_EXCEPTION);
    }

    @Test
    public void testOverwritePayload(){
        Payload payload = new Payload();

        payload.setStringValue("mapValue");

        //Payload value is "immutable": can't be set more than once.
        assertThatThrownBy(() -> payload.setDoubleValue(1.0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(Payload.PAYLOAD_OVERWRITE_EXCEPTION);
    }

    @Test
    public void testPayloadToString() {
        final Payload[] payloads = getTestPayloads();

        assertTrue(payloads[1].toString().contains("doubleValue"));
        assertTrue(payloads[1].toString().contains("47"));

        assertTrue(payloads[2].toString().contains("doubleArray"));
        assertTrue(payloads[2].toString().contains("98"));
        assertTrue(payloads[2].toString().contains("-137"));

        assertTrue(payloads[3].toString().contains("longValue"));
        assertTrue(payloads[3].toString().contains("42"));

        assertTrue(payloads[4].toString().contains("longArray"));
        assertTrue(payloads[4].toString().contains("9,"));
        assertTrue(payloads[4].toString().contains("8,"));
        assertTrue(payloads[4].toString().contains("7,"));
        assertTrue(payloads[4].toString().contains("6,"));
        assertTrue(payloads[4].toString().contains("5,"));
        assertTrue(payloads[4].toString().contains("4"));

        assertTrue(payloads[5].toString().contains("stringValue"));
        assertTrue(payloads[5].toString().contains("foobar"));

        assertTrue(payloads[6].toString().contains("stringArray"));
        assertTrue(payloads[6].toString().contains("foo"));
        assertTrue(payloads[6].toString().contains("bar"));
        assertTrue(payloads[6].toString().contains("baz"));
    }
}
