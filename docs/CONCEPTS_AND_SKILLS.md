# Concepts and Skills Mapping

## Project Goal

Provide a reliable asynchronous processing backend where multiple workers can process jobs concurrently without duplicate ownership and with operational recovery support.

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
