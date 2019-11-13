package com.indeed.proctor.store.utils.test;

import com.google.common.collect.Maps;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.ChangeMetadata;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.RevisionDetails;
import com.indeed.proctor.store.StoreException;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

/**
 * This class is an in-memory implementation of ProctorStore.
 * It's for testing purpose and not production ready.
 * <p>
 * These are design notes of this in-memory store.
 * <p>
 * Initially it stores following data
 * * a single dummy revision in matrix history.
 * * no test definition.
 * <p>
 * the dummy revision is required for the following purpose.
 * * getLatestVersion() can avoid an exception even in initialized state.
 * * getCurrent*() can avoid an exception as they relies on getLatestVersion()
 * <p>
 * With default constructor,
 * Revision id is a sequential number.
 * First revision id is "0" and first update will create a revision "1".
 * If you want to control revision id generation
 * (e.g. if you want to make sure it's unique in more than single stores),
 * create and pass to the constructor your custom id generator.
 * <p>
 * Internally, it stores single data structures to support all queries.
 * * globalHistory: a linked list of update records order by recency
 */
public class InMemoryProctorStore implements ProctorStore {

    /**
     * a list of all updates ordered by recency (the first is the latest)
     */
    private final List<UpdateRecord> globalHistory = new LinkedList<>();

    /**
     * Generator of revision id. It's called every time new revision is added.
     */
    private final Supplier<String> revisionIdGenerator;

    /**
     * Change metadata for the first revision.
     */
    private static final ChangeMetadata INITIAL_CHANGE_METADATA = ChangeMetadata.builder()
            .setUsernameAndAuthor("proctor")
            .setPassword("password")
            .setTimestamp(Instant.EPOCH)
            .setComment("initialize in-memory store")
            .build();

    /**
     * @return default revision id generator that simulates autoincrement id in DB
     */
    public static Supplier<String> autoincrementRevisionIdGenerator() {
        final AtomicLong atomicLong = new AtomicLong(0);
        return () -> Long.toString(atomicLong.getAndIncrement());
    }

    public InMemoryProctorStore() {
        this(autoincrementRevisionIdGenerator());
    }

    public InMemoryProctorStore(final Supplier<String> revisionIdGenerator) {
        this.revisionIdGenerator = revisionIdGenerator;
        insertNewRecord(INITIAL_CHANGE_METADATA, null);
    }

    @Override
    public String getName() {
        return InMemoryProctorStore.class.getSimpleName();
    }

    // --- reader ---

    @Nonnull
    @Override
    public synchronized String getLatestVersion() {
        return globalHistory.get(0).revision.getRevision();
    }

    @Override
    public synchronized TestMatrixVersion getTestMatrix(final String revisionId) throws StoreException {
        final UpdateRecord startRecord = getUpdateRecord(revisionId);

        final List<UpdateRecord> history = dropWhile(globalHistory, r -> !r.equals(startRecord));
        final Map<String, TestDefinition> allTests = new HashMap<>();
        for (final UpdateRecord record : history) {
            final TestEdit testEdit = record.testEdit;
            if (testEdit != null && !allTests.containsKey(testEdit.testName)) {
                allTests.put(testEdit.testName, testEdit.definition);
            }
        }

        final Revision revision = startRecord.revision;
        return new TestMatrixVersion(
                new TestMatrixDefinition(
                        Maps.filterValues(allTests, Objects::nonNull) // remove deleted tests
                ),
                revision.getDate(),
                revision.getRevision(),
                revision.getMessage(),
                revision.getAuthor()
        );
    }

    @CheckForNull
    @Override
    public synchronized TestDefinition getTestDefinition(
            final String testName,
            final String revisionId
    ) throws StoreException {
        final UpdateRecord startRecord = getUpdateRecord(revisionId);

        return dropWhile(globalHistory, r -> !r.equals(startRecord))
                .stream()
                .filter(r -> r.modifiedTests().contains(testName))
                .findFirst()
                .map(r -> Objects.requireNonNull(r.testEdit))
                .map(t -> t.definition)
                .orElse(null);
    }

