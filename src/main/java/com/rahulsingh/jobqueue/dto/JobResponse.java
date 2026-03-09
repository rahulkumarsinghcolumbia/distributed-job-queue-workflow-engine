package com.rahulsingh.jobqueue.dto;

import com.rahulsingh.jobqueue.entity.Job;
import com.rahulsingh.jobqueue.entity.JobStatus;

import java.time.Instant;
import java.util.UUID;

public record JobResponse(
        UUID id,
        String queueName,
        String payload,
        JobStatus status,
        int attemptCount,
        int maxAttempts,
        Instant visibleAt,
        Instant leasedUntil,
        String leaseToken,
        String idempotencyKey,
        String lastError,
        Instant createdAt,
        Instant updatedAt
) {
    public static JobResponse from(Job job) {
        return new JobResponse(
                job.getId(),
                job.getQueueName(),
                job.getPayload(),
                job.getStatus(),
                job.getAttemptCount(),
                job.getMaxAttempts(),
                job.getVisibleAt(),
                job.getLeasedUntil(),
                job.getLeaseToken(),
                job.getIdempotencyKey(),
                job.getLastError(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
