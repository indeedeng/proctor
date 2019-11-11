package com.indeed.proctor.store.utils.test;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.ChangeMetadata;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.RevisionDetails;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.store.cache.CachingProctorStore;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * This class is an in-memory implementation of ProctorStore.
 * It's for testing purpose and not production ready.
 *
 * @author yiqing
 */
public class InMemoryProctorStore implements ProctorStore {

    public static final String REVISION_PREFIX = "revision-";

    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    /**
     * This field stores all TestMatrixVersions in historical order.
     * A new version will be added in head.
     */
    private final LinkedList<TestMatrixVersion> matrixVersionStorage = Lists.newLinkedList();

    /**
     * This field stores a mapping of a revision string to a TestMatrixVersion
     */
    private final Map<String, TestMatrixVersion> revisionMap = Maps.newHashMap();

    /**
     * This field stores a mapping of a revision string to a RevisionDetail
     */
    private final Map<String, RevisionDetails> revisionDetailMap = Maps.newHashMap();

    /**
     * This field stores all Revision information in historical order like matrixVersionStorage.
     * New revision will be added in head.
     */
    private final LinkedList<RevisionAndTest> revisionStorage = Lists.newLinkedList();

    public InMemoryProctorStore() {
        final TestMatrixVersion firstVersion = new TestMatrixVersion();
        final TestMatrixDefinition testMatrixDefinition = new TestMatrixDefinition();
        firstVersion.setAuthor("proctor");
        firstVersion.setVersion("-1");
        firstVersion.setPublished(new Date());
        firstVersion.setTestMatrixDefinition(testMatrixDefinition);
        firstVersion.setDescription("Initial commit");
        matrixVersionStorage.add(firstVersion);
    }

