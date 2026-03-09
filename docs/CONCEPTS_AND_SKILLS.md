# Concepts and Skills Mapping

## Project Goal

Provide a reliable asynchronous processing backend where multiple workers can process jobs concurrently without duplicate ownership and with operational recovery support.

## Example Flow (Job Creation to Processing)

Use case: signup event triggers welcome email job.

1. API client enqueues job with queue name `email` and payload.
2. System saves job as `QUEUED` and marks it visible immediately.
3. Worker polls `/lease` and receives job with `leaseToken` and `leasedUntil`.
4. Worker executes business logic.
5. Success path: worker calls `/ack`, job becomes `SUCCEEDED`.
6. Failure path: worker calls `/fail`; job is retried or moved to `DEAD_LETTER` when attempts are exhausted.
7. Operations path: team checks DLQ and can requeue dead-lettered jobs.

## Example Concurrency Scenario

1. Worker A and Worker B call `/lease` at the same time.
2. Query uses `FOR UPDATE SKIP LOCKED`, so only one worker locks a specific row.
3. Worker A gets Job-101; Worker B cannot lease Job-101 at that moment.
4. If Worker A crashes and lease expires, Job-101 is visible again and can be leased by Worker B (or another worker).

## Concept -> Implementation

1. Worker leasing
- Concept: A worker temporarily owns a job while processing it.
- Implementation: `lease` sets `status=LEASED`, `leaseToken`, `leasedUntil`.
- Why it matters: Prevents two workers from processing the same job at once.

2. Visibility timeout
- Concept: If a worker crashes or stalls, job becomes available again.
- Implementation: Leased jobs are re-eligible when `leased_until <= now`.
- Why it matters: Enables automatic recovery from worker failure.

3. Safe concurrency control
- Concept: Multiple workers can poll at same time without races.
- Implementation: SQL uses `FOR UPDATE SKIP LOCKED` when selecting next job.
- Why it matters: Ensures one active lease per job under contention.

4. Retry + DLQ
- Concept: Failed jobs retry; permanently failing jobs move to DLQ.
- Implementation: `fail` increments attempts and moves to `DEAD_LETTER` at max attempts.
- Why it matters: Prevents infinite retry loops and preserves failed work for ops.

5. Idempotency key
- Concept: Client retries should not create duplicate jobs.
- Implementation: Unique idempotency key + conflict-safe enqueue handling.
- Why it matters: Makes write path safe in real networks with retries/timeouts.

6. Operational recovery APIs
- Concept: Production teams need visibility and control.
- Implementation: Status API + DLQ inspect + DLQ requeue endpoint.
- Why it matters: Faster incident response and lower data loss risk.

## Skill Areas Demonstrated

- Backend system design (async workflows)
- Spring Boot service architecture
- API contract design and validation
- JPA/SQL concurrency patterns
- Transaction boundaries and data consistency
- Failure handling and recovery design
- Integration testing strategy
- DevOps readiness (Docker, profile-based config, docs)
