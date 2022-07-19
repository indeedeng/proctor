package com.indeed.proctor.common;

import com.google.common.collect.ImmutableList;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class TestTestChooser {
    static Map<String, Object> payloadMap = new HashMap<>();

    static {
        payloadMap.put("test_key1", 1.0);
        payloadMap.put("test_key2", 2L);
        payloadMap.put("test_key3", "three");
        payloadMap.put("test_key4", new Double[]{1.0, 2.0});
        payloadMap.put("test_key5", new Long[]{1L, 2L});
        payloadMap.put("test_key6", new String[]{"three", "five"});
    }
    private static final List<TestBucket> TEST_BUCKETS = ImmutableList.of(
            new TestBucket("inactive", -1, "inactive", null),
            new TestBucket("control", 0, "control", null),
            new TestBucket("active", 1, "active", new Payload("active_payload")),
            new TestBucket("payload_tst", 2, "test payload", new Payload(payloadMap))
    );
    private static final List<Allocation> ALLOCATIONS = ImmutableList.of(new Allocation(
            "${}",
            ImmutableList.of(
                    new Range(0, 0.5),
                    new Range(-1, 0),
                    new Range(1, 0.5)
            ),
            "#A1"
    ));
    private static final MaxBucketTestChooser TEST_CHOOSER = new MaxBucketTestChooser();

    private static final class MaxBucketTestChooser implements TestChooser<Void> {
        private final ConsumableTestDefinition consumableTestDefinition;

        public MaxBucketTestChooser() {
            final ConsumableTestDefinition consumableTestDefinition = new ConsumableTestDefinition();
            consumableTestDefinition.setBuckets(TEST_BUCKETS);
            consumableTestDefinition.setAllocations(ALLOCATIONS);
            this.consumableTestDefinition = consumableTestDefinition;
        }

        @Override
        public void printTestBuckets(@Nonnull final PrintWriter writer) {
        }

        @Nullable
        @Override
        public TestBucket getTestBucket(final int value) {
            return consumableTestDefinition.getBuckets()
                    .stream()
                    .filter(bucket -> bucket.getValue() == value)
                    .findFirst()
                    .orElse(null);
        }

        @Nonnull
        @Override
        public String[] getRules() {
            return consumableTestDefinition.getAllocations()
                    .stream()
                    .map(Allocation::getRule)
                    .toArray(String[]::new);
        }

        @Nonnull
        @Override
        public ConsumableTestDefinition getTestDefinition() {
            return consumableTestDefinition;
        }

        @Nonnull
        @Override
        public String getTestName() {
            return "example_tst";
        }

        @Nonnull
        @Override
        public Result chooseInternal(
                @Nullable final Void identifier,
                @Nonnull final Map<String, Object> values,
                @Nonnull final Map<String, TestBucket> testGroups
        ) {
            final Optional<Allocation> firstAllocation = consumableTestDefinition
                    .getAllocations()
                    .stream()
                    .findFirst();
            final Optional<Integer> maxBucketValue = firstAllocation
                    .map(Allocation::getRanges)
                    .map(Collection::stream)
                    .orElse(Stream.empty())
                    .map(Range::getBucketValue)
                    .max(Integer::compareTo);
            return new Result(
                    maxBucketValue.map(this::getTestBucket).orElse(null),
                    firstAllocation.orElse(null)
            );
        }
    }

    @Test
    public void testChoose_withEmptyForceGroupsOptions() {
        assertThat(compareTestChooserResults(
                choose(ForceGroupsOptions.empty()),
                new TestChooser.Result(
                        TEST_CHOOSER.getTestBucket(1),
                        ALLOCATIONS.get(0)
                )
        )).isTrue();
    }

    @Test
    public void testChoose_withForceGroup() {
        assertThat(compareTestChooserResults(
                choose(ForceGroupsOptions.builder()
                        .putForceGroup(TEST_CHOOSER.getTestName(), -1)
                        .build()),
                new TestChooser.Result(
                        TEST_CHOOSER.getTestBucket(-1),
                        null
                )
        )).isTrue();
    }

    @Test
    public void testChoose_withForceGroupAndForcePayload() {
        final Payload testPayload = new Payload("test_force_payload");

        final TestBucket testBucket = TestBucket.builder()
                .from(TEST_CHOOSER.getTestBucket(1))
                .payload(testPayload)
                .build();

        final ForceGroupsOptions options = ForceGroupsOptions.builder()
                .putForceGroup(TEST_CHOOSER.getTestName(), 1)
                .putForcePayload(TEST_CHOOSER.getTestName(), testPayload)
                .build();

        final TestChooser.Result result = choose(options);

        assertThat(compareTestChooserResults(
                result,
                new TestChooser.Result(
                        testBucket,
                        null
                )
        )).isTrue();
    }

    @Test
    public void testChoose_withForceGroupAndForcePayloadMap() {
        final Map<String, Object> map = new HashMap<>();

        map.put("test_key1", 6.0);
        map.put("test_key2", 6L);
        map.put("test_key3", "six");
        map.put("test_key4", new Double[]{6.0, 6.0});
        map.put("test_key5", new Long[]{6L, 6L});
        map.put("test_key6", new String[]{"six", "six"});

        final Payload testPayload = new Payload(map);

        final TestBucket testBucket = TestBucket.builder()
                .from(TEST_CHOOSER.getTestBucket(2))
                .build();

        final ForceGroupsOptions options = ForceGroupsOptions.builder()
                .putForceGroup(TEST_CHOOSER.getTestName(), 2)
                .putForcePayload(TEST_CHOOSER.getTestName(), testPayload)
                .build();

        final TestChooser.Result result = choose(options);

        assertThat(compareTestChooserResults(
                result,
                new TestChooser.Result(
                        testBucket,
                        null
                )
        )).isTrue();
    }

    @Test
    public void testChoose_withForceGroupAndForcePayloadInvalidMap() {
        final Map<String, Object> payloadMap = new HashMap<>();

        payloadMap.put("test_key1", 3L);
        payloadMap.put("test_key2", 2L);
        payloadMap.put("test_key3", "three");
        payloadMap.put("test_key4", new Double[]{1.0, 2.0});
        payloadMap.put("test_key5", new Long[]{1L, 2L});
        payloadMap.put("test_key6", new String[]{"three", "five"});

        final Payload testPayload = new Payload("test_force_payload");

        final TestBucket testBucket = TestBucket.builder()
                .from(TEST_CHOOSER.getTestBucket(2))
                .build();

        final ForceGroupsOptions options = ForceGroupsOptions.builder()
                .putForceGroup(TEST_CHOOSER.getTestName(), 2)
                .putForcePayload(TEST_CHOOSER.getTestName(), testPayload)
                .build();

        final TestChooser.Result result = choose(options);

        assertThat(compareTestChooserResults(
                result,
                new TestChooser.Result(
                        testBucket,
                        null
                )
        )).isTrue();
    }

    @Test
    public void testChoose_withForceGroupToUnknown() {
        // Forcing to use an unknown bucket should be ignored.
        assertThat(compareTestChooserResults(
                choose(ForceGroupsOptions.builder()
                        .putForceGroup(TEST_CHOOSER.getTestName(), -2)
                        .build()),
                new TestChooser.Result(
                        TEST_CHOOSER.getTestBucket(1),
                        ALLOCATIONS.get(0)
                )
        )).isTrue();
    }

    @Test
    public void testChoose_withForceGroupForOtherTests() {
        assertThat(compareTestChooserResults(
                choose(ForceGroupsOptions.builder()
                        .putForceGroup("other_tst", 0)
                        .build()),
                new TestChooser.Result(
                        TEST_CHOOSER.getTestBucket(1),
                        ALLOCATIONS.get(0)
                )
        )).isTrue();
    }

    @Test
    public void testChoose_withDefaultToFallback() {
        assertThat(compareTestChooserResults(
                choose(ForceGroupsOptions.builder()
                        .setDefaultMode(ForceGroupsDefaultMode.FALLBACK)
                        .build()),
                new TestChooser.Result(
                        null,
                        null
                )
        )).isTrue();
    }

    @Test
    public void testChoose_withDefaultToMinActive() {
        assertThat(compareTestChooserResults(
                choose(ForceGroupsOptions.builder()
                        .setDefaultMode(ForceGroupsDefaultMode.MIN_LIVE)
                        .build()),
                new TestChooser.Result(
                        TEST_CHOOSER.getTestBucket(0),
                        null
                )
        )).isTrue();
    }

    @Test
    public void testChoose_withForceGroupAndDefaultToFallback() {
        assertThat(compareTestChooserResults(
                choose(ForceGroupsOptions.builder()
                        .setDefaultMode(ForceGroupsDefaultMode.FALLBACK)
                        .putForceGroup(TEST_CHOOSER.getTestName(), -1)
                        .build()),
                new TestChooser.Result(
                        TEST_CHOOSER.getTestBucket(-1),
                        null
                )
        )).isTrue();
    }

    @Test
    public void testChoose_withForceGroupToUnknownAndDefaultToFallback() {
        assertThat(compareTestChooserResults(
                choose(ForceGroupsOptions.builder()
                        .setDefaultMode(ForceGroupsDefaultMode.FALLBACK)
                        .putForceGroup(TEST_CHOOSER.getTestName(), -2)
                        .build()),
                new TestChooser.Result(
                        null,
                        null
                )
        )).isTrue();
    }

    @Test
    public void testChoose_withForceGroupAndDefaultToMinActive() {
        assertThat(compareTestChooserResults(
                choose(ForceGroupsOptions.builder()
                        .setDefaultMode(ForceGroupsDefaultMode.MIN_LIVE)
                        .putForceGroup(TEST_CHOOSER.getTestName(), -1)
                        .build()),
                new TestChooser.Result(
                        TEST_CHOOSER.getTestBucket(-1),
                        null
                )
        )).isTrue();
    }

    @Test
    public void testChoose_withForceGroupToUnknownAndDefaultToMinActive() {
        assertThat(compareTestChooserResults(
                choose(ForceGroupsOptions.builder()
                        .setDefaultMode(ForceGroupsDefaultMode.MIN_LIVE)
                        .putForceGroup(TEST_CHOOSER.getTestName(), -2)
                        .build()),
                new TestChooser.Result(
                        TEST_CHOOSER.getTestBucket(0),
                        null
                )
        )).isTrue();
    }

    @Test
    public void testvalidateForcePayload() {
        final Map<String, Object> map = new HashMap<>();

        map.put("test_key1", 3L);
        map.put("test_key2", 2.0);
        map.put("test_key3", "three");
        map.put("test_key4", new Double[]{1.0, 2.0});
        map.put("test_key5", new Long[]{1L, 2L});
        map.put("test_key6", new String[]{"three", "five"});

        // NOTE: input map is in String form as it still needs to be parsed and validated
        final Map<String, Object> inputMap = new HashMap<>();

        inputMap.put("test_key1", "1");
        inputMap.put("test_key2", "1");
        inputMap.put("test_key3", "one");
        inputMap.put("test_key4", "1.0, 1.0");
        inputMap.put("test_key5", "1, 1");
        inputMap.put("test_key6", "\"one\", \"one\"");

        final Map<String, Object> validatedMapExpected = new HashMap<>();

        validatedMapExpected.put("test_key1", 1L);
        validatedMapExpected.put("test_key2", 1.0);
        validatedMapExpected.put("test_key3", "one");
        validatedMapExpected.put("test_key4", new Double[]{1.0, 1.0});
        validatedMapExpected.put("test_key5", new Long[]{1L, 1L});
        validatedMapExpected.put("test_key6", new String[]{"one", "one"});

        final Map<String, Object> invalidMap = new HashMap<>();

        invalidMap.put("test_key1", "1L");
        invalidMap.put("test_key2", "1L");
        invalidMap.put("test_key3", "one");
        invalidMap.put("test_key4", "invalid map value to actual payload");
        invalidMap.put("test_key5", "1L, 1L");
        invalidMap.put("test_key6", "\"one\", \"one\"");

        assertThat(new Payload(TEST_CHOOSER.validateForcePayloadMap(map, inputMap))).isEqualTo(new Payload(validatedMapExpected));
        assertThat(new Payload(TEST_CHOOSER.validateForcePayloadMap(map, invalidMap))).isEqualTo(new Payload(map));
    }

    @Test
    public void testvalidateForcePayload_withStringParsing() {
        final Map<String, Object> validatedMapExpected = new HashMap<>();

        validatedMapExpected.put("test_key1", 1.0);
        validatedMapExpected.put("test_key2", 1L);
        validatedMapExpected.put("test_key3", "one");
        validatedMapExpected.put("test_key4", new Double[]{1.0, 1.0});
        validatedMapExpected.put("test_key5", new Long[]{1L, 1L});
        validatedMapExpected.put("test_key6", new String[]{"one", "one"});

        // NOTE: input map is in String form as it still needs to be parsed and validated
        final String forcePayloadString = "map:[\"test_key1\":1, \"test_key2\":1, \"test_key3\":\"one\", \"test_key4\":[1, 1], \"test_key5\":[1, 1], \"test_key6\":[\"one\", \"one\"]]";

        assertThat(choose(ForceGroupsOptions.builder()
                .putForceGroup(TEST_CHOOSER.getTestName(), 2)
                .putForcePayload(TEST_CHOOSER.getTestName(), ForceGroupsOptionsStrings.parseForcePayloadString(forcePayloadString))
                .build()).getTestBucket().getPayload()).isEqualTo(new Payload(validatedMapExpected));
    }

    private static boolean compareTestChooserResults(final TestChooser.Result a, final TestChooser.Result b) {
        if (a.getTestBucket() != null && b.getTestBucket() != null ) {
            return a.getTestBucket().equals(b.getTestBucket()) && a.getAllocation() == b.getAllocation();
        }
        return a.getTestBucket() == b.getTestBucket() && a.getAllocation() == b.getAllocation();
    }

    private static TestChooser.Result choose(final ForceGroupsOptions forceGroupsOptions) {
        return TEST_CHOOSER.choose(
                null,
                Collections.emptyMap(),
                Collections.emptyMap(),
                forceGroupsOptions
        );
    }
}
