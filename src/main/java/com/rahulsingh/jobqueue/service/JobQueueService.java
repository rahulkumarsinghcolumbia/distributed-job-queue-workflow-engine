package com.rahulsingh.jobqueue.service;

import com.rahulsingh.jobqueue.config.QueueProperties;
import com.rahulsingh.jobqueue.entity.Job;
import com.rahulsingh.jobqueue.entity.JobStatus;
import com.rahulsingh.jobqueue.exception.InvalidJobStateException;
import com.rahulsingh.jobqueue.exception.JobNotFoundException;
import com.rahulsingh.jobqueue.exception.LeaseConflictException;
import com.rahulsingh.jobqueue.repository.JobRepository;
import com.rahulsingh.jobqueue.util.LeaseTokenGenerator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobQueueService {

    private final JobRepository jobRepository;
    private final QueueProperties queueProperties;
    private final LeaseTokenGenerator leaseTokenGenerator;

    public JobQueueService(JobRepository jobRepository,
                           QueueProperties queueProperties,
                           LeaseTokenGenerator leaseTokenGenerator) {
        this.jobRepository = jobRepository;
        this.queueProperties = queueProperties;
        this.leaseTokenGenerator = leaseTokenGenerator;
    }

    @Transactional
    public EnqueueResult enqueue(String queueName, String payload, String idempotencyKey, Integer maxAttemptsOverride) {
        String normalizedIdempotencyKey = normalizeOptional(idempotencyKey);

        if (normalizedIdempotencyKey != null) {
            Optional<Job> existing = jobRepository.findByIdempotencyKey(normalizedIdempotencyKey);
            if (existing.isPresent()) {
                return new EnqueueResult(existing.get(), false);
            }
        }

        Job job = new Job();
        job.setQueueName(queueName);
        job.setPayload(payload);
        job.setStatus(JobStatus.QUEUED);
        job.setAttemptCount(0);
        job.setMaxAttempts(maxAttemptsOverride == null ? queueProperties.getMaxAttempts() : maxAttemptsOverride);
        job.setVisibleAt(Instant.now());
        job.setIdempotencyKey(normalizedIdempotencyKey);

        try {
            Job saved = jobRepository.save(job);
            return new EnqueueResult(saved, true);
        } catch (DataIntegrityViolationException ex) {
            if (normalizedIdempotencyKey == null) {
                throw ex;
            }
            Job existing = jobRepository.findByIdempotencyKey(normalizedIdempotencyKey)
                    .orElseThrow(() -> ex);
            return new EnqueueResult(existing, false);
        }
    }

    @Transactional
    public Optional<Job> lease(String queueName, Integer visibilityTimeoutSecondsOverride) {
        int timeoutSeconds = visibilityTimeoutSecondsOverride == null
                ? queueProperties.getDefaultVisibilityTimeoutSeconds()
                : visibilityTimeoutSecondsOverride;

        Instant now = Instant.now();
        List<Job> jobs = jobRepository.lockNextAvailableJob(queueName, now);
        if (jobs.isEmpty()) {
            return Optional.empty();
        }

        Job job = jobs.getFirst();
        String leaseToken = leaseTokenGenerator.newToken();
        Instant leasedUntil = now.plusSeconds(timeoutSeconds);

        job.setStatus(JobStatus.LEASED);
        job.setAttemptCount(job.getAttemptCount() + 1);
        job.setLeaseToken(leaseToken);
        job.setLeasedUntil(leasedUntil);
        job.setVisibleAt(leasedUntil);

        return Optional.of(jobRepository.save(job));
    }

    @Transactional
    public Job ack(UUID jobId, String leaseToken) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        validateLease(job, leaseToken, Instant.now());

        job.setStatus(JobStatus.SUCCEEDED);
        job.setLeaseToken(null);
        job.setLeasedUntil(null);
        job.setVisibleAt(Instant.now());

        return jobRepository.save(job);
    }

    @Transactional
    public Job fail(UUID jobId, String leaseToken, String errorMessage, Integer retryDelaySeconds) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        Instant now = Instant.now();
        validateLease(job, leaseToken, now);

        job.setLastError(errorMessage);
        job.setLeaseToken(null);
        job.setLeasedUntil(null);

        if (job.getAttemptCount() >= job.getMaxAttempts()) {
            job.setStatus(JobStatus.DEAD_LETTER);
            job.setVisibleAt(now);
        } else {
            int retryDelay = retryDelaySeconds == null ? 0 : retryDelaySeconds;
            job.setStatus(JobStatus.QUEUED);
            job.setVisibleAt(now.plusSeconds(retryDelay));
        }

        return jobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public Job getStatus(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));
    }

    @Transactional(readOnly = true)
    public Page<Job> getDlq(String queueName, int page, int size) {
        return jobRepository.findByQueueNameAndStatusOrderByCreatedAtAsc(
                queueName,
                JobStatus.DEAD_LETTER,
                PageRequest.of(page, size)
        );
    }

    @Transactional
    public Job requeueDlq(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        if (job.getStatus() != JobStatus.DEAD_LETTER) {
            throw new InvalidJobStateException("Only DEAD_LETTER jobs can be requeued");
        }

        job.setStatus(JobStatus.QUEUED);
        job.setAttemptCount(0);
        job.setLeaseToken(null);
        job.setLeasedUntil(null);
        job.setVisibleAt(Instant.now());
        job.setLastError(null);

        return jobRepository.save(job);
    }

    private void validateLease(Job job, String leaseToken, Instant now) {
        if (job.getStatus() != JobStatus.LEASED) {
            throw new LeaseConflictException("Job is not currently leased");
        }
        if (job.getLeaseToken() == null || !job.getLeaseToken().equals(leaseToken)) {
            throw new LeaseConflictException("Lease token mismatch");
        }
        if (job.getLeasedUntil() == null || now.isAfter(job.getLeasedUntil())) {
            throw new LeaseConflictException("Lease has expired");
        }
    }

    private String normalizeOptional(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String trimmed = rawValue.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record EnqueueResult(Job job, boolean created) {
    }
}
