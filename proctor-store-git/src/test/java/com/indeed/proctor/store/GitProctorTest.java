package com.indeed.proctor.store;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.common.model.TestType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.groups.Tuple;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;

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
        final String revision1 =
                addTestDefinition("proc_tst", "author", "add a new test", DEFINITION_A);
        final String revision2 =
                addTestDefinition("proc_another_tst", "author", "add a another", DEFINITION_B);
        final String revision3 =
                updateTestDefinition("proc_tst", "author", "edit a test", DEFINITION_B);
        final String revision4 =
                deleteAllTestDefinitions("delete tests");
        final String revision5 =
                addTestDefinition("proc_new_tst", "author", "add a new test", DEFINITION_A);

        assertThat(gitProctor.getTestDefinition("proc_tst", revision1))
                .isEqualTo(DEFINITION_A);
        assertThat(gitProctor.getTestDefinition("proc_tst", revision2))
                .isEqualTo(DEFINITION_A);
        assertThat(gitProctor.getTestDefinition("proc_tst", revision3))
                .isEqualTo(DEFINITION_B);
        assertThatThrownBy(() -> gitProctor.getTestDefinition("proc_tst", revision4))
                .isInstanceOf(StoreException.class);
        assertThatThrownBy(() -> gitProctor.getTestDefinition("proc_tst", revision5))
                .isInstanceOf(StoreException.class);

        assertThatThrownBy(() -> gitProctor.getTestDefinition("proc_another_tst", revision1))
                .isInstanceOf(StoreException.class);
        assertThat(gitProctor.getTestDefinition("proc_another_tst", revision2))
                .isEqualTo(DEFINITION_B);
        assertThat(gitProctor.getTestDefinition("proc_another_tst", revision3))
                .isEqualTo(DEFINITION_B);
        assertThatThrownBy(() -> gitProctor.getTestDefinition("proc_another_tst", revision4))
                .isInstanceOf(StoreException.class);
        assertThatThrownBy(() -> gitProctor.getTestDefinition("proc_another_tst", revision5))
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
        final String revision1 =
                addTestDefinition("proc_tst", "author", "add a new test", DEFINITION_A);
        final String revision2 =
                addTestDefinition("proc_another_tst", "author", "add a another", DEFINITION_B);
        final String revision3 =
                deleteAllTestDefinitions("delete tests");

        assertThat(gitProctor.getTestMatrix(revision1).getTestMatrixDefinition().getTests())
                .hasSize(1)
                .containsEntry("proc_tst", DEFINITION_A);
        assertThat(gitProctor.getTestMatrix(revision1))
                .extracting(TestMatrixVersion::getAuthor, TestMatrixVersion::getDescription, TestMatrixVersion::getVersion)
                .containsExactly("author", "add a new test", revision1);

        assertThat(gitProctor.getTestMatrix(revision2).getTestMatrixDefinition().getTests())
                .hasSize(2)
                .containsEntry("proc_tst", DEFINITION_A)
                .containsEntry("proc_another_tst", DEFINITION_B);
        assertThat(gitProctor.getTestMatrix(revision2))
                .extracting(TestMatrixVersion::getAuthor, TestMatrixVersion::getDescription, TestMatrixVersion::getVersion)
                .containsExactly("author", "add a another", revision2);

        assertThat(gitProctor.getTestMatrix(revision3).getTestMatrixDefinition().getTests())
                .isEmpty();
        assertThat(gitProctor.getCurrentTestMatrix().getTestMatrixDefinition().getTests())
                .isEmpty();
        assertThat(gitProctor.getCurrentTestMatrix())
                .extracting(TestMatrixVersion::getDescription, TestMatrixVersion::getVersion)
                .containsExactly("delete tests", revision3);
    }

    @Test
    public void testHistories() throws StoreException {
        final String revision1 =
                addTestDefinition("proc_a_tst", "author1", "add a new test a", DEFINITION_A);
        final String revision2 =
                addTestDefinition("proc_b_tst", "author2", "add a new test b", DEFINITION_B);
        final String revision3 =
                updateTestDefinition("proc_a_tst", "author3", "edit a test a", DEFINITION_B);

        assertThat(gitProctor.getLatestVersion()).isEqualTo(revision3);

        assertThat(gitProctor.getHistory("proc_a_tst", 0, 10))
                .extracting(Revision::getRevision, Revision::getAuthor, Revision::getMessage)
                .containsExactly(
                        Tuple.tuple(revision3, "author3", "edit a test a"),
                        Tuple.tuple(revision1, "author1", "add a new test a")
                );

        assertThat(gitProctor.getHistory("proc_a_tst", revision1, 0, 10))
                .extracting(Revision::getRevision, Revision::getAuthor, Revision::getMessage)
                .containsExactly(
                        Tuple.tuple(revision1, "author1", "add a new test a")
                );

        assertThat(gitProctor.getHistory("proc_b_tst", 0, 10))
                .extracting(Revision::getRevision, Revision::getAuthor, Revision::getMessage)
                .containsExactly(
                        Tuple.tuple(revision2, "author2", "add a new test b")
                );

        assertThatThrownBy(() -> gitProctor.getHistory("proc_a_tst", UNKNOWN_GIT_REVISION, 0, 10))
                .isInstanceOf(StoreException.class);

        assertThat(gitProctor.getMatrixHistory(0, 10))
                .extracting(Revision::getRevision, Revision::getMessage)
                .containsExactly(
                        Tuple.tuple(revision3, "edit a test a"),
                        Tuple.tuple(revision2, "add a new test b"),
                        Tuple.tuple(revision1, "add a new test a"),
                        Tuple.tuple(initialCommitHash, "initial commit")
                );

        assertThat(gitProctor.getMatrixHistory(1, 1))
                .extracting(Revision::getRevision, Revision::getAuthor, Revision::getMessage)
                .containsExactly(
                        Tuple.tuple(revision2, "author2", "add a new test b")
                );

        assertThat(gitProctor.getAllHistories())
                .hasSize(2)
                .hasEntrySatisfying("proc_a_tst", l ->
                        assertThat(l)
                                .extracting(Revision::getRevision)
                                .containsExactly(revision3, revision1)
                )
                .hasEntrySatisfying("proc_b_tst", l ->
                        assertThat(l)
                                .extracting(Revision::getRevision)
                                .containsExactly(revision2)
                );
    }

    @Test
    public void testRevisionDetails() throws StoreException, IOException, GitAPIException {
        final String revision1 =
                addTestDefinition("proc_a_tst", "author1", "add a new test a", DEFINITION_A);
        addTestDefinition("proc_b_tst", "author2", "add a new test b", DEFINITION_B);
        final String revision3 = deleteAllTestDefinitions("delete all");

        assertThat(gitProctor.getRevisionDetails(revision1))
                .extracting(
                        r -> r.getRevision().getRevision(),
                        r -> r.getRevision().getAuthor(),
                        RevisionDetails::getModifiedTests
                )
                .containsExactly(
                        revision1,
                        "author1",
                        Collections.singleton("proc_a_tst")
                );

        assertThat(gitProctor.getRevisionDetails(revision3))
                .extracting(
                        r -> r.getRevision().getRevision(),
                        r -> r.getRevision().getMessage(),
                        RevisionDetails::getModifiedTests
                )
                .containsExactly(
                        revision3,
                        "delete all",
                        ImmutableSet.of("proc_a_tst", "proc_b_tst")
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

    private static final String UNKNOWN_GIT_REVISION = StringUtils.repeat('0', 40);

    private static final TestDefinition DEFINITION_A = createStubTestDefinition();
    private static final TestDefinition DEFINITION_B = createStubTestDefinition();

    private static TestDefinition createStubTestDefinition() {
        return new TestDefinition(
                "-1",
                null,
                TestType.ANONYMOUS_USER,
                "&" + RandomStringUtils.randomAlphabetic(8).toLowerCase(Locale.ENGLISH),
                ImmutableList.of(
                        new TestBucket("inactive", -1, "")
                ),
                ImmutableList.of(
                        new Allocation(
                                null,
                                ImmutableList.of(
                                        new Range(-1, 1.0)
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