    @Nonnull
    @Override
    public synchronized List<Revision> getMatrixHistory(final int start, final int limit) {
        return globalHistory.stream()
                .map(r -> r.revision)
                .skip(start)
                .limit(limit)
                .collect(toList());
    }

    @Nonnull
    @Override
    public synchronized List<Revision> getMatrixHistory(
            final Instant sinceInclusive,
            final Instant untilExclusive
    ) {
        return globalHistory.stream()
                .map(r -> r.revision)
                .filter(r -> !r.getDate().toInstant().isBefore(sinceInclusive)
                        && r.getDate().toInstant().isBefore(untilExclusive))
                .collect(toList());
    }

    @Nonnull
    @Override
    public synchronized List<Revision> getHistory(
            final String testName,
            final String revisionId,
            final int start,
            final int limit
    ) throws StoreException {
        final UpdateRecord startRecord = getUpdateRecord(revisionId);

        return dropWhile(globalHistory, r -> !r.equals(startRecord))
                .stream()
                .filter(r -> r.modifiedTests().contains(testName))
                .map(r -> r.revision)
                .skip(start)
                .limit(limit)
                .collect(toList());
    }

    @CheckForNull
    @Override
    public synchronized RevisionDetails getRevisionDetails(final String revisionId) throws StoreException {
        return getUpdateRecord(revisionId).toRevisionDetails();
    }

    @Nonnull
    @Override
    public synchronized Map<String, List<Revision>> getAllHistories() {
        return globalHistory.stream()
                .filter(r -> r.testEdit != null)
                .collect(groupingBy(
                        r -> r.testEdit.testName,
                        mapping(r -> r.revision, toList())
                ));
    }

    @Override
    public synchronized TestMatrixVersion getCurrentTestMatrix() throws StoreException {
        return getTestMatrix(getLatestVersion());
    }

    @CheckForNull
    @Override
    public synchronized TestDefinition getCurrentTestDefinition(final String testName) throws StoreException {
        return getTestDefinition(testName, getLatestVersion());
    }

    @Nonnull
    @Override
    public synchronized List<Revision> getHistory(
            final String testName,
            final int start,
            final int limit
    ) throws StoreException {
        return getHistory(testName, getLatestVersion(), start, limit);
    }

    // --- writer ---

    @Override
    public synchronized void addTestDefinition(
            final ChangeMetadata changeMetadata,
            final String testName,
            final TestDefinition testDefinition,
            final Map<String, String> metadata
    ) throws StoreException.TestUpdateException {
        if (getLatestUpdate(testName).isPresent()) {
            throw new StoreException.TestUpdateException(testName + " has been added before");
        }

        insertNewRecord(changeMetadata, new TestEdit(testName, testDefinition));
    }

    @Override
    public synchronized void updateTestDefinition(
            final ChangeMetadata changeMetadata,
            final String previousVersion,
            final String testName,
            final TestDefinition testDefinition,
            final Map<String, String> metadata
    ) throws StoreException.TestUpdateException {
        final UpdateRecord lastUpdate = getLatestUpdate(testName)
                .orElseThrow(() -> new StoreException.TestUpdateException(testName + " not yet added"));

        if (lastUpdate.testEdit.definition == null) {
            throw new StoreException.TestUpdateException(testName + " already deleted");
        }

        if (!previousVersion.equals(lastUpdate.revision.getRevision())) {
            throw new StoreException.TestUpdateException(
                    "Expected previous version is "
                            + lastUpdate.revision.getRevision()
                            + " but " + previousVersion
            );
        }

        if (testDefinition.equals(lastUpdate.testEdit.definition)) {
            throw new StoreException.TestUpdateException(
                    "Attempting to save test definition without changes for test: "
                            + testName
            );
        }

        insertNewRecord(changeMetadata, new TestEdit(testName, testDefinition));
    }

