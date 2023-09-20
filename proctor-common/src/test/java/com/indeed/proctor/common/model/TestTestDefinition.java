package com.indeed.proctor.common.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TestTestDefinition {

    static TestDefinition sample() {
        return new TestDefinition(
                "-1",
                "lang == 'en'",
                TestType.ANONYMOUS_USER,
                "&sample_test",
                sampleDoublePayloadBuckets(),
                sampleAllocations(),
                false,
                sampleConstants(),
                sampleSpecialConstants(),
                "sample test",
                ImmutableList.of("sample_test_tag"));
    }

    private static Map<String, Object> sampleSpecialConstants() {
        return ImmutableMap.of("__COUNTRIES", Lists.newArrayList("US"));
    }

    private static Map<String, Object> sampleConstants() {
        return ImmutableMap.of("COUNTRY_A", "US");
    }

    private static List<Allocation> sampleAllocations() {
        return Lists.newArrayList(
                new Allocation(
                        "country == COUNTRY_A", Lists.newArrayList(new Range(-1, 1.0)), "#A1"));
    }

    private static List<TestBucket> sampleDoublePayloadBuckets() {
        return Lists.newArrayList(new TestBucket("inactive", -1, "inactive", new Payload(1.0)));
    }

    private static List<TestBucket> sampleStringArrayBuckets() {
        return Lists.newArrayList(
                new TestBucket(
                        "inactive", -1, "inactive", new Payload(new String[] {"foo", "bar"})));
    }

    @Test
    public void testEqualityWithAlmostEmptyTestDefinition() {
        // compare minimal TestDefinition to minimal that has one change
        for (final Function<TestDefinition, TestDefinition> modifier :
                Arrays.asList(
                        (Function<TestDefinition, TestDefinition>)
                                input -> {
                                    input.setVersion("new version");
                                    return input;
                                },
                        input -> {
                            input.setRule("new rule");
                            return input;
                        },
                        input -> {
                            input.setSalt("new salt");
                            return input;
                        },
                        input -> {
                            input.setTestType(TestType.EMAIL_ADDRESS);
                            return input;
                        },
                        input -> {
                            input.setSilent(true);
                            return input;
                        },
                        input -> {
                            input.setDescription("new Description");
                            return input;
                        },
                        input -> {
                            input.setBuckets(TestTestDefinition.sampleDoublePayloadBuckets());
                            return input;
                        },
                        input -> {
                            input.setBuckets(TestTestDefinition.sampleStringArrayBuckets());
                            return input;
                        },
                        input -> {
                            input.setAllocations(TestTestDefinition.sampleAllocations());
                            return input;
                        },
                        input -> {
                            input.setConstants(TestTestDefinition.sampleConstants());
                            return input;
                        },
                        input -> {
                            input.setSpecialConstants(TestTestDefinition.sampleSpecialConstants());
                            return input;
                        })) {
            final TestDefinition initialTest = new TestDefinition();
            final TestDefinition modifiedTest = modifier.apply(new TestDefinition());
            final TestDefinition anotherModifiedTest = modifier.apply(new TestDefinition());
            assertThat(initialTest).isNotEqualTo(anotherModifiedTest);
            assertEquals(modifiedTest, anotherModifiedTest);
            assertEquals(modifiedTest.hashCode(), anotherModifiedTest.hashCode());
        }
    }

    @Test
    public void testUnEqualityModified() {
        // compare a filled sample with one that has changes in the collections
        final TestDefinition sampleTest = sample();

        for (final Function<TestDefinition, TestDefinition> modifier :
                Arrays.asList(
                        (Function<TestDefinition, TestDefinition>)
                                input -> {
                                    input.getAllocations().get(0).setRule("new allocation Rule");
                                    return input;
                                },
                        input -> {
                            input.getAllocations().get(0).setId("new Id");
                            return input;
                        },
                        input -> {
                            input.getAllocations().get(0).getRanges().get(0).setLength(0.3);
                            return input;
                        },
                        input -> {
                            input.getAllocations().get(0).getRanges().get(0).setBucketValue(7);
                            return input;
                        },
                        // buckets, special case due to bad equals method
                        input -> {
                            input.setBuckets(null);
                            return input;
                        },
                        input -> {
                            input.getBuckets()
                                    .set(
                                            0,
                                            TestBucket.builder()
                                                    .from(input.getBuckets().get(0))
                                                    .name("new bucket name")
                                                    .build());
                            return input;
                        },
                        input -> {
                            input.getBuckets()
                                    .set(
                                            0,
                                            TestBucket.builder()
                                                    .from(input.getBuckets().get(0))
                                                    .description("new bucket description")
                                                    .build());
                            return input;
                        },
                        input -> {
                            input.getBuckets()
                                    .set(
                                            0,
                                            TestBucket.builder()
                                                    .from(input.getBuckets().get(0))
                                                    .value(42)
                                                    .build());
                            return input;
                        },
                        input -> {
                            input.getBuckets()
                                    .set(
                                            0,
                                            TestBucket.builder()
                                                    .from(input.getBuckets().get(0))
                                                    .payload(Payload.EMPTY_PAYLOAD)
                                                    .build());
                            return input;
                        },
                        input -> {
                            input.getBuckets()
                                    .set(
                                            0,
                                            TestBucket.builder()
                                                    .from(input.getBuckets().get(0))
                                                    .payload(new Payload(42.1))
                                                    .build());
                            return input;
                        },
                        input -> {
                            input.getBuckets()
                                    .set(
                                            0,
                                            TestBucket.builder()
                                                    .from(input.getBuckets().get(0))
                                                    .payload(new Payload("1"))
                                                    .build());
                            return input;
                        },
                        input -> {
                            input.setConstants(ImmutableMap.of("COUNTRY_A", "CA"));
                            return input;
                        },
                        input -> {
                            input.setConstants(
                                    ImmutableMap.of("COUNTRY_A", "US", "COUNTRY_B", "CA"));
                            return input;
                        },
                        input -> {
                            input.setConstants(
                                    ImmutableMap.of("COUNTRY_A", Lists.newArrayList("CA")));
                            return input;
                        },
                        input -> {
                            input.setConstants(
                                    ImmutableMap.of("COUNTRY_A", Lists.newArrayList("US", "CA")));
                            return input;
                        })) {
            final TestDefinition anotherTest = modifier.apply(sample());
            assertThat(sampleTest).isNotEqualTo(anotherTest);
        }
    }

    @Test
    public void testHashCode() {
        final TestDefinition otherTest = sample();
        final TestDefinition sampleTest = sample();
        assertEquals(sampleTest.hashCode(), otherTest.hashCode());
        otherTest
                .getBuckets()
                .set(
                        0,
                        TestBucket.builder()
                                .from(otherTest.getBuckets().get(0))
                                .description("changed")
                                .build());
        assertNotEquals(sampleTest.hashCode(), otherTest.hashCode());
    }

    @Test
    public void testBuilder() {
        final String version = "-1";
        final String rule = "lang == 'en'";
        final TestType testType = TestType.ANONYMOUS_USER;
        final String salt = "&sample_test";
        final List<TestBucket> buckets = sampleDoublePayloadBuckets();
        final List<Allocation> allocations = sampleAllocations();
        final boolean silent = true;
        final Map<String, Object> constants = sampleConstants();
        final Map<String, Object> specialContants = sampleSpecialConstants();
        final String description = "sample test";
        final List<String> metatags = ImmutableList.of("sample_test_tag");
        final TestDependency dependsOn = new TestDependency("sample_par_test", 1);
        final boolean anonymous = true;

        final TestDefinition definition1 =
                TestDefinition.builder()
                        .setVersion(version)
                        .setRule(rule)
                        .setTestType(testType)
                        .setSalt(salt)
                        .setBuckets(buckets)
                        .setAllocations(allocations)
                        .setSilent(silent)
                        .setConstants(constants)
                        .setSpecialConstants(specialContants)
                        .setDescription(description)
                        .setMetaTags(metatags)
                        .setDependsOn(dependsOn)
                        .setAnonymous(anonymous)
                        .build();

        final TestDefinition definition2 = TestDefinition.builder().from(definition1).build();

        for (final TestDefinition definition : Arrays.asList(definition1, definition2)) {
            assertThat(definition.getVersion()).isEqualTo(version);
            assertThat(definition.getRule()).isEqualTo(rule);
            assertThat(definition.getTestType()).isEqualTo(testType);
            assertThat(definition.getSalt()).isEqualTo(salt);
            assertThat(definition.getBuckets()).isEqualTo(buckets);
            assertThat(definition.getAllocations()).isEqualTo(allocations);
            assertThat(definition.getSilent()).isEqualTo(silent);
            assertThat(definition.getConstants()).isEqualTo(constants);
            assertThat(definition.getSpecialConstants()).isEqualTo(specialContants);
            assertThat(definition.getDescription()).isEqualTo(description);
            assertThat(definition.getMetaTags()).isEqualTo(metatags);
            assertThat(definition.getDependsOn()).isEqualTo(dependsOn);
            assertThat(definition.getAnonymous()).isEqualTo(anonymous);
        }
    }
}
