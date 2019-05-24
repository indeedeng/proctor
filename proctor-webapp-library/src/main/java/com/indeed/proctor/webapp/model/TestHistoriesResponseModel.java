package com.indeed.proctor.webapp.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.indeed.proctor.store.Revision;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestHistoriesResponseModel {
    private final int totalNumberOfTests;
    private final List<TestHistory> testHistories;

    @JsonCreator
    public TestHistoriesResponseModel(
            @JsonProperty("totalNumberOfTests") final int totalNumberOfTests,
            @JsonProperty("testHistories") final List<TestHistory> testHistories
    ) {
        this.totalNumberOfTests = totalNumberOfTests;
        this.testHistories = testHistories;
    }

    public TestHistoriesResponseModel(final Map<String, List<Revision>> histories, final int limit) {
        this.totalNumberOfTests = histories.size();
        Stream<Map.Entry<String, List<Revision>>> stream = histories.entrySet().stream().sorted(
                Map.Entry.comparingByKey()
        );
        if (limit >= 0) {
            stream = stream.limit(limit);
        }
        testHistories = stream.map(entry -> new TestHistory(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    @ApiModelProperty("the total number of tests")
    public int getTotalNumberOfTests() {
        return totalNumberOfTests;
    }

    @ApiModelProperty("the list of histories for each test")
    public List<TestHistory> getTestHistories() {
        return testHistories;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TestHistoriesResponseModel)) {
            return false;
        }
        final TestHistoriesResponseModel that = (TestHistoriesResponseModel) o;
        return getTotalNumberOfTests() == that.getTotalNumberOfTests() &&
                Objects.equal(getTestHistories(), that.getTestHistories());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getTotalNumberOfTests(), getTestHistories());
    }

    @VisibleForTesting
    public static class TestHistory {
        private final String testName;
        private final List<Revision> revisions;

        @VisibleForTesting
        @JsonCreator
        public TestHistory(
                @JsonProperty("testName") final String testName,
                @JsonProperty("revisions") final List<Revision> revisions
        ) {
            this.testName = testName;
            this.revisions = revisions;
        }

        @ApiModelProperty("the test name")
        public String getTestName() {
            return this.testName;
        }

        @ApiModelProperty("the change history of the test")
        public List<Revision> getRevisions() {
            return revisions;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof TestHistory)) {
                return false;
            }
            final TestHistory that = (TestHistory) o;
            return Objects.equal(getTestName(), that.getTestName()) &&
                    Objects.equal(getRevisions(), that.getRevisions());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(getTestName(), getRevisions());
        }
    }
}
