package com.indeed.proctor.webapp.controllers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.StoreException;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Set;

import static com.indeed.proctor.webapp.controllers.TestSearchApiController.ProctorTest;
import static com.indeed.proctor.webapp.controllers.TestSearchApiController.Sort.FAVORITESFIRST;
import static com.indeed.proctor.webapp.controllers.TestSearchApiController.Sort.TESTNAME;
import static com.indeed.proctor.webapp.controllers.TestSearchApiController.Sort.UPDATEDDATE;
import static com.indeed.proctor.webapp.controllers.TestSearchApiController.getComparator;
import static com.indeed.proctor.webapp.controllers.TestSearchApiController.matchesAllIgnoreCase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestSearchApiControllerTest {
    private ProctorStore createMockProctorStore() throws StoreException {
        final TestDefinition definition = new TestDefinition();
        definition.setTestType(TestType.ANONYMOUS_USER);
        definition.setSalt("&traveltotokyotst");
        final TestMatrixDefinition testMatrixDefinition = new TestMatrixDefinition(
            ImmutableMap.of(
                "traveltotokyotst", definition
            )
        );
        final TestMatrixVersion testMatrixVersion = new TestMatrixVersion();
        testMatrixVersion.setTestMatrixDefinition(testMatrixDefinition);

        final ProctorStore store = mock(ProctorStore.class);
        when(store.getCurrentTestMatrix()).thenReturn(testMatrixVersion);
        return store;
    }

    @Test
    public void testSearchEndpoint() throws Exception {
        final ProctorStore store = createMockProctorStore();
        final TestSearchApiController testSearchApiController = new TestSearchApiController(null, store, store, store);
        final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(testSearchApiController).build();
        mockMvc.perform(
            MockMvcRequestBuilders.get("/proctor/matrix/tests")
                        .param("q", "tokyo tst")
        )
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.content().string(CoreMatchers.containsString("traveltotokyotst")));
    }

    @Test
    public void testComparator() {
        final ProctorTest a20 =
                new ProctorTest("a", new TestDefinition(), 20);
        final ProctorTest b10 =
                new ProctorTest("b", new TestDefinition(), 10);
        final ProctorTest c30 =
                new ProctorTest("c", new TestDefinition(), 30);

        final Set<String> favoriteTests = ImmutableSet.of("b");
        assertThat(
                Ordering.from(getComparator(TESTNAME, favoriteTests))
                        .sortedCopy(Arrays.asList(b10, c30, a20))
        ).containsExactly(a20, b10, c30);

        assertThat(
                Ordering.from(getComparator(FAVORITESFIRST, favoriteTests))
                        .sortedCopy(Arrays.asList(b10, c30, a20))
        ).containsExactly(b10, a20, c30);

        assertThat(
                Ordering.from(getComparator(UPDATEDDATE, favoriteTests))
                        .sortedCopy(Arrays.asList(b10, c30, a20))
        ).containsExactly(c30, a20, b10);

    }

    @Test
    public void testMatchFilterTypeAll() {
        final TestDefinition definition = new TestDefinition();
        definition.setTestType(TestType.ANONYMOUS_USER);
        definition.setSalt("&traveltotokyotst");

        assertThat(
                matchesAllIgnoreCase(
                        "traveltotokyotst",
                        definition,
                        TestSearchApiController.FilterType.ALL,
                        Arrays.asList("Travel", "Tokyo", "User")
                )
        ).isTrue();

        assertThat(
                matchesAllIgnoreCase(
                        "traveltotokyotst",
                        definition,
                        TestSearchApiController.FilterType.ALL,
                        Arrays.asList("Travel", "Kyoto")
                )
        ).isFalse();
    }
}