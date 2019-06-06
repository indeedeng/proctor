package com.indeed.proctor.webapp.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.store.utils.test.InMemoryProctorStore;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.model.api.TestHistoriesResponseModel;
import com.indeed.proctor.webapp.model.WebappConfiguration;
import com.indeed.proctor.webapp.views.JsonView;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.indeed.proctor.store.utils.test.InMemoryProctorStore.REVISION_PREFIX;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class TestTestMatrixApiController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestMatrixApiController controller;
    private InMemoryProctorStore trunkStore;
    private InMemoryProctorStore qaStore;
    private InMemoryProctorStore prodStore;

    private static final String TEST_NAME = "dummyTest";

    @Before
    public void setUp() {
        final WebappConfiguration configuration = new WebappConfiguration(false, false, 1000, 10);
        trunkStore = spy(new InMemoryProctorStore());
        qaStore = spy(new InMemoryProctorStore());
        prodStore = spy(new InMemoryProctorStore());
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
        addStubTest(trunkStore, "foo0");
        final JsonView jsonView = controller.getTestMatrix(Environment.WORKING.getName());
        final String json = renderedJson(jsonView);
        assertThat(json).contains("\"version\" : \"foo0\"");
    }

    @Test
    public void testGetTestMatrixForRevision() throws Exception {
        final String[] trunkVersions = new String[]{"111", "222", "333"};
        final String[] qaVersions = new String[]{"123", "234", "345"};
        final String[] prodVersions = new String[]{"146", "257", "369"};

        addStubTest(trunkStore, trunkVersions);
        addStubTest(qaStore, qaVersions);
        addStubTest(prodStore, prodVersions);

        final List<String> versions = ImmutableList.<String>builder()
                .add(trunkVersions)
                .add(qaVersions)
                .add(prodVersions)
                .build();

        for (final String version : versions) {
            final JsonView jsonView = controller.getTestMatrix(constructRevision(version));
            final TestMatrixVersion testMatrixVersion = parsedRenderedJson(jsonView, TestMatrixVersion.class);

            assertThat(testMatrixVersion.getVersion())
                    .isEqualTo(version);
        }
    }

    @Test
    public void testGetTestMatrixHistoryForRevisionEmpty() throws Exception {
        final String[] trunkVersions = new String[]{"111", "222", "333"};
        final String[] qaVersions = new String[]{"123", "234", "345"};
        final String[] prodVersions = new String[]{"146", "257", "369"};

        addStubTest(trunkStore, trunkVersions);
        addStubTest(qaStore, qaVersions);
        addStubTest(prodStore, prodVersions);

        assertThatThrownBy(() -> controller.getTestMatrixHistory("1234", 1, 100))
                .isInstanceOf(TestMatrixApiController.ResourceNotFoundException.class);
    }

    @Test
    public void testGetTestMatrixHistoryForBranchEmpty() throws Exception {
        final String[] trunkVersions = new String[]{"111", "222", "333"};
        final String[] qaVersions = new String[]{"123", "234", "345"};

        addStubTest(trunkStore, trunkVersions);
        addStubTest(qaStore, qaVersions);

        final JsonView jsonView = controller.getTestMatrixHistory(Environment.PRODUCTION.getName(), 1, 100);
        assertThat(parsedRenderedJson(jsonView, List.class)).isEqualTo(emptyList());
    }

    @Test
    public void testGetTestDefinitionNotFound() throws Exception {
        final String[] trunkVersions = new String[]{"111", "222", "333"};
        final String[] qaVersions = new String[]{"123", "234", "345"};
        final String[] prodVersions = new String[]{"146", "257", "369"};

        addStubTest(trunkStore, trunkVersions);
        addStubTest(qaStore, qaVersions);
        addStubTest(prodStore, prodVersions);

        assertThatThrownBy(() -> controller.getTestDefinition("123", "fooTest"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("fooTest not found");
    }

    @Test
    public void testGetTestDefinitionHistoryNotFound() throws Exception {
        final String[] trunkVersions = new String[]{"111", "222", "333"};
        final String[] qaVersions = new String[]{"123", "234", "345"};
        final String[] prodVersions = new String[]{"146", "257", "369"};

        addStubTest(trunkStore, trunkVersions);
        addStubTest(qaStore, qaVersions);
        addStubTest(prodStore, prodVersions);

        assertThatThrownBy(() -> controller.getTestDefinitionHistory("123", "fooTest", 1, 100))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fooTest not found");
    }

    @Test
    public void testGetTestDefinitionForBranch() throws Exception {
        final String[] trunkVersions = new String[]{"111", "222", "333"};
        final String[] qaVersions = new String[]{"123", "234", "345"};
        final String[] prodVersions = new String[]{"146", "257", "369"};

        addStubTest(trunkStore, trunkVersions);
        addStubTest(qaStore, qaVersions);
        addStubTest(prodStore, prodVersions);

        {
            final JsonView jsonView = controller.getTestDefinition(Environment.WORKING.getName(), TEST_NAME);
            final TestDefinition testDefinition = parsedRenderedJson(jsonView, TestDefinition.class);
            assertThat(testDefinition.getVersion())
                    .isEqualTo("333");
        }
        {
            final JsonView jsonView = controller.getTestDefinition(Environment.QA.getName(), TEST_NAME);
            final TestDefinition testDefinition = parsedRenderedJson(jsonView, TestDefinition.class);
            assertThat(testDefinition.getVersion())
                    .isEqualTo("345");
        }
        {
            final JsonView jsonView = controller.getTestDefinition(Environment.PRODUCTION.getName(), TEST_NAME);
            final TestDefinition testDefinition = parsedRenderedJson(jsonView, TestDefinition.class);
            assertThat(testDefinition.getVersion())
                    .isEqualTo("369");
        }
    }

    @Test
    public void testGetTestDefinitionForRevision() throws Exception {
        final String[] trunkVersions = new String[]{"111", "222", "333"};
        final String[] qaVersions = new String[]{"123", "234", "345"};
        final String[] prodVersions = new String[]{"146", "257", "369"};

        addStubTest(trunkStore, trunkVersions);
        addStubTest(qaStore, qaVersions);
        addStubTest(prodStore, prodVersions);

        final List<String> versions = ImmutableList.<String>builder()
                .add(trunkVersions)
                .add(qaVersions)
                .add(prodVersions)
                .build();

        for (final String version : versions) {
            final JsonView jsonView = controller.getTestDefinition(constructRevision(version), TEST_NAME);
            final TestDefinition testDefinition = parsedRenderedJson(jsonView, TestDefinition.class);
            assertThat(testDefinition.getVersion())
                    .isEqualTo(version);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetTestDefinitionHistoryForBranchAndRevision() throws Exception {
        final String[] trunkVersions = new String[]{"111", "222", "333"};
        final String[] qaVersions = new String[]{"123", "234", "345"};
        final String[] prodVersions = new String[]{"146", "257", "369"};

        addStubTest(trunkStore, trunkVersions);
        addStubTest(qaStore, qaVersions);
        addStubTest(prodStore, prodVersions);

        {
            final JsonView jsonView = controller.getTestDefinitionHistory(Environment.WORKING.getName(), TEST_NAME, 1, 1);
            final List<Map> revisions = parsedRenderedJson(jsonView, List.class);
            assertThat(revisions)
                    .extracting(r -> r.get("revision"))
                    .containsExactly(constructRevision("222"));
        }
        {
            final JsonView jsonView = controller.getTestDefinitionHistory(Environment.QA.getName(), TEST_NAME, 0, 2);
            final List<Map> revisions = parsedRenderedJson(jsonView, List.class);
            assertThat(revisions)
                    .extracting(r -> r.get("revision"))
                    .containsExactly(constructRevision("345"), constructRevision("234"));
        }
        {
            final JsonView jsonView = controller.getTestDefinitionHistory(Environment.PRODUCTION.getName(), TEST_NAME, 2, 3);
            final List<Map> revisions = parsedRenderedJson(jsonView, List.class);
            assertThat(revisions)
                    .extracting(r -> r.get("revision"))
                    .containsExactly(constructRevision("146"));
        }
        {
            final JsonView jsonView = controller.getTestDefinitionHistory(constructRevision("234"), TEST_NAME, 0, 2);
            final List<Map> revisions = parsedRenderedJson(jsonView, List.class);
            assertThat(revisions)
                    .extracting(r -> r.get("revision"))
                    .containsExactly(constructRevision("234"), constructRevision("123"));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetTestDefinitionHistoryForBranchWithMock() throws Exception {
        final String testName = "fooTest";
        final Revision expected = new Revision("r1", "a1", new Date(), "m1");
        when(qaStore.getHistory(testName, 1, 100)).thenReturn(singletonList(expected));
        final JsonView testDefinitionHistory = controller.getTestDefinitionHistory(Environment.QA.getName(), testName, 1, 100);
        assertThat(parsedRenderedJson(testDefinitionHistory, List.class))
                .hasOnlyOneElementSatisfying(o -> assertThat((Map) o)
                        .containsEntry("author", expected.getAuthor())
                        .containsEntry("message", expected.getMessage())
                        .containsEntry("revision", expected.getRevision()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetTestDefinitionHistoryForRevisionWithMock() throws Exception {
        final String testName = "fooTest";
        final String revision = "1234";
        final Revision expected = new Revision(revision, "a1", new Date(), "m1");
        when(prodStore.getAllHistories()).thenReturn(ImmutableMap.of(testName, singletonList(expected)));
        when(prodStore.getHistory(testName, revision, 1, 100)).thenReturn(singletonList(expected));
        final JsonView testDefinitionHistory = controller.getTestDefinitionHistory(revision, testName, 1, 100);
        assertThat(parsedRenderedJson(testDefinitionHistory, List.class))
                .hasOnlyOneElementSatisfying(o -> assertThat((Map) o)
                        .containsEntry("author", expected.getAuthor())
                        .containsEntry("message", expected.getMessage())
                        .containsEntry("revision", expected.getRevision()));
    }


    @Test
    public void testGetTestHistories() throws Exception {
        final String branch = "trunk";
        final int limit = 2;

        final Revision revision1 = new Revision("1234", "user", new Date(), "message");
        final Revision revision2 = new Revision("2345", "user", new Date(), "message");
        final Revision revision3 = new Revision("3456", "user", new Date(), "message");

        final Map<String, List<Revision>> histories = ImmutableMap.of(
                "test1", ImmutableList.of(revision1),
                "test2", ImmutableList.of(revision2),
                "test3", ImmutableList.of(revision3)
        );
        when(trunkStore.getAllHistories()).thenReturn(histories);

        final TestHistoriesResponseModel expected = new TestHistoriesResponseModel(
                histories.size(),
                ImmutableList.of(
                        new TestHistoriesResponseModel.TestHistory("test1", ImmutableList.of(revision1)),
                        new TestHistoriesResponseModel.TestHistory("test2", ImmutableList.of(revision2))
                )
        );

        final JsonView jsonView = controller.getTestHistories(branch, limit);
        final TestHistoriesResponseModel actual = parsedRenderedJson(jsonView, TestHistoriesResponseModel.class);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testGetTestHistoriesThrowsNotFoundException() {
        final String branch = "wrongBranch";
        final int limit = 2;

        assertThatThrownBy(() -> controller.getTestHistories(branch, limit))
                .isInstanceOf(TestMatrixApiController.ResourceNotFoundException.class);
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

    private static void addStubTest(final InMemoryProctorStore store, final String... versions) throws StoreException.TestUpdateException {
        String lastVersion = null;
        for (final String version : versions) {
            final TestDefinition definition = new TestDefinition();
            definition.setVersion(version);
            if (lastVersion == null) {
                store.addTestDefinition("testUser", "testPassword", TEST_NAME, definition, null, "testComment");
            } else {
                store.updateTestDefinition("testUser", "testPassword", lastVersion, TEST_NAME, definition, null, "testComment");
            }
            lastVersion = version;
        }
    }

    private static String constructRevision(final String version) {
        return REVISION_PREFIX + version;
    }
}
