package com.indeed.proctor.store;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.Maps.newHashMapWithExpectedSize;

class GitHistoryParser {
    private static final Logger LOGGER = Logger.getLogger(GitHistoryParser.class);

    private static final int EXPECTED_NUMBER_ACTIVE_TESTS = 7000;

    private final RevWalk revWalk;
    private final DiffFormatter diffFormatter;
    private final Pattern testNamePattern;

    private static final Cache<String, List<DiffEntry>> DIFF_ENTRIES_CACHE = CacheBuilder
            .newBuilder()
            .build();

    private GitHistoryParser(
            final RevWalk revWalk,
            final DiffFormatter diffFormatter,
            final String definitionDirectory
    ) {
        this.revWalk = revWalk;
        this.diffFormatter = diffFormatter;
        testNamePattern = compileTestNamePattern(definitionDirectory);
    }

    /**
     * @return a map of testnames and git commits making changes to given tests
     */
    Map<String, List<Revision>> parseFromHead(final ObjectId head) throws IOException {
        final Map<String, List<Revision>> histories = newHashMapWithExpectedSize(EXPECTED_NUMBER_ACTIVE_TESTS);
        final Set<ObjectId> visited = Sets.newHashSet();
        final Queue<RevCommit> queue = new LinkedList<>();
        queue.add(revWalk.parseCommit(head));
        final long start = System.currentTimeMillis();
        while (!queue.isEmpty()) {
            parseCommit(queue.poll(), histories, visited, queue);
        }
        final long middle = System.currentTimeMillis();
        sortByDate(histories);
        final long end = System.currentTimeMillis();
        LOGGER.info(String.format("Took %d ms to parse, %d ms to sort revisions in chronological order", middle - start, end - middle));
        return histories;
    }

    /**
     * @return a revision details for a single revision
     */
    @Nonnull
    RevisionDetails parseRevisionDetails(final ObjectId revisionId) throws IOException {
        final RevCommit commit = revWalk.parseCommit(revisionId);
        final Revision revision = createRevisionFromCommit(commit);
        final Set<String> modifiedTests = getModifiedTests(commit);
        return new RevisionDetails(
                revision,
                modifiedTests
        );
    }

    /**
     * Add a commit to all histories of all tests modified by this commit
     */
    private void parseCommit(
            final RevCommit commit,
            final Map<String, List<Revision>> histories,
            final Set<ObjectId> visited,
            final Queue<RevCommit> queue
    ) throws IOException {
        if (!visited.add(commit.getId())) {
            return;
        }

        final Set<String> modifiedTests = getModifiedTests(commit);
        for (final String testName : modifiedTests) {
            final List<Revision> history = histories.computeIfAbsent(testName, x -> new ArrayList<>());
            history.add(createRevisionFromCommit(commit));
        }

        final RevCommit[] parents = commit.getParents();
        for (final RevCommit parent : parents) {
            queue.add(revWalk.parseCommit(parent.getId()));
        }
    }

    private Set<String> getModifiedTests(final RevCommit commit) throws IOException {
        final RevCommit[] parents = commit.getParents();
        final Set<String> result = new HashSet<>();
        if (parents.length == 1) { // merge commit if length > 1
            final RevCommit parent = revWalk.parseCommit(parents[0].getId());
            // get diff of this commit to its parent, as list of paths
            final List<DiffEntry> diffs = getDiffEntries(commit, parent);
            for (final DiffEntry diff : diffs) {
                final String changePath = diff.getChangeType().equals(DiffEntry.ChangeType.DELETE) ? diff.getOldPath() : diff.getNewPath();
                final Matcher testNameMatcher = testNamePattern.matcher(changePath);

                if (testNameMatcher.matches()) {
                    final String testName = testNameMatcher.group(1);
                    result.add(testName);
                }
            }
        }
        return result;
    }

    private List<DiffEntry> getDiffEntries(final RevCommit commit, final RevCommit parent) throws IOException {
        try {
            return DIFF_ENTRIES_CACHE.get(commit.getName(), () -> diffFormatter.scan(parent.getTree(), commit.getTree()));
        } catch (final ExecutionException e) {
            Throwables.propagateIfInstanceOf(e.getCause(), IOException.class);
            throw Throwables.propagate(e.getCause());
        }
    }

    @VisibleForTesting
    static void sortByDate(final Map<String, List<Revision>> histories) {
        final Comparator<Revision> comparator = (o1, o2) -> o2.getDate().compareTo(o1.getDate());
        for (final List<Revision> revisions : histories.values()) {
            revisions.sort(comparator);
        }
    }

    /**
     * @return a pattern that if it matches contains the testname in the first group of the matcher
     */
    @VisibleForTesting
    static Pattern compileTestNamePattern(final String definitionDirectory) {
        return Pattern.compile(definitionDirectory +
                File.separator + "(\\w+)" + File.separator + FileBasedProctorStore.TEST_DEFINITION_FILENAME);
    }

    static Revision createRevisionFromCommit(final RevCommit commit) {
        return new Revision(
                commit.getName(),
                commit.getAuthorIdent().getName(),
                new Date((long) commit.getCommitTime() * 1000 /* convert seconds to milliseconds */),
                commit.getFullMessage()
        );
    }

    static GitHistoryParser fromRepository(final Repository repository, final String testDefinitionDirectory) {
        final RevWalk revWalk = new RevWalk(repository);
        final DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
        df.setRepository(repository);
        df.setDiffComparator(RawTextComparator.DEFAULT);
        df.setDetectRenames(false); // to regard rename changes as add and remove
        return new GitHistoryParser(revWalk, df, testDefinitionDirectory);
    }
}
