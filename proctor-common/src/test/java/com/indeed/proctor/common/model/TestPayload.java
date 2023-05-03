package com.indeed.proctor.common.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.PayloadType;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestPayload {

    /**
     * Verify that the fields for a Payload are effectively jointly static:
     * only one of them may be set once ever.  If a setter is called on
     * a Payload that already contains a value, the Payload will throw
     * an IllegalStateException.
     */
    @Test
    public void verifyPayloadSetters() {
        final Map<PayloadType, Payload> payloads = getPayloadTypePayloadSampleMap(0);
        final ObjectMapper objectMapper = new ObjectMapper();

        assertThatThrownBy(() ->
                payloads.get(PayloadType.DOUBLE_VALUE).setStringArray(new String[]{"foo", "bar", "baz"}))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() ->
                payloads.get(PayloadType.DOUBLE_ARRAY).setStringValue("foobar"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() ->
                payloads.get(PayloadType.LONG_VALUE).setLongArray(new Long[]{9L, 8L, 7L, 6L, 5L, 4L}))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() ->
                payloads.get(PayloadType.LONG_ARRAY).setLongValue(42L))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() ->
                payloads.get(PayloadType.STRING_VALUE).setDoubleArray(new Double[]{98.0D, -137D}))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() ->
                payloads.get(PayloadType.STRING_ARRAY).setDoubleValue(47.0D))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() ->
                payloads.get(PayloadType.MAP).setMap(Collections.<String, Object>emptyMap()))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() ->
                payloads.get(PayloadType.JSON).setJson(objectMapper.readTree("{}")))
                .isInstanceOf(IllegalStateException.class);
    }


    // Test
    private void verifyPayload(final Payload payload, final PayloadType type) {
        assertThat(payload.fetchPayloadType()).isEqualTo(Optional.ofNullable(type));
        assertThat(payload.fetchType()).isEqualTo(Optional.ofNullable(type).map(t -> t.payloadTypeName).orElse("none"));

        if (type == null) {
            // payloads[0] is empty so all null.
            assertEquals(0, payload.numFieldsDefined());
            assertNull(payload.fetchAValue());
        } else {
            // Only one field defined.
            assertEquals(1, payload.numFieldsDefined());
        }

        if (type == PayloadType.DOUBLE_VALUE) {
            assertNotNull(payload.getDoubleValue());
            assertSame(payload.fetchAValue(), payload.getDoubleValue());
        } else {
            assertNull(payload.getDoubleValue());
        }

        if (type == PayloadType.DOUBLE_ARRAY) {
            assertNotNull(payload.getDoubleArray());
            assertSame(payload.fetchAValue(), payload.getDoubleArray());
        } else {
            assertNull(payload.getDoubleArray());
        }

        if (type == PayloadType.LONG_VALUE) {
            assertNotNull(payload.getLongValue());
            assertSame(payload.fetchAValue(), payload.getLongValue());
        } else {
            assertNull(payload.getLongValue());
        }

        if (type == PayloadType.LONG_ARRAY) {
            assertNotNull(payload.getLongArray());
            assertSame(payload.fetchAValue(), payload.getLongArray());
        } else {
            assertNull(payload.getLongArray());
        }

        if (type == PayloadType.STRING_VALUE) {
            assertNotNull(payload.getStringValue());
            assertSame(payload.fetchAValue(), payload.getStringValue());
        } else {
            assertNull(payload.getStringValue());
        }

        if (type == PayloadType.STRING_ARRAY) {
            assertNotNull(payload.getStringArray());
            assertSame(payload.fetchAValue(), payload.getStringArray());
        } else {
            assertNull(payload.getStringArray());
        }

        if (type == PayloadType.MAP) {
            assertNotNull(payload.getMap());
            assertSame(payload.fetchAValue(), payload.getMap());
        } else {
            assertNull(payload.getMap());
        }

        if (type == PayloadType.JSON) {
            assertNotNull(payload.getJson());
            assertSame(payload.fetchAValue(), payload.getJson());
        } else {
            assertNull(payload.getJson());
        }

        if (type != null) {
            switch (type) {
                case DOUBLE_VALUE:
                    assertEquals(47D, payload.getDoubleValue(), 0.0); // should auto-unbox
                    break;
                case DOUBLE_ARRAY:
                    final Double[] da = payload.getDoubleArray();
                    assertNotNull(da);
                    assertEquals(2, da.length);
                    assertEquals(98D, da[0], 0.0);      // auto-unbox
                    assertEquals(-137D, da[1], 0.0);    // auto-unbox
                    break;
                case LONG_VALUE:
                    assertEquals(42L, (long) payload.getLongValue()); // should auto-unbox
                    break;
                case LONG_ARRAY:
                    final Long[] la = payload.getLongArray();
                    assertNotNull(la);
                    assertEquals(6, la.length);
                    assertEquals(9L, (long) la[0]);      // auto-unbox
                    assertEquals(8L, (long) la[1]);      // auto-unbox
                    assertEquals(7L, (long) la[2]);      // auto-unbox
                    assertEquals(6L, (long) la[3]);      // auto-unbox
                    assertEquals(5L, (long) la[4]);      // auto-unbox
                    assertEquals(4L, (long) la[5]);      // auto-unbox
                    break;
                case STRING_VALUE:
                    assertEquals("foobar0", payload.getStringValue());
                    break;
                case STRING_ARRAY:
                    final String[] sa = payload.getStringArray();
                    assertNotNull(sa);
                    assertEquals(3, sa.length);
                    assertEquals("foo", sa[0]);
                    assertEquals("bar", sa[1]);
                    assertEquals("baz0", sa[2]);
                    break;
                case MAP:
                    final Map<String, Object> ma = payload.getMap();
                    assertNotNull(ma);
                    assertEquals(0, Double.compare(1.0D, (Double) (ma.get("a"))));
                    assertArrayEquals(new Double[]{57.0D, -8.0D, 79.97D}, (Double[]) ma.get("b"));
                    assertEquals("somevals0", ma.get("c"));
                    break;
                case JSON:
                    final JsonNode json = payload.getJson();
                    assertNotNull(json);
                    assertEquals(
                        "{" +
                                    "\"foo\":{" +
                                            "\"bar\":\"baz\"," +
                                            "\"abc\":456" +
                                        "}" +
                                    "}", json.toString());
                    break;
                default:
                    fail();  // default case should never be reached.
            }
        }
    }

    /**
     * Given two vectors of test Payloads, confirms that:
     *  type(payloadsA[i]) == type(payloadsA[j]) iff i==j
     */
    private void verifyPayloadTypeComparisons(final Map<PayloadType, Payload> payloadsA, final Map<PayloadType, Payload> payloadsB) {
        assertThat(payloadsA.keySet()).isEqualTo(payloadsB.keySet());
        payloadsA.forEach((typeA, payloadA) ->
                payloadsB.forEach((typeB, payloadB) -> {
                            if (typeA == typeB) {
                                assertTrue("should be true: " + payloadA + "].sameType(payloadsB[" + payloadB + "])",
                                        payloadA.sameType(payloadB));
                            } else {
                                assertFalse("should be false: payloadsA[" + payloadA + "].sameType(payloadsB[" + payloadA + "])",
                                        payloadA.sameType(payloadB));
                            }
                        }
                ));
    }

    @Test
    public void testPayloadFields() {
        verifyPayload(Payload.EMPTY_PAYLOAD, null);
        getPayloadTypePayloadSampleMap(0)
                .forEach((type, payload) ->
                        verifyPayload(payload, type)
                );
    }

    @Test
    public void testPayloadTypeComparisons() {
        final Map<PayloadType, Payload> payloadsA = getPayloadTypePayloadSampleMap(0);
        final Map<PayloadType, Payload> payloadsB = getPayloadTypePayloadSampleMap(1);

        // Verify payloads are the same type as themselves.
        verifyPayloadTypeComparisons(payloadsA, payloadsA);

        // Verify same type as set the same way
        verifyPayloadTypeComparisons(payloadsA, payloadsB);
        // Verify symmetric
        verifyPayloadTypeComparisons(payloadsB, payloadsA);
    }

    @Test
    public void testOverwritePayload(){
        final Payload payload = new Payload("mapValue");

        //Payload value is "immutable": can't be set more than once.
        assertThatThrownBy(() -> payload.setDoubleValue(1.0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(Payload.PAYLOAD_OVERWRITE_EXCEPTION);
    }

    @Test
    public void testPayloadToString() {
        final Map<PayloadType, Payload> payloads = getPayloadTypePayloadSampleMap(0);

        assertThat(payloads.get(PayloadType.DOUBLE_VALUE).toString())
                .contains("doubleValue")
                .contains("47");

        assertThat(payloads.get(PayloadType.DOUBLE_ARRAY).toString())
                .contains("doubleArray")
                .contains("98")
                .contains("-137");

        assertThat(payloads.get(PayloadType.LONG_VALUE).toString())
                .contains("longValue")
                .contains("42");

        assertThat(payloads.get(PayloadType.LONG_ARRAY).toString())
                .contains("longArray")
                .contains("9,")
                .contains("8,")
                .contains("7,")
                .contains("6,")
                .contains("5,")
                .contains("4");

        assertThat(payloads.get(PayloadType.STRING_VALUE).toString())
                .contains("stringValue")
                .contains("foobar");

        assertThat(payloads.get(PayloadType.STRING_ARRAY).toString())
                .contains("stringArray")
                .contains("foo")
                .contains("bar")
                .contains("baz");

        assertThat(payloads.get(PayloadType.JSON).toString()).isEqualTo(
                "{ json : {" +
                                    "\"foo\":{" +
                                            "\"bar\":\"baz\"," +
                                            "\"abc\":456" +
                                        "}" +
                                    "} }" );
    }

    private static Map<PayloadType, Payload> getPayloadTypePayloadSampleMap(final int seed) {
        final ObjectMapper objectMapper = new ObjectMapper();
        final String jsonString =
                "{" +
                    "\"foo\" : { " +
                        "\"bar\" : \"baz\"," +
                        "\"abc\" : 456" +
                    "} " +
                "}";
        final ImmutableMap<PayloadType, Consumer<Payload>> map = ImmutableMap.<PayloadType, Consumer<Payload>>builder()
                .put(PayloadType.DOUBLE_VALUE, p -> p.setDoubleValue(47.0D + seed))
                .put(PayloadType.DOUBLE_ARRAY, p -> p.setDoubleArray(new Double[]{98.0D, -137D + seed}))
                .put(PayloadType.LONG_VALUE, p -> p.setLongValue(42L + seed))
                .put(PayloadType.LONG_ARRAY, p -> p.setLongArray(new Long[]{9L, 8L, 7L, 6L, 5L, 4L + seed}))
                .put(PayloadType.STRING_VALUE, p -> p.setStringValue("foobar" + seed))
                .put(PayloadType.STRING_ARRAY, p -> p.setStringArray(new String[]{"foo", "bar", "baz" + seed}))
                .put(PayloadType.MAP, p -> p.setMap(ImmutableMap.of("a", 1.0D, "b", new Double[]{57.0D, -8.0D, 79.97D}, "c", "somevals" + seed)))
                .put(PayloadType.JSON, p -> {
                    try {
                        p.setJson(objectMapper.readTree(jsonString));
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .build();
        return map.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> {
                    final Payload p = new Payload();
                    e.getValue().accept(p);
                    return p;
                }));
    }
}
