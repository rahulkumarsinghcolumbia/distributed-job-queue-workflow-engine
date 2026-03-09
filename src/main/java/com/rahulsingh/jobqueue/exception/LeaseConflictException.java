package com.rahulsingh.jobqueue.exception;

public class LeaseConflictException extends RuntimeException {

    public LeaseConflictException(String message) {
        super(message);
    }
}
