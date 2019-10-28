package com.indeed.proctor.common.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

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
                ImmutableList.of("sample_test_tag")
        );
    }

    private static Map<String, Object> sampleSpecialConstants() {
        return ImmutableMap.of(
                "__COUNTRIES", Lists.newArrayList("US")
        );
    }

    private static Map<String, Object> sampleConstants() {
        return ImmutableMap.of(
                "COUNTRY_A", "US"
        );
    }

    private static List<Allocation> sampleAllocations() {
        return Lists.newArrayList(
                new Allocation(
                        "country == COUNTRY_A",
                        Lists.newArrayList(
                                new Range(
                                        -1,
                                        1.0
                                )
                        ),
                        "#A1"
                )
        );
    }

    private static List<TestBucket> sampleDoublePayloadBuckets() {
        final Payload p1 = new Payload();
        p1.setDoubleValue(1.0);
        return Lists.newArrayList(
                new TestBucket(
                        "inactive",
                        -1,
                        "inactive",
                        p1
                )
        );
    }

    private static List<TestBucket> sampleStringArrayBuckets() {
        final Payload p1 = new Payload();
        p1.setStringArray(new String[] {"foo", "bar"});
        return Lists.newArrayList(
                new TestBucket(
                        "inactive",
                        -1,
                        "inactive",
                        p1
                )
        );
    }


    @Test
    public void testEqualityWithAlmostEmptyTestDefinition() {
        // compare minimal TestDefinition to minimal that has one change
        for (final Function<TestDefinition, TestDefinition> modifier : Arrays.asList(
                (Function<TestDefinition, TestDefinition>) input -> {
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
                }
        )) {
            final TestDefinition initialTest = new TestDefinition();
            final TestDefinition modifiedTest = modifier.apply(new TestDefinition());
            final TestDefinition anotherModifiedTest = modifier.apply(new TestDefinition());
            assertThat(initialTest, not(equalTo(anotherModifiedTest)));
            assertEquals(modifiedTest, anotherModifiedTest);
            assertEquals(modifiedTest.hashCode(), anotherModifiedTest.hashCode());
        }
    }

    @Test
    public void testUnEqualityModified() {
        // compare a filled sample with one that has changes in the collections
        final TestDefinition sampleTest = sample();

        for (final Function<TestDefinition, TestDefinition> modifier : Arrays.asList(
                (Function<TestDefinition, TestDefinition>) input -> {
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
                    input.getBuckets().get(0).setName("new bucket name");
                    return input;
                },
                input -> {
                    input.getBuckets().get(0).setDescription("new bucket description");
                    return input;
                },
                input -> {
                    input.getBuckets().get(0).setValue(42);
                    return input;
                },
                input -> {
                    input.getBuckets().get(0).setPayload(Payload.EMPTY_PAYLOAD);
                    return input;
                },
                input -> {
                    final Payload p2 = new Payload();
                    p2.setDoubleValue(42.1);
                    input.getBuckets().get(0).setPayload(p2);
                    return input;
                },
                input -> {
                    final Payload p2 = new Payload();
                    p2.setStringValue("1");
                    input.getBuckets().get(0).setPayload(p2);
                    return input;
                },
                input -> {
                    input.setConstants(ImmutableMap.of("COUNTRY_A", "CA"));
                    return input;
                },
                input -> {
                    input.setConstants(ImmutableMap.of("COUNTRY_A", "US", "COUNTRY_B", "CA"));
                    return input;
                },
                input -> {
                    input.setConstants(ImmutableMap.of("COUNTRY_A", Lists.newArrayList("CA")));
                    return input;
                },
                input -> {
                    input.setConstants(ImmutableMap.of("COUNTRY_A", Lists.newArrayList("US", "CA")));
                    return input;
                }
        )) {
            final TestDefinition anotherTest = modifier.apply(sample());
            assertThat(sampleTest, not(equalTo(anotherTest)));
        }
    }

    @Test
    public void testHashCode() {
        final TestDefinition otherTest = sample();
        final TestDefinition sampleTest = sample();
        assertEquals(sampleTest.hashCode(), otherTest.hashCode());
        otherTest.getBuckets().get(0).setDescription("changed");
        assertNotEquals(sampleTest.hashCode(), otherTest.hashCode());
    }

}