    @Override
    public synchronized void deleteTestDefinition(
            final ChangeMetadata changeMetadata,
            final String previousVersion,
            final String testName,
            final TestDefinition testDefinition
    ) throws StoreException.TestUpdateException {
        final UpdateRecord lastUpdate = getLatestUpdate(testName)
                .orElseThrow(() -> new StoreException.TestUpdateException(testName + " not yet added"));

        if (lastUpdate.testEdit.definition == null) {
            throw new StoreException.TestUpdateException(testName + " already deleted");
        }

        if (!previousVersion.equals(lastUpdate.revision.getRevision())) {
            throw new StoreException.TestUpdateException(
                    "Expected previous version is "
                            + lastUpdate.revision.getRevision()
                            + " but " + previousVersion
            );
        }

        insertNewRecord(changeMetadata, new TestEdit(testName, null));
    }

    // no-op methods

    @Override
    public boolean cleanUserWorkspace(final String username) {
        return false;
    }

    @Override
    public void verifySetup() {
    }

    @Override
    public void refresh() {
    }

    @Override
    public void close() {
    }

    // utility classes and methods

    private void insertNewRecord(
            final ChangeMetadata changeMetadata,
            @Nullable final TestEdit testEdit
    ) {
        final Revision revision = new Revision(
                revisionIdGenerator.get(),
                changeMetadata.getAuthor(),
                Date.from(changeMetadata.getTimestamp()),
                changeMetadata.getComment()
        );
        final UpdateRecord record = new UpdateRecord(revision, testEdit);

        globalHistory.add(0, record);
    }

    private UpdateRecord getUpdateRecord(final String revisionId) throws StoreException {
        return globalHistory.stream()
                .filter(x -> x.revision.getRevision().equals(revisionId))
                .findFirst()
                .orElseThrow(() -> new StoreException("Unknown revision " + revisionId));
    }

    private Optional<UpdateRecord> getLatestUpdate(final String testName) {
        return globalHistory.stream()
                .filter(x -> x.modifiedTests().contains(testName))
                .findFirst();
    }

    /**
     * Equivalent to the following in Java 9
     * list.stream()
     * .dropWhile(predicate)
     * .collect(toList())
     */
    private static <T> List<T> dropWhile(
            final List<T> list,
            final Predicate<T> predicate
    ) {
        final List<T> res = new ArrayList<>(list.size());
        boolean take = false;
        for (final T x : list) {
            if (!predicate.test(x)) {
                take = true;
            }
            if (take) {
                res.add(x);
            }
        }
        return res;
    }

    private static class UpdateRecord {
        private final Revision revision;
        @Nullable
        private final TestEdit testEdit;

        private UpdateRecord(
                final Revision revision,
                @Nullable final TestEdit testEdit
        ) {
            this.revision = Objects.requireNonNull(revision);
            this.testEdit = testEdit;
        }

        private RevisionDetails toRevisionDetails() {
            return new RevisionDetails(
                    revision,
                    modifiedTests()
            );
        }

        private Set<String> modifiedTests() {
            return this.testEdit == null
                    ? Collections.emptySet()
                    : Collections.singleton(testEdit.testName);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final UpdateRecord record = (UpdateRecord) o;
            return Objects.equals(revision, record.revision) &&
                    Objects.equals(testEdit, record.testEdit);
        }

        @Override
        public int hashCode() {
            return Objects.hash(revision, testEdit);
        }
    }

    private static class TestEdit {
        private final String testName;
        @Nullable
        private final TestDefinition definition;

        private TestEdit(
                final String testName,
                @Nullable final TestDefinition definition
        ) {
            this.testName = Objects.requireNonNull(testName);
            this.definition = definition;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TestEdit testEdit = (TestEdit) o;
            return Objects.equals(testName, testEdit.testName) &&
                    Objects.equals(definition, testEdit.definition);
        }

        @Override
        public int hashCode() {
            return Objects.hash(testName, definition);
        }
    }
}
