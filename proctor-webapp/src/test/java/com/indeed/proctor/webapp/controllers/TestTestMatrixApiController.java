package com.indeed.proctor.webapp.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.store.ChangeMetadata;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.store.utils.test.InMemoryProctorStore;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.model.WebappConfiguration;
import com.indeed.proctor.webapp.model.api.TestHistoriesResponseModel;
import com.indeed.proctor.webapp.views.JsonView;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestTestMatrixApiController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestMatrixApiController controller;
    private InMemoryProctorStore trunkStore;
    private InMemoryProctorStore qaStore;
    private InMemoryProctorStore prodStore;

    private static final String STUB_TEST_NAME = "dummyTest";
    private static final TestDefinition STUB_TEST_DEFINITION = new TestDefinition();

    static {
        STUB_TEST_DEFINITION.setSalt("&stub_test");
        STUB_TEST_DEFINITION.setRule("country == \"US\"");
        STUB_TEST_DEFINITION.setTestType(TestType.ANONYMOUS_USER);
        STUB_TEST_DEFINITION.setBuckets(ImmutableList.of(new TestBucket("", 1, "")));
        STUB_TEST_DEFINITION.setAllocations(ImmutableList.of(new Allocation(
                "",
                ImmutableList.of(new Range(1, 1.0))
        )));
    }

    @Before
    public void setUp() {
        final WebappConfiguration configuration =
                new WebappConfiguration(false, false, 1000, 10);
        final Supplier<String> generator = InMemoryProctorStore.autoincrementRevisionIdGenerator();
        trunkStore = new InMemoryProctorStore(generator);
        qaStore = new InMemoryProctorStore(generator);
        prodStore = new InMemoryProctorStore(generator);
        controller = new TestMatrixApiController(configuration, trunkStore, qaStore, prodStore);
    }

    @Test
    public void testGetTestMatrixNotFound() throws Exception {
        assertThatThrownBy(() -> controller.getTestMatrix("1234"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("1234 not correct");
    }

    @Test
    public void testGetTestMatrixForBranch() throws Exception {
        final String version = addTest(trunkStore, STUB_TEST_NAME, STUB_TEST_DEFINITION);
        final JsonView jsonView = controller.getTestMatrix(Environment.WORKING.getName());
        final String json = renderedJson(jsonView);
        assertThat(json).contains("\"version\" : \"" + version + "\"");
    }

    @Test
    public void testGetTestMatrixForRevision() throws Exception {
        final String trunkRevision = addStubTest(trunkStore);
        final String qaRevision = addStubTest(qaStore);
        final String prodRevision = addStubTest(prodStore);

        final List<String> revisions = Arrays.asList(
                trunkRevision,
                qaRevision,
                prodRevision
        );

        for (final String revision : revisions) {
            final JsonView jsonView = controller.getTestMatrix(revision);
            final TestMatrixVersion testMatrixVersion = parsedRenderedJson(
                    jsonView, TestMatrixVersion.class);

            assertThat(testMatrixVersion.getVersion())
                    .isEqualTo(revision);
        }
    }

    @Test
    public void testGetTestMatrixHistoryForRevisionEmpty() throws Exception {
        final String trunkRevision = addStubTest(trunkStore);
        final String qaRevision = addStubTest(qaStore);
        final String prodRevision = addStubTest(prodStore);

        final String unknownRevision = "1234";
        assertThat(unknownRevision).isNotIn(trunkRevision, qaRevision, prodRevision);

        assertThatThrownBy(() -> controller.getTestMatrixHistory(unknownRevision, 1, 100))
                .isInstanceOf(TestMatrixApiController.ResourceNotFoundException.class);
    }

    @Test
    public void testGetTestMatrixHistoryForBranchEmpty() throws Exception {
        addStubTest(trunkStore);
        addStubTest(qaStore);

        final JsonView jsonView = controller.getTestMatrixHistory(Environment.PRODUCTION.getName(), 1, 100);
        assertThat(parsedRenderedJson(jsonView, List.class)).isEqualTo(emptyList());
    }

    @Test
    public void testGetTestDefinitionNotFound() throws Exception {
        final String trunkRevision = addStubTest(trunkStore);

        final String unknownTestName = "fooTest";
        assertThat(unknownTestName).isNotEqualTo(STUB_TEST_NAME);

        assertThatThrownBy(() -> controller.getTestDefinition(trunkRevision, unknownTestName))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("fooTest not found");
    }

    @Test
    public void testGetTestDefinitionHistoryNotFound() throws Exception {
        final String trunkRevision = addStubTest(trunkStore);

        final String unknownTestName = "fooTest";
        assertThat(unknownTestName).isNotEqualTo(STUB_TEST_NAME);

        assertThatThrownBy(() -> controller.getTestDefinitionHistory(trunkRevision, unknownTestName, 0, 100))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fooTest not found");
    }

    @Test
    public void testGetTestDefinitionForBranch() throws Exception {
        addStubTest(trunkStore);
        addStubTest(qaStore);
        addStubTest(prodStore);

        for (final Environment environment : Environment.values()) {
            final JsonView jsonView = controller.getTestDefinition(environment.getName(), STUB_TEST_NAME);
            final TestDefinition testDefinition = parsedRenderedJson(jsonView, TestDefinition.class);
            assertThat(testDefinition).isEqualTo(STUB_TEST_DEFINITION);
        }
    }

    @Test
    public void testGetTestDefinitionForRevision() throws Exception {
        final String trunkRevision = addStubTest(trunkStore);
        final String qaRevision = addStubTest(qaStore);
        final String prodRevision = addStubTest(prodStore);

        for (final String revision : Arrays.asList(trunkRevision, qaRevision, prodRevision)) {
            final JsonView jsonView = controller.getTestDefinition(revision, STUB_TEST_NAME);
            final TestDefinition testDefinition = parsedRenderedJson(jsonView, TestDefinition.class);
            assertThat(testDefinition).isEqualTo(STUB_TEST_DEFINITION);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetTestDefinitionHistoryForBranchAndRevision() throws Exception {
        final String trunkRevision1 = addStubTest(trunkStore);
        final String qaRevision1 = addStubTest(qaStore);
        final String prodRevision1 = addStubTest(prodStore);
        final TestDefinition anotherTest = new TestDefinition(STUB_TEST_DEFINITION);
        anotherTest.setSalt("different_salt");
        final String qaRevision2 = addTest(qaStore, STUB_TEST_NAME, anotherTest);

        {
            final JsonView jsonView = controller.getTestDefinitionHistory(Environment.WORKING.getName(), STUB_TEST_NAME, 0, 1);
            final List<Map> revisions = parsedRenderedJson(jsonView, List.class);
            assertThat(revisions)
                    .extracting(r -> r.get("revision"))
                    .containsExactly(trunkRevision1);
        }
        {
            final JsonView jsonView = controller.getTestDefinitionHistory(Environment.QA.getName(), STUB_TEST_NAME, 0, 2);
            final List<Map> revisions = parsedRenderedJson(jsonView, List.class);
            assertThat(revisions)
                    .extracting(r -> r.get("revision"))
                    .containsExactly(qaRevision2, qaRevision1);
        }
        {
            final JsonView jsonView = controller.getTestDefinitionHistory(Environment.PRODUCTION.getName(), STUB_TEST_NAME, 0, 3);
            final List<Map> revisions = parsedRenderedJson(jsonView, List.class);
            assertThat(revisions)
                    .extracting(r -> r.get("revision"))
                    .containsExactly(prodRevision1);
        }
        {
            final JsonView jsonView = controller.getTestDefinitionHistory(qaRevision1, STUB_TEST_NAME, 0, 2);
            final List<Map> revisions = parsedRenderedJson(jsonView, List.class);
            assertThat(revisions)
                    .extracting(r -> r.get("revision"))
                    .containsExactly(qaRevision1);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetTestDefinitionHistoryForBranch() throws Exception {
        trunkStore.addTestDefinition(
                ChangeMetadata.builder()
                        .setUsernameAndAuthor("testUser")
                        .setPassword("")
                        .setComment("testComment")
                        .setTimestamp(Instant.EPOCH)
                        .build(),
                STUB_TEST_NAME,
                STUB_TEST_DEFINITION,
                emptyMap()
        );

        final String trunkRevision = trunkStore.getLatestVersion();
        final JsonView testDefinitionHistory = controller.getTestDefinitionHistory(Environment.WORKING.getName(), STUB_TEST_NAME, 0, 100);
        assertThat(parsedRenderedJson(testDefinitionHistory, List.class))
                .hasOnlyOneElementSatisfying(o -> assertThat((Map) o)
                        .containsEntry("author", "testUser")
                        .containsEntry("message", "testComment")
                        .containsEntry("revision", trunkRevision));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetTestDefinitionHistoryForRevision() throws Exception {
        trunkStore.addTestDefinition(
                ChangeMetadata.builder()
                        .setUsernameAndAuthor("testUser")
                        .setPassword("")
                        .setComment("testComment")
                        .setTimestamp(Instant.EPOCH)
                        .build(),
                STUB_TEST_NAME,
                STUB_TEST_DEFINITION,
                emptyMap()
        );

        final String trunkRevision = trunkStore.getLatestVersion();
        final JsonView testDefinitionHistory = controller.getTestDefinitionHistory(trunkRevision, STUB_TEST_NAME, 0, 100);
        assertThat(parsedRenderedJson(testDefinitionHistory, List.class))
                .hasOnlyOneElementSatisfying(o -> assertThat((Map) o)
                        .containsEntry("author", "testUser")
                        .containsEntry("message", "testComment")
                        .containsEntry("revision", trunkRevision));
    }

    @Test
    public void testGetTestHistories() throws Exception {
        final String branch = "trunk";
        final int limit = 2;

        final String revision1 = addTest(trunkStore, "test1", STUB_TEST_DEFINITION);
        final String revision2 = addTest(trunkStore, "test2", STUB_TEST_DEFINITION);
        addTest(trunkStore, "test3", STUB_TEST_DEFINITION);

        final JsonView jsonView = controller.getTestHistories(branch, limit);
        final TestHistoriesResponseModel actual = parsedRenderedJson(jsonView, TestHistoriesResponseModel.class);

        assertThat(actual.getTotalNumberOfTests()).isEqualTo(3);
        assertThat(actual.getTestHistories()).hasSize(2);
        assertThat(actual.getTestHistories())
                .extracting(TestHistoriesResponseModel.TestHistory::getTestName)
                .containsExactly("test1", "test2");
        assertThat(actual.getTestHistories())
                .extracting(x -> x.getRevisions().get(0).getRevision())
                .containsExactly(revision1, revision2);
    }

    @Test
    public void testGetTestHistoriesThrowsNotFoundException() {
        final String branch = "wrongBranch";
        final int limit = 2;

        assertThatThrownBy(() -> controller.getTestHistories(branch, limit))
                .isInstanceOf(TestMatrixApiController.ResourceNotFoundException.class);
    }

    @Test
    public void testGetRevisionDetails() throws Exception {
        trunkStore.addTestDefinition(
                ChangeMetadata.builder()
                        .setUsernameAndAuthor("testUser")
                        .setPassword("")
                        .setComment("testComment")
                        .setTimestamp(Instant.EPOCH)
                        .build(),
                STUB_TEST_NAME,
                STUB_TEST_DEFINITION,
                emptyMap()
        );
        final String trunkRevision = trunkStore.getLatestVersion();

        final JsonView jsonView = controller.getRevisionDetails("trunk", trunkRevision);

        final Map actual = parsedRenderedJson(jsonView, Map.class);
        final Map expected = ImmutableMap.of(
                "revision", ImmutableMap.of(
                        "revision", trunkRevision,
                        "author", "testUser",
                        "date", "1970-01-01T00:00:00.000+0000",
                        "message", "testComment"
                ),
                "modifiedTests", singletonList(STUB_TEST_NAME)
        );

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testGetRevisionDetailsThrowsNotFoundException() {
        assertThatThrownBy(() -> controller.getRevisionDetails("new_branch", "123"))
                .isInstanceOf(TestMatrixApiController.ResourceNotFoundException.class);
        assertThatThrownBy(() -> controller.getRevisionDetails("trunk", "007"))
                .isInstanceOf(StoreException.class);
    }

    private static <T> T parsedRenderedJson(final JsonView jsonView, final Class<T> clazz) throws Exception {
        final String json = renderedJson(jsonView);
        return MAPPER.readValue(json, clazz);
    }

    private static String renderedJson(final JsonView jsonView) throws Exception {
        final HttpServletResponse responseMock = mock(HttpServletResponse.class);
        final StringWriter stringWriter = new StringWriter();
        when(responseMock.getWriter()).thenReturn(new PrintWriter(stringWriter));
        jsonView.render(null, null, responseMock);
        return stringWriter.toString();
    }

    /**
     * Add a stub test definition and return its revision id
     */

    private static String addStubTest(
            final ProctorStore store
    ) throws StoreException {
        return addTest(store, STUB_TEST_NAME, STUB_TEST_DEFINITION);
    }

    private static String addTest(
            final ProctorStore store,
            final String testName,
            final TestDefinition definition
    ) throws StoreException {
        if (store.getCurrentTestDefinition(testName) == null) {
            store.addTestDefinition(
                    ChangeMetadata.builder()
                            .setUsernameAndAuthor("testUser")
                            .setPassword("")
                            .setComment("testComment")
                            .build(),
                    testName,
                    definition,
                    emptyMap()
            );
        } else {
            final String previousRevision = store.getHistory(
                    testName, 0, 1
            ).get(0).getRevision();

            store.updateTestDefinition(
                    ChangeMetadata.builder()
                            .setUsernameAndAuthor("testUser")
                            .setPassword("")
                            .setComment("testComment")
                            .build(),
                    previousRevision,
                    testName,
                    definition,
                    emptyMap()
            );
        }

        return store.getLatestVersion();
    }
}
