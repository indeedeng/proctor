package com.indeed.proctor.webapp.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.model.WebappConfiguration;
import com.indeed.proctor.webapp.testutil.InMemoryProctorStore;
import com.indeed.proctor.webapp.views.JsonView;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.indeed.proctor.webapp.testutil.InMemoryProctorStore.REVISION_PREFIX;
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
        final String revision = "1234";
        addStubTest(prodStore, revision);
        assertThat(controller.getTestMatrix(REVISION_PREFIX + revision)).isNotNull();
    }

    @Test
    public void testGetTestMatrixHistoryForRevisionEmpty() throws Exception {
        assertThatThrownBy(() -> controller.getTestMatrixHistory("1234", 1, 100))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Branch 1234 not correct");
    }

    @Test
    public void testGetTestMatrixHistoryForBranchEmpty() throws Exception {
        final JsonView jsonView = controller.getTestMatrixHistory(Environment.PRODUCTION.getName(), 1, 100);
        assertThat(parsedRenderedJson(jsonView, List.class)).isEqualTo(emptyList());
    }

    @Test
    public void testGetTestDefinitionNotFound() throws Exception {
        assertThatThrownBy(() -> controller.getTestDefinition("1234", "fooTest"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("fooTest not found");
    }

    @Test
    public void testGetTestDefinitionHistoryNotFound() throws Exception {
        assertThatThrownBy(() -> controller.getTestDefinitionHistory("1234", "fooTest", 1, 100))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("fooTest not found");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testGetTestDefinitionHistoryForBranch() throws Exception {
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
    public void testGetTestDefinitionHistoryForRevision() throws Exception {
        String testName = "fooTest";
        String revision = "1234";
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

    private static void addStubTest(final InMemoryProctorStore prodStore, final String revision) throws StoreException.TestUpdateException {
        final TestDefinition definition = new TestDefinition();
        definition.setVersion(revision);
        prodStore.addTestDefinition("testUser", "testPassword", "testAuthor", definition, null, "testComment");
    }

}
