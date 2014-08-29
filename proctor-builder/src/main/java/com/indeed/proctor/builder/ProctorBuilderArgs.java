package com.indeed.proctor.builder;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import java.io.PrintWriter;

/**
 * @author parker
 */
class ProctorBuilderArgs {

    private String outputdir;
    private String filename;

    // Artifact overrides
    private String author = null;
    private String version = "";

    protected final Options options;

    ProctorBuilderArgs() {
        options = new Options();

        options.addOption(OptionBuilder.hasArg(true)
                              .withLongOpt("output")
                              .withArgName("output directory")
                              .withDescription("The directory to write into. Use - for STDOUT")
                              .create("o"));
        options.addOption(OptionBuilder.hasArg(true)
                              .withLongOpt("filename")
                              .withArgName("filename")
                              .withDescription("The filename to use. default=proctor-tests-matrix.json")
                              .create("f"));

        options.addOption(OptionBuilder.hasArg(true)
                              .withArgName("author")
                              .withDescription("override for Artifact.Audit.author")
                              .withLongOpt("author")
                              .create("a"));
        options.addOption(OptionBuilder.hasArg(true)
                              .withArgName("version")
                              .withDescription("override for Artifact.Audit.version")
                              .withLongOpt("version")
                              .create("v"));
    }


    public final void parse(final String[] args) {
        final CommandLineParser parser = new PosixParser();
        try {
            final CommandLine result = parser.parse(options, args);
            extract(result);
        } catch (Exception e) {
            System.err.println("Parameter Error - "+e.getMessage());
            final PrintWriter pw = new PrintWriter(System.err);
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(pw, 80, " ", "", options, 1, 2, "");
            pw.close();
            System.exit(-1);
        }
    }


    protected void extract(final CommandLine results) {
        this.outputdir = results.getOptionValue("output", "-");
        this.filename = results.getOptionValue("filename", "proctor-tests-matrix.json");

        if (results.hasOption("author")) {
            this.author = results.getOptionValue("author");
        }
        if (results.hasOption("version")) {
            final String v = results.getOptionValue("version");
            if (v.length() > 0 && v.charAt(0) == 'r') { // support "svn-like" revisions like 'r149569'
                this.version = v.substring(1);
            } else {
                this.version = v;
            }
        }
    }

    public String getOutputdir() {
        return outputdir;
    }

    public String getFilename() {
        return filename;
    }

    public String getAuthor() {
        return author;
    }

    public String getVersion() {
        return version;
    }
}
