package com.indeed.proctor.common;

/**
* @author matts
*/
public class InvalidRuleException extends Exception {
    public InvalidRuleException(final String message) {
        super(message);
    }

    public InvalidRuleException(final Throwable throwable, final String message) {
        super(message, throwable);
    }

    private static final long serialVersionUID = 2401266961303036201L;
}
