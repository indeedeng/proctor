package com.indeed.proctor.store;

import com.google.common.collect.ImmutableList;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.common.model.TestType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
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
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GitProctorTest {
    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private GitProctor gitProctor;
    private Git git;
    private String initialCommitHash;

    @Before
    public void setUp() throws IOException, GitAPIException {
        git = Git.init().setDirectory(testFolder.getRoot()).call();

        // initial commit is required to initialize GitProctor
        testFolder.newFile(".gitkeep");
        git.add().addFilepattern(".gitkeep").call();
        initialCommitHash = git.commit().setMessage("initial commit").call().getId().getName();

        gitProctor = new GitProctor(testFolder.getRoot().getPath(), "", "");
    }

    @Test
    public void testGetDefinition() throws StoreException, IOException, GitAPIException {
        final String revisionA =
                addTestDefinition("proc_tst", "author", "add a new test", DEFINITION_A);
        final String revisionB =
                addTestDefinition("proc_another_tst", "author", "add a another", DEFINITION_B);
        final String revisionC =
                updateTestDefinition("proc_tst", "author", "edit a test", DEFINITION_B);
        final String revisionD =
                deleteAllTestDefinitions("delete tests");
        final String revisionE =
                addTestDefinition("proc_new_tst", "author", "add a new test", DEFINITION_A);

        assertThat(gitProctor.getTestDefinition("proc_tst", revisionA))
                .isEqualTo(DEFINITION_A);
        assertThat(gitProctor.getTestDefinition("proc_tst", revisionB))
                .isEqualTo(DEFINITION_A);
        assertThat(gitProctor.getTestDefinition("proc_tst", revisionC))
                .isEqualTo(DEFINITION_B);
        assertThatThrownBy(() -> gitProctor.getTestDefinition("proc_tst", revisionD))
                .isInstanceOf(StoreException.class);
        assertThatThrownBy(() -> gitProctor.getTestDefinition("proc_tst", revisionE))
                .isInstanceOf(StoreException.class);

        assertThatThrownBy(() -> gitProctor.getTestDefinition("proc_another_tst", revisionA))
                .isInstanceOf(StoreException.class);
        assertThat(gitProctor.getTestDefinition("proc_another_tst", revisionB))
                .isEqualTo(DEFINITION_B);
        assertThat(gitProctor.getTestDefinition("proc_another_tst", revisionC))
                .isEqualTo(DEFINITION_B);
        assertThatThrownBy(() -> gitProctor.getTestDefinition("proc_another_tst", revisionD))
                .isInstanceOf(StoreException.class);
        assertThatThrownBy(() -> gitProctor.getTestDefinition("proc_another_tst", revisionE))
                .isInstanceOf(StoreException.class);

        assertThatThrownBy(() -> gitProctor.getCurrentTestDefinition("proc_tst"))
                .isInstanceOf(StoreException.class);
        assertThatThrownBy(() -> gitProctor.getCurrentTestDefinition("proc_another_tst"))
                .isInstanceOf(StoreException.class);
        assertThat(gitProctor.getCurrentTestDefinition("proc_new_tst"))
                .isEqualTo(DEFINITION_A);

    }

    @Test
    public void testGetMatrix() throws StoreException, IOException, GitAPIException {
        final String revisionA =
                addTestDefinition("proc_tst", "author", "add a new test", DEFINITION_A);
        final String revisionB =
                addTestDefinition("proc_another_tst", "author", "add a another", DEFINITION_B);
        final String revisionC =
                deleteAllTestDefinitions("delete tests");

        assertThat(gitProctor.getTestMatrix(revisionA).getTestMatrixDefinition().getTests())
                .hasSize(1)
                .containsEntry("proc_tst", DEFINITION_A);
        assertThat(gitProctor.getTestMatrix(revisionA))
                .extracting(TestMatrixVersion::getAuthor, TestMatrixVersion::getDescription, TestMatrixVersion::getVersion)
                .containsExactly("author", "add a new test", revisionA);

        assertThat(gitProctor.getTestMatrix(revisionB).getTestMatrixDefinition().getTests())
                .hasSize(2)
                .containsEntry("proc_tst", DEFINITION_A)
                .containsEntry("proc_another_tst", DEFINITION_B);
        assertThat(gitProctor.getTestMatrix(revisionB))
                .extracting(TestMatrixVersion::getAuthor, TestMatrixVersion::getDescription, TestMatrixVersion::getVersion)
                .containsExactly("author", "add a another", revisionB);

        assertThat(gitProctor.getTestMatrix(revisionC).getTestMatrixDefinition().getTests())
                .isEmpty();
        assertThat(gitProctor.getCurrentTestMatrix().getTestMatrixDefinition().getTests())
                .isEmpty();
        assertThat(gitProctor.getCurrentTestMatrix())
                .extracting(TestMatrixVersion::getDescription, TestMatrixVersion::getVersion)
                .containsExactly("delete tests", revisionC);
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

        assertThat(gitProctor.getMatrixHistory(0, 10))
                .extracting(Revision::getRevision, Revision::getMessage)
                .containsExactly(
                        Tuple.tuple(revisionC, "edit a test a"),
                        Tuple.tuple(revisionB, "add a new test b"),
                        Tuple.tuple(revisionA, "add a new test a"),
                        Tuple.tuple(initialCommitHash, "initial commit")
                );

        assertThat(gitProctor.getMatrixHistory(1, 1))
                .extracting(Revision::getRevision, Revision::getAuthor, Revision::getMessage)
                .containsExactly(
                        Tuple.tuple(revisionB, "author2", "add a new test b")
                );

        assertThat(gitProctor.getAllHistories())
                .hasSize(2)
                .hasEntrySatisfying("proc_a_tst", l ->
                        assertThat(l)
                                .extracting(Revision::getRevision)
                                .containsExactly(revisionC, revisionA)
                )
                .hasEntrySatisfying("proc_b_tst", l ->
                        assertThat(l)
                                .extracting(Revision::getRevision)
                                .containsExactly(revisionB)
                );
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

    private static final TestDefinition DEFINITION_A = createStubTestDefinition();
    private static final TestDefinition DEFINITION_B = createStubTestDefinition();

    private static TestDefinition createStubTestDefinition() {
        final double activeRatio = ThreadLocalRandom.current().nextInt(100) / 100.0;
        return new TestDefinition(
                "-1",
                null,
                TestType.ANONYMOUS_USER,
                "&" + RandomStringUtils.randomAlphabetic(8),
                ImmutableList.of(
                        new TestBucket("inactive", -1, ""),
                        new TestBucket("active", 1, "")
                ),
                ImmutableList.of(
                        new Allocation(
                                null,
                                ImmutableList.of(
                                        new Range(-1, 1.0 - activeRatio),
                                        new Range(1, activeRatio)
                                ),
                                "#A1"
                        )
                ),
                false,
                Collections.emptyMap(),
                Collections.emptyMap(),
                RandomStringUtils.randomAlphabetic(8)
        );
    }

}
