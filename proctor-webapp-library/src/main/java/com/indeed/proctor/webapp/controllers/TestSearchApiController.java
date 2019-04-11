package com.indeed.proctor.webapp.controllers;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixDefinition;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.model.WebappConfiguration;
import com.indeed.proctor.webapp.util.TestSearchUtil;
import com.indeed.proctor.webapp.views.JsonView;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toList;

@Controller
@RequestMapping(value = {"/proctor/matrix/tests", "/matrix/tests"})
public class TestSearchApiController extends AbstractController {
    @Autowired
    public TestSearchApiController(
            final WebappConfiguration configuration,
            @Qualifier("trunk") final ProctorStore trunkStore,
            @Qualifier("qa") final ProctorStore qaStore,
            @Qualifier("production") final ProctorStore productionStore
    ) {
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

    @VisibleForTesting
    enum Sort {
        FAVORITESFIRST,
        TESTNAME,
        UPDATEDDATE,
    }

    private static class TestsResponse {
        private final List<ProctorTest> tests;
        private final int totalTestCount;
        private final int matchingTestCount;

        TestsResponse(final List<ProctorTest> tests, final int totalTestCount, final int matchingTestCount) {
            this.tests = tests;
            this.totalTestCount = totalTestCount;
            this.matchingTestCount = matchingTestCount;
        }

        public List<ProctorTest> getTests() {
            return tests;
        }

        public int getTotalTestCount() {
            return totalTestCount;
        }

        public int getMatchingTestCount() {
            return matchingTestCount;
        }
    }

    @VisibleForTesting
    static class ProctorTest {
        private final String name;
        private final TestDefinition definition;
        private final long lastUpdated;

        ProctorTest(final String name, final TestDefinition definition, final long lastUpdated) {
            this.name = name;
            this.definition = definition;
            this.lastUpdated = lastUpdated;
        }

        public String getName() {
            return name;
        }

        public TestDefinition getDefinition() {
            return definition;
        }

        public long getLastUpdated() {
            return lastUpdated;
        }
    }

    private static boolean matchFilterType(
            final String testName,
            final TestDefinition definition,
            final FilterType type,
            final String query
    ) {
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
    static Comparator<ProctorTest> getComparator(
            final Sort sort,
            final Set<String> favoriteTestNames
    ) {
        switch (sort) {
            case TESTNAME:
                return Comparator.comparing(ProctorTest::getName, String::compareToIgnoreCase);
            case FAVORITESFIRST:
                return Comparator.comparing(
                        ProctorTest::getName,
                        TestSearchUtil.givenSetFirstComparator(favoriteTestNames)
                ).thenComparing(
                        ProctorTest::getName,
                        String::compareToIgnoreCase
                );
            case UPDATEDDATE:
                return Comparator.comparing(
                        ProctorTest::getLastUpdated
                ).reversed().thenComparing(
                        ProctorTest::getName,
                        String::compareToIgnoreCase
                );
            default:
                throw new IllegalArgumentException();
        }
    }

    private static List<ProctorTest> toProctorTests(
            final Map<String, TestDefinition> matrix,
            final ProctorStore store
    ) throws StoreException {
        final List<ProctorTest> proctorTests = Lists.newArrayListWithExpectedSize(matrix.size());
        for (final Map.Entry<String, TestDefinition> e : matrix.entrySet()) {
            final List<Revision> revisions = store.getHistory(e.getKey(), 0, 1);
            final long updatedTime;
            if (CollectionUtils.isEmpty(revisions)) {
                updatedTime = 0;
            } else {
                updatedTime = revisions.get(0).getDate().getTime();
            }
            proctorTests.add(new ProctorTest(e.getKey(), e.getValue(), updatedTime));
        }
        return proctorTests;
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
            @CookieValue(value = "FavoriteTests", defaultValue = "") final String favoriteTestsRaw
    ) throws StoreException {
        final Set<String> favoriteTestNames = Sets.newHashSet(Splitter.on(",").split(favoriteTestsRaw));

        final Environment environment = determineEnvironmentFromParameter(branch);
        final TestMatrixDefinition testMatrixDefinition = getCurrentMatrix(environment).getTestMatrixDefinition();
        final Map<String, TestDefinition> allTestMatrix = testMatrixDefinition != null
                ? testMatrixDefinition.getTests()
                : Collections.emptyMap();

        final Map<String, TestDefinition> matchingTestMatrix = Maps.filterEntries(allTestMatrix,
                e -> e != null && matchFilterType(e.getKey(), e.getValue(), filterType, q)
                        && matchFilterActive(e.getValue().getAllocations(), filterActive)
        );

        final List<ProctorTest> searchResult =
                toProctorTests(matchingTestMatrix, determineStoreFromEnvironment(environment))
                        .stream()
                        .sorted(getComparator(sort, favoriteTestNames))
                        .limit(limit)
                        .collect(toList());

        return new JsonView(new TestsResponse(searchResult, allTestMatrix.size(), searchResult.size()));
    }
}