    @Override
    public String getName() {
        return InMemoryProctorStore.class.getName();
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public TestMatrixVersion getCurrentTestMatrix() throws StoreException {
        return synchronizedRead(() -> {
            if (matrixVersionStorage.isEmpty()) {
                return null;
            } else {
                return matrixVersionStorage.getFirst();
            }
        });
    }

    @Override
    public TestDefinition getCurrentTestDefinition(final String test) throws StoreException {
        return synchronizedRead(() -> {
            final TestMatrixVersion currentTestMatrix = getCurrentTestMatrix();
            if (currentTestMatrix == null) {
                return null;
            }
            return currentTestMatrix.getTestMatrixDefinition().getTests().get(test);
        });
    }

    @Override
    public void verifySetup() throws StoreException {
        /** do nothing */
    }

    @Override
    public boolean cleanUserWorkspace(final String username) {
        /** do nothing */
        return true;
    }

    @Override
    public void updateTestDefinition(
            final ChangeMetadata changeMetadata,
            final String previousVersion,
            final String testName,
            final TestDefinition testDefinition,
            final Map<String, String> metadata
    ) throws StoreException.TestUpdateException {
        synchronizedWrite((Callable<Void>) () -> {
            try {
                final TestMatrixVersion currentTestMatrix = getCurrentTestMatrix();
                final String currentVersion = currentTestMatrix.getVersion();
                Preconditions.checkState(previousVersion.equals(currentVersion), "Previous version doesn't match");
                final String newVersion = testDefinition.getVersion();
                final TestMatrixDefinition newTestMatrixDefinition = cloneTestMatrixDefinition(currentTestMatrix.getTestMatrixDefinition());
                newTestMatrixDefinition.getTests().put(testName, testDefinition);
                commitTestMatrixVersion(changeMetadata.getUsername(), newVersion, newTestMatrixDefinition, changeMetadata.getComment(), testName);
            } catch (final Exception e) {
                throw new StoreException.TestUpdateException("", e);
            }
            return null;
        });
    }

    @Override
    public void deleteTestDefinition(
            final ChangeMetadata changeMetadata,
            final String previousVersion,
            final String testName,
            final TestDefinition testDefinition
    ) throws StoreException.TestUpdateException {
        synchronizedWrite((Callable<Void>) () -> {

            try {
                final TestMatrixVersion currentTestMatrix = getCurrentTestMatrix();
                final String currentVersion = currentTestMatrix.getVersion();

                Preconditions.checkState(previousVersion.equals(currentVersion), "Previous version doesn't match");

                final TestMatrixDefinition newTestMatrixDefinition = cloneTestMatrixDefinition(currentTestMatrix.getTestMatrixDefinition());

                if (newTestMatrixDefinition.getTests().containsKey(testName)) {
                    final String newVersion = testDefinition.getVersion();
                    newTestMatrixDefinition.getTests().remove(testName);
                    commitTestMatrixVersion(changeMetadata.getUsername(), newVersion, newTestMatrixDefinition, changeMetadata.getComment(), testName);
                }
            } catch (final Exception e) {
                throw new StoreException.TestUpdateException("Failed to delete test", e);
            }
            return null;
        });
    }

    @Override
    public void addTestDefinition(
            final ChangeMetadata changeMetadata,
            final String testName,
            final TestDefinition testDefinition,
            final Map<String, String> metadata
    ) throws StoreException.TestUpdateException {
        synchronizedWrite((Callable<Void>) () -> {
            try {
                final TestMatrixVersion currentTestMatrix = getCurrentTestMatrix();
                final TestMatrixDefinition newTestMatrixDefinition = cloneTestMatrixDefinition(currentTestMatrix.getTestMatrixDefinition());
                Preconditions.checkState(!newTestMatrixDefinition.getTests().containsKey(testName), "Test already exists");
                newTestMatrixDefinition.getTests().put(testName, testDefinition);
                final String newVersion = testDefinition.getVersion();
                commitTestMatrixVersion(changeMetadata.getUsername(), newVersion, newTestMatrixDefinition, changeMetadata.getComment(), testName);
            } catch (final Exception e) {
                throw new StoreException.TestUpdateException("Failed to add ", e);
            }
            return null;
        });
    }

    @Nonnull
    @Override
    public String getLatestVersion() throws StoreException {
        return synchronizedRead(() -> {
            final TestMatrixVersion currentTestMatrix = getCurrentTestMatrix();
            if (currentTestMatrix == null) {
                return "-1";
            }
            return currentTestMatrix.getVersion();
        });
    }

    @Override
    public TestMatrixVersion getTestMatrix(final String fetchRevision) throws StoreException {
        failIfRevisionNotInStore(fetchRevision);
        return synchronizedRead(() -> revisionMap.get(fetchRevision));
    }

    @Override
    public TestDefinition getTestDefinition(final String test, final String fetchRevision) throws StoreException {
        return synchronizedRead(() -> {
            final TestMatrixVersion testMatrix = getTestMatrix(fetchRevision);
            if (testMatrix == null) {
                return null;
            }

            final TestMatrixDefinition testMatrixDefinition = testMatrix.getTestMatrixDefinition();
            Preconditions.checkNotNull(testMatrixDefinition, "TestMatrixDefinition must not be null");
            return testMatrixDefinition.getTests().get(test);
        });
    }

    @Nonnull
    @Override
    public List<Revision> getMatrixHistory(final int start, final int limit) throws StoreException {
        return synchronizedRead(() -> {
            final List<RevisionAndTest> result = CachingProctorStore.selectHistorySet(revisionStorage, start, limit);
            return castToRevisionList(result);
        });
    }

    @Nonnull
    @Override
    public List<Revision> getMatrixHistory(final Instant sinceInclusive, final Instant untilExclusive) throws StoreException {
        return synchronizedRead(() -> {
            final List<RevisionAndTest> result = revisionStorage
                    .stream()
                    .filter(r -> {
                        final Instant rDate = r.getDate().toInstant();
                        return ((rDate.isAfter(sinceInclusive)) || rDate.equals(sinceInclusive))
                                && (rDate.isBefore(untilExclusive));
                    })
                    .collect(Collectors.toList());
            return castToRevisionList(result);
        });
    }

    @Nonnull
    @Override
    public List<Revision> getHistory(final String test, final int start, final int limit) throws StoreException {
        return synchronizedRead(() -> {
            final List<Revision> revisions = filterRevisionByTest(revisionStorage, test);
            return CachingProctorStore.selectHistorySet(revisions, start, limit);
        });
    }

    @Nonnull
    @Override
    public List<Revision> getHistory(final String test, final String revision, final int start, final int limit) throws StoreException {
        return synchronizedRead(() -> {
            // check revision in store
            failIfRevisionNotInStore(revision);
            final List<Revision> revisions = filterRevisionByTest(revisionStorage, test);
            return CachingProctorStore.selectRevisionHistorySetFrom(revisions, revision, start, limit);
        });
    }

    @CheckForNull
    @Override
    public RevisionDetails getRevisionDetails(final String revisionId) throws StoreException {
        return synchronizedRead(() -> revisionDetailMap.get(revisionId));
    }

    @Nonnull
    @Override
    public Map<String, List<Revision>> getAllHistories() throws StoreException {
        return synchronizedRead(() -> {
            final Map<String, List<Revision>> result = Maps.newHashMap();

            for (final RevisionAndTest revisionAndTest : revisionStorage) {
                final String testName = revisionAndTest.getTestName();
                if (!result.containsKey(testName)) {
                    result.put(testName, Lists.<Revision>newArrayList());
                }
                result.get(testName).add(revisionAndTest);
            }
            return result;
        });
    }

    @Override
    public void refresh() throws StoreException {
    }

    private void failIfRevisionNotInStore(final String revision) throws StoreException {
        revisionStorage.stream()
                .filter(revisionAndTest -> revisionAndTest.getRevision().equals(revision))
                .findFirst()
                .orElseThrow(() -> new StoreException("Cannot find revision " + revision));
    }

    private void commitTestMatrixVersion(final String username, final String newVersion, final TestMatrixDefinition testMatrixDefinition,
                                         final String comment, final String testName) {
        final TestMatrixVersion newTestMatrixVersion = new TestMatrixVersion();
        final Date now = new Date();
        newTestMatrixVersion.setAuthor(username);
        newTestMatrixVersion.setVersion(newVersion);
        newTestMatrixVersion.setPublished(now);
        newTestMatrixVersion.setTestMatrixDefinition(testMatrixDefinition);
        newTestMatrixVersion.setDescription(comment);
        final String revisionString = REVISION_PREFIX + newVersion;
        final RevisionDetails revisionDetails = new RevisionDetails(
                new Revision(revisionString, username, now, comment),
                Collections.singleton(testName)
        );
        final RevisionAndTest revision = new RevisionAndTest(revisionString, username, now, comment, testName);
        if (revisionMap.containsKey(revisionString)) {
            throw new RuntimeException("Revision conflict! " + revisionString);
        }
        revisionMap.put(revisionString, newTestMatrixVersion);
        revisionDetailMap.put(revisionString, revisionDetails);
        matrixVersionStorage.addFirst(newTestMatrixVersion);
        revisionStorage.addFirst(revision);
    }

    private static TestMatrixDefinition cloneTestMatrixDefinition(final TestMatrixDefinition old) {
        final Map<String, TestDefinition> tests = old.getTests();
        final Map<String, TestDefinition> newTests = new HashMap<String, TestDefinition>();
        for (final Map.Entry<String, TestDefinition> entry : tests.entrySet()) {
            newTests.put(entry.getKey(), entry.getValue());
        }
        final TestMatrixDefinition newt = new TestMatrixDefinition();
        newt.setTests(newTests);
        return newt;
    }

    private static List<Revision> castToRevisionList(final List<RevisionAndTest> list) {
        return new ArrayList<>(list);
    }

    private static List<Revision> filterRevisionByTest(final List<RevisionAndTest> revisionHistory, final String test) {
        return revisionHistory.stream()
                .filter(revisionAndTest -> revisionAndTest.getTestName().equals(test))
                .collect(Collectors.toList());
    }

    /**
     * This class is almost the same as Revision,
     * but with a new field to store the name of test added/modified/deleted in this Revision
     */
    private static class RevisionAndTest extends Revision {
        final String testName;

        RevisionAndTest(final String revision, final String author, final Date date, final String message, final String testName) {
            super(revision, author, date, message);
            this.testName = testName;
        }

        public String getTestName() {
            return testName;
        }
    }

    private <T> T synchronizedRead(final Callable<T> callable) throws StoreException {
        try {
            if (readLock.tryLock(10, TimeUnit.SECONDS)) {
                try {
                    return callable.call();
                } catch (final Exception e) {
                    throw new StoreException("Failed to perform read operation to cache. ", e);
                } finally {
                    readLock.unlock();
                }
            } else {
                throw new RuntimeException("Failed to acquire lock");
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw Throwables.propagate(e);
        }
    }

    private <T> T synchronizedWrite(final Callable<T> callable) throws StoreException.TestUpdateException {
        try {
            if (writeLock.tryLock(10, TimeUnit.SECONDS)) {
                try {
                    return callable.call();
                } catch (final Exception e) {
                    throw new StoreException.TestUpdateException("Failed to perform write operation to cache. ", e);
                } finally {
                    writeLock.unlock();
                }
            } else {
                throw new RuntimeException("Failed to acquire lock");
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw Throwables.propagate(e);
        }
    }
}
