package com.indeed.proctor.consumer;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.NameObfuscator;
import com.indeed.proctor.consumer.ProctorGroupStubber.FakeTest;
import org.junit.Before;
import org.junit.Test;

import static com.indeed.proctor.consumer.ProctorGroupStubber.CONTROL_BUCKET_WITH_PAYLOAD;
import static com.indeed.proctor.consumer.ProctorGroupStubber.GROUP_1_BUCKET_WITH_PAYLOAD;
import static com.indeed.proctor.consumer.ProctorGroupStubber.StubTest.CONTROL_SELECTED_TEST;
import static com.indeed.proctor.consumer.ProctorGroupStubber.StubTest.GROUP1_SELECTED_TEST;
import static com.indeed.proctor.consumer.ProctorGroupStubber.buildSampleProctorResult;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class ProctorJavascriptPayloadBuilderTest {

    private static final NameObfuscator NAME_OBFUSCATOR = new NameObfuscator();
    public static final String NOTEXIST_TESTNAME = "notexist";
    private static final String NOTEXIST_NAME_HASH = NAME_OBFUSCATOR.obfuscateTestName(NOTEXIST_TESTNAME);
    private static final String CONTROL_NAME_HASH = NAME_OBFUSCATOR.obfuscateTestName(CONTROL_SELECTED_TEST.getName());
    private static final String ACTIVE_NAME_HASH = NAME_OBFUSCATOR.obfuscateTestName(GROUP1_SELECTED_TEST.getName());

    private AbstractGroups groups;

    @Before
    public void setUp() {
        final ProctorResult proctorResult = buildSampleProctorResult();
        groups = new AbstractGroups(proctorResult) {};


    }

    @Test
    public void testGetJavaScriptConfigLists() {
        final ProctorJavascriptPayloadBuilder groupsBuilder = new ProctorJavascriptPayloadBuilder(groups);
        final FakeTest[] stubTests = {
                new FakeTest("notexist", 42),
                new FakeTest(CONTROL_SELECTED_TEST.getName(), 43),
                new FakeTest(GROUP1_SELECTED_TEST.getName(), 44)};

        assertThat(groupsBuilder.buildAlphabetizedListJavascriptConfig(stubTests))
                .containsExactly(
                        asList(42, null),
                        asList(0, CONTROL_BUCKET_WITH_PAYLOAD.getPayload().getStringValue()),
                        asList(1, GROUP_1_BUCKET_WITH_PAYLOAD.getPayload().getStringValue())
                );
    }

    @Test
    public void buildObfuscatedJavaScriptConfigMap() {
        final ProctorJavascriptPayloadBuilder groupsBuilder = new ProctorJavascriptPayloadBuilder(groups);
        final FakeTest[] stubTests = {
                new FakeTest("notexist", 42),
                new FakeTest(CONTROL_SELECTED_TEST.getName(), 43),
                new FakeTest(GROUP1_SELECTED_TEST.getName(), 44)};

        assertThat(groupsBuilder.buildObfuscatedJavaScriptConfigMap(stubTests))
                .isEqualTo(new ImmutableMap.Builder<>()
                        .put(NOTEXIST_NAME_HASH, asList(42, null))
                        .put(CONTROL_NAME_HASH, asList(0, CONTROL_BUCKET_WITH_PAYLOAD.getPayload().getStringValue()))
                        .put(ACTIVE_NAME_HASH, asList(1, GROUP_1_BUCKET_WITH_PAYLOAD.getPayload().getStringValue()))
                        .build()
                );
    }
}
