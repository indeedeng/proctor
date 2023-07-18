package com.indeed.proctor.webapp.views;

/** Names of json views / jsp files in resources/META-INF/resources/WEB-INF/jsp */
public enum ProctorView {
    DETAILS("definition/details"),
    EDIT("definition/edit"),
    CREATE("definition/edit"),
    MATRIX_LIST("matrix/list"),
    MATRIX_USAGE("matrix/usage"),
    MATRIX_COMPATIBILITY("matrix/compatibility"),
    JOBS("jobs"),
    ERROR("error"),
/*checkstyle*/ ;

    private final String name;

    ProctorView(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
