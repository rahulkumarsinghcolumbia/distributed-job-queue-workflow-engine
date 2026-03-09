package com.rahulsingh.jobqueue.dto;

public record LeaseResponse(boolean leased, JobResponse job) {
}
