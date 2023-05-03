package com.indeed.proctor.common.model;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * This represents what's serialized to the database, while {@link TestMatrixArtifact} represents what is consumed by applications
 *
 * @author ketan
 */
public class TestMatrixVersion {
    @Nullable
    private TestMatrixDefinition testMatrixDefinition;
    @Nullable
    private Date published;
    private String version;
    @Nullable
    private String description;
    @Nullable
    private String author;

    public TestMatrixVersion() {
    }

    public TestMatrixVersion(@Nullable final TestMatrixDefinition testMatrixDefinition,
                             @Nullable final Date published,
                             final String version,
                             @Nullable final String description,
                             @Nullable final String author
    ) {
        this.testMatrixDefinition = testMatrixDefinition;
        this.published = published;
        this.version = version;
        this.description = description;
        this.author = author;
    }

    public TestMatrixVersion(final TestMatrixVersion testMatrixVersion) {
        this.testMatrixDefinition = (testMatrixVersion.testMatrixDefinition != null) ?
                new TestMatrixDefinition(testMatrixVersion.testMatrixDefinition) : null;
        this.published = testMatrixVersion.published;
        this.version = testMatrixVersion.version;
        this.description = testMatrixVersion.description;
        this.author = testMatrixVersion.author;
    }

    @Nullable
    public TestMatrixDefinition getTestMatrixDefinition() {
        return testMatrixDefinition;
    }

    public void setTestMatrixDefinition(@Nullable final TestMatrixDefinition testMatrixDefinition) {
        this.testMatrixDefinition = testMatrixDefinition;
    }

    @Nullable
    public Date getPublished() {
        return published;
    }

    public void setPublished(@Nullable final Date published) {
        this.published = published;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable final String description) {
        this.description = description;
    }

    @Nullable
    public String getAuthor() {
        return author;
    }

    public void setAuthor(@Nullable final String author) {
        this.author = author;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TestMatrixVersion.class.getSimpleName() + "[", "]")
                .add("testMatrixDefinition=" + testMatrixDefinition)
                .add("published=" + published)
                .add("version='" + version + "'")
                .add("description='" + description + "'")
                .add("author='" + author + "'")
                .toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        final TestMatrixVersion that = (TestMatrixVersion) o;
        return Objects.equals(testMatrixDefinition, that.testMatrixDefinition)
                && Objects.equals(published, that.published)
                && Objects.equals(version, that.version)
                && Objects.equals(description, that.description)
                && Objects.equals(author, that.author);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testMatrixDefinition, published, version, description, author);
    }
}
