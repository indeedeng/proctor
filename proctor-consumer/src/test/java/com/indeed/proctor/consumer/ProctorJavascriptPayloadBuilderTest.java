package com.indeed.proctor.consumer;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.NameObfuscator;
import com.indeed.proctor.consumer.ProctorGroupStubber.FakeTest;
import org.junit.Before;
import org.junit.Test;

import static com.indeed.proctor.consumer.ProctorGroupStubber.CONTROL_BUCKET_WITH_PAYLOAD;
import static com.indeed.proctor.consumer.ProctorGroupStubber.FALLBACK_TEST_BUCKET;
import static com.indeed.proctor.consumer.ProctorGroupStubber.GROUP_1_BUCKET;
import static com.indeed.proctor.consumer.ProctorGroupStubber.GROUP_1_BUCKET_WITH_PAYLOAD;
import static com.indeed.proctor.consumer.ProctorGroupStubber.INACTIVE_BUCKET;
import static com.indeed.proctor.consumer.ProctorGroupStubber.StubTest.CONTROL_SELECTED_TEST;
import static com.indeed.proctor.consumer.ProctorGroupStubber.StubTest.GROUP1_SELECTED_TEST;
import static com.indeed.proctor.consumer.ProctorGroupStubber.StubTest.NO_BUCKETS_WITH_FALLBACK_TEST;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class ProctorJavascriptPayloadBuilderTest {

    private static final NameObfuscator NAME_OBFUSCATOR = new NameObfuscator();
    public static final String NOTEXIST_TESTNAME = "notexist";
    private static final String NOTEXIST_NAME_HASH = NAME_OBFUSCATOR.obfuscateTestName(NOTEXIST_TESTNAME);
    private static final String CONTROL_NAME_HASH = NAME_OBFUSCATOR.obfuscateTestName(CONTROL_SELECTED_TEST.getName());
    private static final String ACTIVE_NAME_HASH = NAME_OBFUSCATOR.obfuscateTestName(GROUP1_SELECTED_TEST.getName());
    private static final String FALLBACK_NAME_HASH = NAME_OBFUSCATOR.obfuscateTestName(NO_BUCKETS_WITH_FALLBACK_TEST.getName());

    private AbstractGroups groups;

    @Before
    public void setUp() {
        final ProctorResult proctorResult = new ProctorGroupStubber.ProctorResultStubBuilder()
                .withStubTest(ProctorGroupStubber.StubTest.CONTROL_SELECTED_TEST, CONTROL_BUCKET_WITH_PAYLOAD,
                        INACTIVE_BUCKET, CONTROL_BUCKET_WITH_PAYLOAD, GROUP_1_BUCKET_WITH_PAYLOAD)
                .withStubTest(ProctorGroupStubber.StubTest.GROUP1_SELECTED_TEST, GROUP_1_BUCKET_WITH_PAYLOAD,
                        INACTIVE_BUCKET, CONTROL_BUCKET_WITH_PAYLOAD, GROUP_1_BUCKET_WITH_PAYLOAD)
                .withStubTest(NO_BUCKETS_WITH_FALLBACK_TEST, null,
                        INACTIVE_BUCKET, GROUP_1_BUCKET, FALLBACK_TEST_BUCKET)
                .build();
        groups = new AbstractGroups(proctorResult) {};
    }

    @Test
    public void testGetJavaScriptConfigLists() {
        final ProctorJavascriptPayloadBuilder groupsBuilder = new ProctorJavascriptPayloadBuilder(groups);
        final FakeTest[] stubTests = {
                new FakeTest("notexist", 77), // use fallback value
                new FakeTest(CONTROL_SELECTED_TEST.getName(), 98),
                new FakeTest(GROUP1_SELECTED_TEST.getName(), 99),
                new FakeTest(NO_BUCKETS_WITH_FALLBACK_TEST.getName(), FALLBACK_TEST_BUCKET.getValue())}; // use fallback value and payload

        assertThat(groupsBuilder.buildAlphabetizedListJavascriptConfig(stubTests))
                .containsExactly(
                        asList(77, null),
                        asList(0, CONTROL_BUCKET_WITH_PAYLOAD.getPayload().getStringValue()),
                        asList(1, GROUP_1_BUCKET_WITH_PAYLOAD.getPayload().getStringValue()),
                        asList(FALLBACK_TEST_BUCKET.getValue(), FALLBACK_TEST_BUCKET.getPayload().getStringValue())
                );
    }

    @Test
    public void buildObfuscatedJavaScriptConfigMap() {
        final ProctorJavascriptPayloadBuilder groupsBuilder = new ProctorJavascriptPayloadBuilder(groups);
        final FakeTest[] stubTests = {
                new FakeTest("notexist", 77), // use fallback value
                new FakeTest(CONTROL_SELECTED_TEST.getName(), 43),
                new FakeTest(GROUP1_SELECTED_TEST.getName(), 44),
                new FakeTest(NO_BUCKETS_WITH_FALLBACK_TEST.getName(), FALLBACK_TEST_BUCKET.getValue())}; // use fallback value and payload

        assertThat(groupsBuilder.buildObfuscatedJavaScriptConfigMap(stubTests))
                .isEqualTo(new ImmutableMap.Builder<>()
                        .put(NOTEXIST_NAME_HASH, asList(77, null))
                        .put(CONTROL_NAME_HASH, asList(0, CONTROL_BUCKET_WITH_PAYLOAD.getPayload().getStringValue()))
                        .put(ACTIVE_NAME_HASH, asList(1, GROUP_1_BUCKET_WITH_PAYLOAD.getPayload().getStringValue()))
                        .put(FALLBACK_NAME_HASH, asList(FALLBACK_TEST_BUCKET.getValue(), FALLBACK_TEST_BUCKET.getPayload().getStringValue()))
                        .build()
                );
    }
}
