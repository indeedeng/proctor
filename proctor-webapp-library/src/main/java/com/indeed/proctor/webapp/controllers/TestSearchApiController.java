package com.indeed.proctor.webapp.controllers;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.model.WebappConfiguration;
import com.indeed.proctor.webapp.util.TestSearchUtil;
import com.indeed.proctor.webapp.views.JsonView;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping(value = {"/proctor/matrix/tests", "/matrix/tests"})
public class TestSearchApiController extends AbstractController {
    @Autowired
    public TestSearchApiController(
            final WebappConfiguration configuration,
            @Qualifier("trunk") final ProctorStore trunkStore,
            @Qualifier("qa") final ProctorStore qaStore,
            @Qualifier("production") final ProctorStore productionStore) {
        super(configuration, trunkStore, qaStore, productionStore);
    }

    private enum FilterType {
        ALL,
        TESTNAME,
        DESCRIPTION,
        RULE,
        BUCKET,
        BUCKETDESCRIPTION,
    }

    private enum FilterActive {
        ALL,
        ACTIVE,
        INACTIVE,
    }

    private enum Sort {
        FAVORITESFIRST,
        TESTNAME,
        UPDATEDDATE,
    }

    private static class TestsResponse {
        private final List<TestNameAndDefinition> tests;
        private final int totalTestCount;

        TestsResponse(final List<TestNameAndDefinition> tests, final int totalTestCount) {
            this.tests = tests;
            this.totalTestCount = totalTestCount;
        }

        public List<TestNameAndDefinition> getTests() {
            return tests;
        }

        public int getTotalTestCount() {
            return totalTestCount;
        }
    }

    private static class TestNameAndDefinition {
        private final String name;
        private final TestDefinition definition;

        TestNameAndDefinition(final String name, final TestDefinition definition) {
            this.name = name;
            this.definition = definition;
        }

        public String getName() {
            return name;
        }

        public TestDefinition getDefinition() {
            return definition;
        }
    }

    private static boolean matchFilterType(
            final String testName,
            final TestDefinition definition,
            final FilterType type,
            final String query) {
        if (query.isEmpty()) {
            return true;
        }
        final String lowerQuery = query.toLowerCase();
        switch (type) {
            case TESTNAME:
                return TestSearchUtil.matchTestName(testName, lowerQuery);
            case DESCRIPTION:
                return TestSearchUtil.matchDescription(definition, lowerQuery);
            case RULE:
                return TestSearchUtil.matchRule(definition, lowerQuery);
            case BUCKET:
                return TestSearchUtil.matchBucket(definition, lowerQuery);
            case BUCKETDESCRIPTION:
                return TestSearchUtil.matchBucketDescription(definition, lowerQuery);
            case ALL:
                return TestSearchUtil.matchAll(testName, definition, lowerQuery);
            default:
                throw new IllegalArgumentException("unknown filter type: " + type);
        }
    }

    private static boolean matchFilterActive(final List<Allocation> allocations, final FilterActive filterActive) {
        switch (filterActive) {
            case ALL:
                return true;
            case ACTIVE:
                return TestSearchUtil.matchActiveAllocation(allocations);
            case INACTIVE:
                return !TestSearchUtil.matchActiveAllocation(allocations);
            default:
                throw new IllegalArgumentException("unknown filter type: " + filterActive);
        }
    }

    @VisibleForTesting
    static Comparator<Map.Entry<String, TestDefinition>> getComparator(
            final Sort sort,
            final Set<String> favoriteTestNames) {
        switch (sort) {
            case TESTNAME:
                return Comparator.comparing(Map.Entry::getKey, String::compareToIgnoreCase);
            case FAVORITESFIRST:
                return Comparator.comparing(
                        Map.Entry::getKey,
                        TestSearchUtil.givenSetFirstComparator(favoriteTestNames)
                                .thenComparing(String::compareToIgnoreCase));
            case UPDATEDDATE:
                throw new NotImplementedException("Updated date not implemented");
            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * API for proctor tests with filtering functionality
     *
     * @param branch           environment
     * @param limit            number of tests to return
     * @param q                query string to search
     * @param filterType       {@link FilterType}
     * @param filterActive     {@link FilterActive}
     * @param sort             {@link Sort}
     * @param favoriteTestsRaw comma-separated favorite tests saved in cookie
     * @return JSON of TestsResponse
     */
    @ApiOperation(value = "Proctor tests with filters", response = List.class)
    @GetMapping
    public JsonView viewTestNames(
            @RequestParam(defaultValue = "trunk") final String branch,
            @RequestParam(defaultValue = "100") final int limit,
            @RequestParam(defaultValue = "") final String q,
            @RequestParam(defaultValue = "ALL") final FilterType filterType,
            @RequestParam(defaultValue = "ALL") final FilterActive filterActive,
            @RequestParam(defaultValue = "FAVORITESFIRST") final Sort sort,
            @CookieValue(value = "FavoriteTests", defaultValue = "") final String favoriteTestsRaw) {
        final Set<String> favoriteTestNames = Sets.newHashSet(Splitter.on(",").split(favoriteTestsRaw));

        final Environment environment = determineEnvironmentFromParameter(branch);
        final Map<String, TestDefinition> tests = getCurrentMatrix(environment).getTestMatrixDefinition().getTests();
        final List<TestNameAndDefinition> result = tests.entrySet().stream()
                .filter(entry -> matchFilterType(entry.getKey(), entry.getValue(), filterType, q)
                        && matchFilterActive(entry.getValue().getAllocations(), filterActive))
                .sorted(getComparator(sort, favoriteTestNames))
                .limit(limit)
                .map(entry -> new TestNameAndDefinition(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        return new JsonView(new TestsResponse(result, tests.size()));
    }
}
