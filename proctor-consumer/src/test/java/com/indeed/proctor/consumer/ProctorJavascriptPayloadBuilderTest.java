package com.indeed.proctor.consumer;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.NameObfuscator;
import com.indeed.proctor.consumer.ProctorGroupStubber.FakeTest;
import org.junit.Before;
import org.junit.Test;

import static com.indeed.proctor.consumer.ProctorGroupStubber.ProctorGroupsWithForced;
import static com.indeed.proctor.consumer.ProctorGroupStubber.ProctorGroupsWithHoldout;
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
    private ProctorGroupsWithForced groupsWithForced;
    private ProctorGroupsWithHoldout groupsWithHoldOut;

    @Before
    public void setUp() {
        final ProctorResult proctorResult = buildSampleProctorResult();
        groups = new AbstractGroups(proctorResult) {};
        groupsWithForced = new ProctorGroupsWithForced(proctorResult);
        groupsWithHoldOut = new ProctorGroupsWithHoldout(proctorResult);
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
                        asList(0, "controlPayload"),
                        asList(1, "activePayload")
                );
        final ProctorJavascriptPayloadBuilder groupsWithForcedBuilder = new ProctorJavascriptPayloadBuilder(groupsWithForced);
        assertThat(groupsWithForcedBuilder.buildAlphabetizedListJavascriptConfig(stubTests))
                .containsExactly(
                        asList(42, null),
                        asList(0, "controlPayload"),
                        asList(0, "controlPayload") // forced
                );
        final ProctorJavascriptPayloadBuilder groupsWithHoldoutBuilder = new ProctorJavascriptPayloadBuilder(groupsWithHoldOut);
        assertThat(groupsWithHoldoutBuilder.buildAlphabetizedListJavascriptConfig(stubTests))
                .containsExactly(
                        asList(42, null), // no fallback
                        asList(-1, null),
                        asList(-1, null)
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
                        .put(CONTROL_NAME_HASH, asList(0, "controlPayload"))
                        .put(ACTIVE_NAME_HASH, asList(1, "activePayload"))
                        .build()
                );
        final ProctorJavascriptPayloadBuilder groupsWithForcedBuilder = new ProctorJavascriptPayloadBuilder(groupsWithForced);
        assertThat(groupsWithForcedBuilder.buildObfuscatedJavaScriptConfigMap(stubTests))
                .isEqualTo(new ImmutableMap.Builder<>()
                        .put(NOTEXIST_NAME_HASH, asList(42, null))
                        .put(CONTROL_NAME_HASH, asList(0, "controlPayload"))
                        .put(ACTIVE_NAME_HASH, asList(0, "controlPayload")) // forced
                        .build()
                );
        final ProctorJavascriptPayloadBuilder groupsWithHoldoutBuilder = new ProctorJavascriptPayloadBuilder(groupsWithHoldOut);
        assertThat(groupsWithHoldoutBuilder.buildObfuscatedJavaScriptConfigMap(stubTests))
                .isEqualTo(new ImmutableMap.Builder<>()
                        .put(NOTEXIST_NAME_HASH, asList(42, null)) // no fallback
                        .put(CONTROL_NAME_HASH, asList(-1, null))
                        .put(ACTIVE_NAME_HASH, asList(-1, null))
                        .build()
                );
    }
}
