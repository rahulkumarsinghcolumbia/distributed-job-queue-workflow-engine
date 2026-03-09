package com.rahulsingh.jobqueue.entity;

public enum JobStatus {
    QUEUED,
    LEASED,
    SUCCEEDED,
    DEAD_LETTER
}
