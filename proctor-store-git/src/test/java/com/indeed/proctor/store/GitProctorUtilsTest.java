package com.indeed.proctor.store;

import org.junit.Test;

import static com.indeed.proctor.store.GitProctorUtils.determineAuthorIdFromNameAndEmail;
import static org.assertj.core.api.Assertions.assertThat;

public class GitProctorUtilsTest {
    @Test
    public void testDetermineAuthorId_manualCommits() {
        // manual commit should use local-part of email as user's id
        assertThat(determineAuthorIdFromNameAndEmail("Foo Bar", "foo@example.com"))
                .isEqualTo("foo");
    }

    @Test
    public void testDetermineAuthorId_proctorWebapp() {
        // commit from proctor webapp puts same value in name and email.
        // we should use name (or email) as user's id.
        assertThat(determineAuthorIdFromNameAndEmail("foo", "foo")).isEqualTo("foo");
    }

    @Test
    public void testDetermineAuthorId_manualCommitsWithoutEmail() {
        // manual commit without valid email should fallback to name
        assertThat(determineAuthorIdFromNameAndEmail("Foo Bar", "")).isEqualTo("Foo Bar");
        assertThat(determineAuthorIdFromNameAndEmail("Foo Bar", "this-is-not-email"))
                .isEqualTo("Foo Bar");
    }
}
