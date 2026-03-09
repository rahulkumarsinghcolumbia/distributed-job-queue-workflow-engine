package com.rahulsingh.jobqueue.repository;

import com.rahulsingh.jobqueue.entity.Job;
import com.rahulsingh.jobqueue.entity.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findByIdempotencyKey(String idempotencyKey);

    Page<Job> findByQueueNameAndStatusOrderByCreatedAtAsc(String queueName, JobStatus status, Pageable pageable);

    @Query(value = """
            SELECT *
            FROM jobs
            WHERE queue_name = :queueName
              AND ((status = 'QUEUED' AND visible_at <= :now)
                OR (status = 'LEASED' AND leased_until <= :now))
            ORDER BY created_at
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Job> lockNextAvailableJob(@Param("queueName") String queueName, @Param("now") Instant now);
}
