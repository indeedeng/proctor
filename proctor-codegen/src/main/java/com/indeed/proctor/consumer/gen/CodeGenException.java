package com.indeed.proctor.consumer.gen;

public class CodeGenException extends Exception {
    public CodeGenException(String message, Throwable throwable) {
        super(message, throwable);
    }
    public CodeGenException(String message) {
        super(message);
    }
}
