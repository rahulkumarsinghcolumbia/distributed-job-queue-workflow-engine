package com.rahulsingh.jobqueue.dto;

public record EnqueueResponse(boolean created, JobResponse job) {
}
