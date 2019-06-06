package com.indeed.proctor.store;

import com.google.common.collect.ImmutableList;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestType;
import org.apache.commons.io.FileUtils;
import org.assertj.core.groups.Tuple;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class GitProctorTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private GitProctor gitProctor;
    private Git git;

    @Before
    public void setUp() throws IOException, GitAPIException {
        git = Git.init().setDirectory(testFolder.getRoot()).call();
        testFolder.newFile(".gitkeep");
        git.add().addFilepattern(".gitkeep").call();
        git.commit().setMessage("initial").call();

        gitProctor = new GitProctor(testFolder.getRoot().getPath(), "", "");
    }

    @Test
    public void testHistories() throws StoreException {
        final String revisionA =
                addTestDefinition("proc_a_tst", "author1", "add a new test a", DEFINITION_A);
        final String revisionB =
                addTestDefinition("proc_b_tst", "author2", "add a new test b", DEFINITION_B);
        final String revisionC =
                updateTestDefinition("proc_a_tst", "author3", "edit a test a", DEFINITION_B);

        assertThat(gitProctor.getLatestVersion()).isEqualTo(revisionC);

        assertThat(gitProctor.getHistory("proc_a_tst", 0, 10))
                .extracting(Revision::getRevision, Revision::getAuthor, Revision::getMessage)
                .containsExactly(
                        Tuple.tuple(revisionC, "author3", "edit a test a"),
                        Tuple.tuple(revisionA, "author1", "add a new test a")
                );

        assertThat(gitProctor.getHistory("proc_b_tst", 0, 10))
                .extracting(Revision::getRevision, Revision::getAuthor, Revision::getMessage)
                .containsExactly(
                        Tuple.tuple(revisionB, "author2", "add a new test b")
                );

        assertThat(gitProctor.getMatrixHistory(0, 3))
                .extracting(Revision::getRevision, Revision::getAuthor, Revision::getMessage)
                .containsExactly(
                        Tuple.tuple(revisionC, "author3", "edit a test a"),
                        Tuple.tuple(revisionB, "author2", "add a new test b"),
                        Tuple.tuple(revisionA, "author1", "add a new test a")
                );

        assertThat(gitProctor.getMatrixHistory(1, 1))
                .extracting(Revision::getRevision, Revision::getAuthor, Revision::getMessage)
                .containsExactly(
                        Tuple.tuple(revisionB, "author2", "add a new test b")
                );

        assertThat(gitProctor.getAllHistories())
                .containsOnlyKeys("proc_a_tst", "proc_b_tst");

        assertThat(gitProctor.getAllHistories().get("proc_a_tst"))
                .extracting(Revision::getRevision)
                .containsExactly(revisionC, revisionA);

        assertThat(gitProctor.getAllHistories().get("proc_b_tst"))
                .extracting(Revision::getRevision)
                .containsExactly(revisionB);

    }

    @Test
    public void testRevisionDetails() throws StoreException, IOException, GitAPIException {
        final String revisionA =
                addTestDefinition("proc_a_tst", "author1", "add a new test a", DEFINITION_A);
        addTestDefinition("proc_b_tst", "author2", "add a new test b", DEFINITION_B);
        final String revisionC = deleteAllTestDefinitions("delete all");

        assertThat(gitProctor.getRevisionDetails(revisionA))
                .extracting(
                        r -> r.getRevision().getRevision(),
                        r -> r.getRevision().getAuthor(),
                        RevisionDetails::getModifiedTests
                )
                .containsExactly(
                        revisionA,
                        "author1",
                        Collections.singletonList("proc_a_tst")
                );

        assertThat(gitProctor.getRevisionDetails(revisionC))
                .extracting(
                        r -> r.getRevision().getRevision(),
                        r -> r.getRevision().getMessage(),
                        RevisionDetails::getModifiedTests
                )
                .containsExactly(
                        revisionC,
                        "delete all",
                        Arrays.asList("proc_a_tst", "proc_b_tst")
                );

        assertThat(gitProctor.getRevisionDetails("invalidrevisionid"))
                .isNull();

        final String unknownRevision = "5f508f66bbacc1df5b9644b621763bcf26321f61";
        assertThat(gitProctor.getRevisionDetails(unknownRevision))
                .isNull();
    }

    private String addTestDefinition(
            final String testName,
            final String author,
            final String message,
            final TestDefinition definition
    ) throws StoreException {
        gitProctor.addTestDefinition(
                "",
                "",
                author,
                testName,
                definition,
                Collections.emptyMap(),
                message
        );
        return gitProctor.getHistory(testName, 0, 1).get(0)
                .getRevision();
    }

    private String updateTestDefinition(
            final String testName,
            final String author,
            final String message,
            final TestDefinition definition
    ) throws StoreException {
        gitProctor.updateTestDefinition(
                "",
                "",
                author,
                "",
                testName,
                definition,
                Collections.emptyMap(),
                message
        );
        return gitProctor.getHistory(testName, 0, 1).get(0)
                .getRevision();
    }

    private String deleteAllTestDefinitions(
            final String message
    ) throws IOException, GitAPIException, StoreException {
        FileUtils.deleteDirectory(
                testFolder.getRoot().toPath().resolve(GitProctor.DEFAULT_TEST_DEFINITIONS_DIRECTORY).toFile()
        );
        git.add().addFilepattern("*").call();
        final String revision = git.commit().setMessage(message).call().getId().getName();
        gitProctor.refresh();
        return revision;
    }

    private static final TestDefinition DEFINITION_A = new TestDefinition(
            "-1",
            null,
            TestType.ANONYMOUS_USER,
            "&test_a",
            ImmutableList.of(new TestBucket("active", 1, "")),
            ImmutableList.of(
                    new Allocation(null, ImmutableList.of(new Range(1, 1.0)), "#A1")
            ),
            false,
            Collections.emptyMap(),
            Collections.emptyMap(),
            "Create test a"
    );

    private static final TestDefinition DEFINITION_B = new TestDefinition(
            "-1",
            null,
            TestType.AUTHENTICATED_USER,
            "&test_b",
            ImmutableList.of(new TestBucket("inactive", -1, "")),
            ImmutableList.of(
                    new Allocation(null, ImmutableList.of(new Range(-1, 1.0)), "#A1")
            ),
            false,
            Collections.emptyMap(),
            Collections.emptyMap(),
            "Create test b"
    );

}
