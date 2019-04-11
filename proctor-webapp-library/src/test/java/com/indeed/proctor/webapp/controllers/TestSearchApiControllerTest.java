package com.indeed.proctor.webapp.controllers;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.indeed.proctor.common.model.TestDefinition;
import org.junit.Test;

import java.util.Arrays;
import java.util.Set;

import static com.indeed.proctor.webapp.controllers.TestSearchApiController.ProctorTest;
import static com.indeed.proctor.webapp.controllers.TestSearchApiController.Sort.FAVORITESFIRST;
import static com.indeed.proctor.webapp.controllers.TestSearchApiController.Sort.TESTNAME;
import static com.indeed.proctor.webapp.controllers.TestSearchApiController.Sort.UPDATEDDATE;
import static com.indeed.proctor.webapp.controllers.TestSearchApiController.getComparator;
import static org.assertj.core.api.Assertions.assertThat;

public class TestSearchApiControllerTest {
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
}