package com.indeed.proctor.common;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.PayloadType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author pwp
 */
public class TestPayloadType {
    @Test
    public void testPayloadTypeNames() {
        List<String> names = PayloadType.allTypeNames();
        assertEquals(7, names.size());

        for (String s : new String[]{"doubleValue", "doubleArray",
                                     "longValue", "longArray",
                                     "stringValue", "stringArray","map"}) {
            PayloadType p = PayloadType.payloadTypeForName(s);
            assertEquals(s, p.payloadTypeName);
        }
        // Verify that trying to get the PayloadType for a nonsense
        // name throws an exception.
        try {
            PayloadType p = PayloadType.payloadTypeForName("gruntleBuggy");
            assertFalse("should have thrown IllegalArgumentException on \"gruntleBuggy\"", true);
        } catch (IllegalArgumentException e) {
            // expected.
        }
    }

    private Payload[] getTestPayloads() {
        Payload[] payloads = new Payload[7];
        for (int i = 0; i < payloads.length; i++)
            payloads[i] = new Payload();
        payloads[0].setDoubleValue(47.0D);
        payloads[1].setDoubleArray(new Double[]{98.0D, -137D});
        payloads[2].setLongValue(42L);
        payloads[3].setLongArray(new Long[]{9L, 8L, 7L, 6L, 5L, 4L});
        payloads[4].setStringValue("foobar");
        payloads[5].setStringArray(new String[]{"foo", "bar", "baz"});
        payloads[6].setMap(ImmutableMap.of("a", new Double(1.0D), "b", new Double[]{57.0D, -8.0D, 79.97D}, "c", (Object) "somevals"));
        return payloads;
    }

    // Must be in the same order as getTestPayloads().
    private PayloadType[] allPayloadTypes() {
        return new PayloadType[]{PayloadType.DOUBLE_VALUE, PayloadType.DOUBLE_ARRAY,
                                  PayloadType.LONG_VALUE, PayloadType.LONG_ARRAY,
                                  PayloadType.STRING_VALUE, PayloadType.STRING_ARRAY,
                                  PayloadType.MAP};
    }

    @Test
    public void testPayloadTypeForValueRetrieval_DoubleArrayType() {
        assertTrue(PayloadType.DOUBLE_ARRAY.equals(PayloadType.payloadTypeForValue(new ArrayList<Double>(){{add(1.1D);}})));
        assertFalse(PayloadType.DOUBLE_ARRAY.equals(PayloadType.payloadTypeForValue(new ArrayList<Long>(){{add(100L);}})));
        assertTrue(PayloadType.DOUBLE_ARRAY.equals(PayloadType.payloadTypeForValue(new ArrayList<Float>(){{add(1.2F);}})));
        assertTrue(PayloadType.DOUBLE_ARRAY.equals(PayloadType.payloadTypeForValue(new Double[]{1.0D, 2.0D})));
        assertTrue(PayloadType.DOUBLE_ARRAY.equals(PayloadType.payloadTypeForValue(new Float[]{1.0F, 2.0F})));
        assertFalse(PayloadType.DOUBLE_ARRAY.equals(PayloadType.payloadTypeForValue(new Long[]{100L, 200L})));
    }

    @Test
    public void testPayloadTypeForValueRetrieval_LongArrayType() {
        assertTrue(PayloadType.LONG_ARRAY.equals(PayloadType.payloadTypeForValue(new ArrayList<Long>(){{add(11L);}})));
        assertTrue(PayloadType.LONG_ARRAY.equals(PayloadType.payloadTypeForValue(new ArrayList<Integer>(){{add(11);}})));
        assertFalse(PayloadType.LONG_ARRAY.equals(PayloadType.payloadTypeForValue(new ArrayList<Double>(){{add(1.1D);}})));
        assertTrue(PayloadType.LONG_ARRAY.equals(PayloadType.payloadTypeForValue(new Long[]{100L, 200L})));
        assertTrue(PayloadType.LONG_ARRAY.equals(PayloadType.payloadTypeForValue(new Integer[]{10, 20})));
        assertFalse(PayloadType.LONG_ARRAY.equals(PayloadType.payloadTypeForValue(new Double[]{1.0D, 2.0D})));
    }

    @Test
    public void testPayloadTypeForValueRetrieval_StringArrayType() {
        assertTrue(PayloadType.STRING_ARRAY.equals(PayloadType.payloadTypeForValue(new ArrayList<String>(){{add("Ya");}})));
        assertFalse(PayloadType.STRING_ARRAY.equals(PayloadType.payloadTypeForValue(new ArrayList<Integer>(){{add(100);}})));
        assertTrue(PayloadType.STRING_ARRAY.equals(PayloadType.payloadTypeForValue(new String[]{"yea", "Ya"})));
        assertFalse(PayloadType.STRING_ARRAY.equals(PayloadType.payloadTypeForValue(new Float[]{1.0F, 2.0F})));
    }

    @Test
    public void testPayloadTypeForValueRetrieval_NonArrayTypes() {
        assertTrue(PayloadType.MAP.equals(PayloadType.payloadTypeForValue(ImmutableMap.of("string","string"))));
        assertTrue(PayloadType.LONG_VALUE.equals(PayloadType.payloadTypeForValue(100)));
        assertTrue(PayloadType.LONG_VALUE.equals(PayloadType.payloadTypeForValue(100L)));
        assertTrue(PayloadType.DOUBLE_VALUE.equals(PayloadType.payloadTypeForValue(1.1D)));
        assertTrue(PayloadType.DOUBLE_VALUE.equals(PayloadType.payloadTypeForValue(2.1F)));
        assertTrue(PayloadType.STRING_VALUE.equals(PayloadType.payloadTypeForValue("yes")));
    }

    @Test
    public void testUnknownPayloadTypeForValue() {
        try {
            PayloadType.payloadTypeForValue(new ArrayList());
        } catch(IllegalArgumentException e) {
            //expected
        }
        try {
            PayloadType.payloadTypeForValue(PayloadType.DOUBLE_VALUE);
        } catch(IllegalArgumentException e) {
            //expected
        }
    }

    @Test
    public void testPayloadHasType() {
        final Payload[] payloads = getTestPayloads();
        final PayloadType[] payloadTypes = allPayloadTypes();
        for (int i = 0; i < payloads.length; i++) {
            for (int j = 0; j < payloadTypes.length; j++) {
                if (i == j) {
                    assertTrue("should be true: payloadTypes["+j+"].payloadHasThisType(payloads["+i+"])", payloadTypes[j].payloadHasThisType(payloads[i]));
                    assertTrue("should be true: payloads[i].fetchType().equals(payloadTypes[j].payloadTypeName",
                            payloads[i].fetchType().equals(payloadTypes[j].payloadTypeName));
                } else {
                    assertFalse("should be false: payloadTypes["+j+"].payloadHasThisType(payloads["+i+"])", payloadTypes[j].payloadHasThisType(payloads[i]));
                }
            }
        }
    }
}
