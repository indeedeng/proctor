package com.indeed.proctor.common;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.indeed.proctor.common.SpecificationGenerator.generatePayloadSpecification;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SpecificationGeneratorTest {

    private final SpecificationGenerator generator = new SpecificationGenerator();

    @Test
    public void testGenerateSpecificationFromEmptyDefinition() {
        final String description = "this is an empty test with no buckets";
        final TestDefinition empty = stubTestDefinition(description, emptyList());
        empty.setDescription(description);
        final TestSpecification specification = generator.generateSpecification(empty);
        assertEquals(description, specification.getDescription());
        assertEquals(0, specification.getBuckets().size());
        assertEquals(-1, specification.getFallbackValue());
        assertNull(specification.getPayload());
    }

    @Test
    public void testGenerateSpecificationWithBuckets() {
        final String description = "this test has 3 buckets";
        final TestBucket control = new TestBucket("control", 0, "control bucket");
        final TestBucket inactiveBucket = new TestBucket("inactive", -3, "status quo");
        final TestBucket test = new TestBucket("test", 1, "test bucket");
        final TestDefinition empty = stubTestDefinition(description, Arrays.asList(control, inactiveBucket, test));
        final TestSpecification specification = generator.generateSpecification(empty);
        assertEquals(description, specification.getDescription());
        assertEquals(3, specification.getBuckets().size());
        assertEquals(inactiveBucket.getValue(), specification.getFallbackValue());
        assertNull(specification.getPayload());
        final Map<String, Integer> buckets = specification.getBuckets();
        assertEquals(inactiveBucket.getValue(), (int) buckets.get(inactiveBucket.getName()));
        assertEquals(control.getValue(), (int) buckets.get(control.getName()));
        assertEquals(test.getValue(), (int) buckets.get(test.getName()));
        // buckets should be ordered by value ascending
        final List<Integer> values = new ArrayList<>(buckets.values());
        assertEquals(inactiveBucket.getValue(), values.get(0).intValue());
        assertEquals(control.getValue(), values.get(1).intValue());
        assertEquals(test.getValue(), values.get(2).intValue());
    }

    @Test
    public void testGenerateSpecificationDoubleArrayPayload() {
        final String description = "this test has a payload buckets";
        final TestBucket inactiveBucket = new TestBucket("inactive", 0, "status quo", new Payload(new Double[]{1.4d, 4.5d}));
        final TestBucket control = new TestBucket("control", 0, "control bucket", new Payload(new Double[]{0.0, 2.4d}));
        final TestBucket test = new TestBucket("test", 1, "test bucket", new Payload(new Double[]{22.22, 33.33}));

        final TestDefinition empty = stubTestDefinition(description, Arrays.asList(inactiveBucket, control, test));
        final TestSpecification specification = generator.generateSpecification(empty);
        assertEquals(description, specification.getDescription());
        assertEquals(3, specification.getBuckets().size());
        assertEquals(inactiveBucket.getValue(), specification.getFallbackValue());
        final PayloadSpecification payload = specification.getPayload();
        assertNotNull(payload);
        assertEquals(PayloadType.DOUBLE_ARRAY.payloadTypeName, payload.getType());
        assertNull(payload.getSchema());
        assertNull(payload.getValidator());
        final Map<String, Integer> buckets = specification.getBuckets();
        assertEquals(inactiveBucket.getValue(), (int) buckets.get(inactiveBucket.getName()));
        assertEquals(control.getValue(), (int) buckets.get(control.getName()));
        assertEquals(test.getValue(), (int) buckets.get(test.getName()));
    }

    @Test
    public void testGenerateSpecificationPayloadMapSchema() {
        final String description = "this test has a payload buckets";
        final Payload inactivePayload = new Payload(ImmutableMap.of(
                "da", new Double[]{1.4d, 4.5d},
                "lv", 5L,
                "sa", new String[]{"foo", "bar"}
        ));
        final TestBucket bucket = new TestBucket("inactive", -3, "status quo", inactivePayload);
        final TestDefinition empty = stubTestDefinition(description, singletonList(bucket));
        final TestSpecification specification = generator.generateSpecification(empty);
        assertEquals(description, specification.getDescription());
        assertEquals(1, specification.getBuckets().size());
        assertEquals(bucket.getValue(), specification.getFallbackValue());
        final PayloadSpecification payload = specification.getPayload();
        assertNotNull(payload);
        assertEquals(PayloadType.MAP.payloadTypeName, payload.getType());
        final Map<String, String> schema = payload.getSchema();
        assertNotNull(schema);
        assertEquals(3, schema.size());
        assertEquals(PayloadType.DOUBLE_ARRAY.payloadTypeName, schema.get("da"));
        assertEquals(PayloadType.STRING_ARRAY.payloadTypeName, schema.get("sa"));
        assertEquals(PayloadType.LONG_VALUE.payloadTypeName, schema.get("lv"));
        assertNull(payload.getValidator());

        final Map<String, Integer> buckets = specification.getBuckets();
        assertEquals(bucket.getValue(), (int) buckets.get(bucket.getName()));
    }

    @Test
    public void testGeneratePayloadSpecification() {
        assertThat(generatePayloadSpecification(emptyList())).isNotPresent();

        assertThat(generatePayloadSpecification(singletonList(new Payload()))).isNotPresent();

        assertThat(generatePayloadSpecification(singletonList(new Payload(12L))).get().getType())
                .isEqualTo(PayloadType.LONG_VALUE.payloadTypeName);

        PayloadSpecification payloadSpecification = generatePayloadSpecification(singletonList(new Payload(emptyMap()))).get();
        assertThat(payloadSpecification.getType()).isEqualTo(PayloadType.MAP.payloadTypeName);
        assertThat(payloadSpecification.getSchema()).isNull();

        final Map<String, Object> nullValueMap = new HashMap<>();
        nullValueMap.put("foo", null);
        assertThatThrownBy(() -> generatePayloadSpecification(singletonList(new Payload(nullValueMap))))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot infer payload type");


        payloadSpecification = generatePayloadSpecification(singletonList(
                new Payload(ImmutableMap.<String, Object>builder()
                        .put("doubleKey", 42D)
                        .put("longKey", 33L)
                        .put("otherKey", "foo")
                        .build())
        )).get();
        assertThat(payloadSpecification.getType()).isEqualTo(PayloadType.MAP.payloadTypeName);
        assertThat(payloadSpecification.getSchema())
                .containsEntry("doubleKey", PayloadType.DOUBLE_VALUE.payloadTypeName)
                .containsEntry("longKey", PayloadType.LONG_VALUE.payloadTypeName)
                .containsEntry("otherKey", PayloadType.STRING_VALUE.payloadTypeName)
                .hasSize(3);

        // check conflicting types causes exception
        assertThatThrownBy(() -> generatePayloadSpecification(Arrays.asList(
                new Payload("fooString"),
                new Payload(42L)
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payloads not compatible");

        // check empty list causes exception
        assertThatThrownBy(() -> generatePayloadSpecification(singletonList(
                new Payload(ImmutableMap.<String, Object>builder()
                        .put("someKey", emptyList())
                        .build())
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot infer map schema")
                .hasMessageContaining("someKey");

        // check conflicting map types causes exception
        assertThatThrownBy(() -> generatePayloadSpecification(Arrays.asList(
                new Payload(ImmutableMap.<String, Object>builder()
                        .put("longKey", singletonList(42L))
                        .build()),
                new Payload(ImmutableMap.<String, Object>builder()
                        .put("longKey", singletonList(42D)) // double instead of long for same key
                        .build())
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ambiguous map schema")
                .hasMessageContaining("longKey");

        // check union of keys is used
        payloadSpecification = generatePayloadSpecification(Arrays.asList(
                new Payload(ImmutableMap.<String, Object>builder()
                        .put("doubleKey", 42D)
                        .build()),
                new Payload(ImmutableMap.<String, Object>builder()
                        .put("longKey", 32L)
                        .build())
        )).get();
        assertThat(payloadSpecification.getType()).isEqualTo(PayloadType.MAP.payloadTypeName);
        assertThat(payloadSpecification.getSchema())
                .containsEntry("doubleKey", PayloadType.DOUBLE_VALUE.payloadTypeName)
                .containsEntry("longKey", PayloadType.LONG_VALUE.payloadTypeName)
                .hasSize(2);

        // check emptylist is ok when other bucket has values
        payloadSpecification = generatePayloadSpecification(Arrays.asList(
                new Payload(ImmutableMap.<String, Object>builder()
                        .put("doubleArrayKeyOptional", emptyList())
                        .put("longArrayKeyOptional", singletonList(33L))
                        .build()),
                new Payload(ImmutableMap.<String, Object>builder()
                        .put("doubleArrayKeyOptional", singletonList(41D))
                        .put("longArrayKeyOptional", emptyList())
                        .build())
        )).get();
        assertThat(payloadSpecification.getType()).isEqualTo(PayloadType.MAP.payloadTypeName);
        assertThat(payloadSpecification.getSchema())
                .containsEntry("doubleArrayKeyOptional", PayloadType.DOUBLE_ARRAY.payloadTypeName)
                .containsEntry("longArrayKeyOptional", PayloadType.LONG_ARRAY.payloadTypeName)
                .hasSize(2);

        // check empty list conflicts with non list type
        assertThatThrownBy(() -> generatePayloadSpecification(Arrays.asList(
                new Payload(ImmutableMap.<String, Object>builder()
                        .put("longKey", emptyList())
                        .build()),
                new Payload(ImmutableMap.<String, Object>builder()
                        .put("longKey", 42D) // double instead of long for same key
                        .build())
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot infer map schema type")
                .hasMessageContaining("longKey");
    }

    private static TestDefinition stubTestDefinition(final String description, final List<TestBucket> buckets) {
        return new TestDefinition(
                "empty",
                "",
                TestType.ANONYMOUS_USER,
                "salty",
                buckets,
                emptyList(),
                false,
                emptyMap(),
                emptyMap(),
                description,
                emptyList()
        );
    }
}
