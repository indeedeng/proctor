package com.indeed.proctor.common;

import java.io.IOException;

public class MissingTestMatrixException extends IOException {
    public MissingTestMatrixException(final String message) {
        super(message);
    }

    private static final long serialVersionUID = 3936507131275504415L;
}