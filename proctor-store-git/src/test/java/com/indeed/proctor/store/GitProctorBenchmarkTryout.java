package com.indeed.proctor.store;

import com.google.common.io.Files;
import com.indeed.proctor.common.model.TestMatrixVersion;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/** Not a unit test, but a CLI app to run a small benchmark */
public class GitProctorBenchmarkTryout {

    public static void main(String args[]) throws IOException {
        final String gitUrl = System.console().readLine("git url: ");
        final String gituser = System.console().readLine("user: ");
        final String password = new String(System.console().readPassword("password: "));
        final int num_revisions =
                Integer.parseInt(System.console().readLine("number of histories: "));
        final String testDefinitionDirectory =
                System.console().readLine("test definitions directory: ");

        final File tempDir = Files.createTempDir();
        try {
            final GitProctor client =
                    new GitProctor(gitUrl, gituser, password, testDefinitionDirectory);

            System.out.println("Running load matrix for last " + num_revisions + " revisions");
            final long start = System.currentTimeMillis();
            final List<Revision> revisions = client.getMatrixHistory(0, num_revisions);
            for (final Revision rev : revisions) {
                final TestMatrixVersion matrix = client.getTestMatrix(rev.getRevision());
            }
            final long elapsed = System.currentTimeMillis() - start;
            System.out.println(
                    "Finished reading matrix history ("
                            + revisions.size()
                            + ") in "
                            + elapsed
                            + " ms");
            client.close();
        } catch (StoreException e) {
            e.printStackTrace(System.err);
        } finally {
            System.out.println("Deleting temp dir : " + tempDir);
            FileUtils.deleteDirectory(tempDir);
        }
    }
}
