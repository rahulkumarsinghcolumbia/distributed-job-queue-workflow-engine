# Distributed Job Queue & Workflow Engine

A production-style backend system built with Java/Spring Boot for reliable asynchronous job processing.

## 1) What This Project Does (Simple)

This service accepts background jobs and allows workers to process them safely at scale.

- Producers create jobs (`enqueue`)
- Workers pick jobs (`lease`)
- Workers finish (`ack`) or fail (`fail`) jobs
- Failed jobs retry automatically
- Jobs that exceed retry limits move to a dead-letter queue (DLQ)
- Operators can inspect and requeue DLQ jobs

## End-to-End Flow Example (How It Works)

Example use case: send a welcome email after user signup.

1. Producer creates a job with `POST /api/v1/jobs/enqueue`.
Job is stored with `status=QUEUED`, `attemptCount=0`, `visibleAt=now`. If the same `idempotencyKey` is sent again, system returns existing job instead of creating a duplicate.

2. Worker polls with `POST /api/v1/jobs/lease`.
Database selects one available job using `FOR UPDATE SKIP LOCKED`. Job changes to `status=LEASED` and system sets `leaseToken` with `leasedUntil = now + visibilityTimeout`.

3. Worker processes payload.
If success, worker calls `POST /api/v1/jobs/{jobId}/ack` with lease token and job changes to `status=SUCCEEDED`.

4. If processing fails:
Worker calls `POST /api/v1/jobs/{jobId}/fail`. If attempts remain, job returns to `status=QUEUED` (with optional retry delay). If attempts are exhausted, job moves to `status=DEAD_LETTER`.

5. Operations recovery:
Team inspects DLQ with `GET /api/v1/jobs/dlq` and requeues dead-lettered jobs with `POST /api/v1/jobs/{jobId}/dlq/requeue`.

### How Concurrency Is Maintained

Example: Worker A and Worker B poll at the same time.

1. Both call `/lease` concurrently.
2. SQL locking (`FOR UPDATE SKIP LOCKED`) ensures only one worker locks the same row.
3. Suppose Worker A gets the job; Worker B skips that locked row and gets another job (or no job).
4. If Worker A crashes and does not `ack` before `leasedUntil`, the job becomes leasable again after timeout.

## 2) What This Project Demonstrates

This project demonstrates core distributed-systems/backend concepts:

- Safe concurrent processing with DB row locking (`FOR UPDATE SKIP LOCKED`)
- Visibility timeout and worker leasing semantics
- At-least-once processing with retries and DLQ recovery
- Idempotency key support to prevent duplicate creation on client retries
- Operational APIs for failure handling and reprocessing

## 3) Skill Set This Project Showcases

This project is designed to show these skills on your resume/interviews:

- Java + Spring Boot backend engineering
- API design for asynchronous workflows
- Database-driven concurrency control and transactional consistency
- Fault-tolerance patterns (retry, DLQ, recovery)
- Idempotency and write-safety in distributed systems
- Integration testing for lifecycle and concurrency behavior
- Developer experience (Docker setup, Swagger docs, runnable worker sample)

## 4) Tech Stack

- Java 21
- Spring Boot 3.4.x
- Spring Data JPA / Hibernate
- H2 (default local mode)
- PostgreSQL (production-like mode)
- OpenAPI/Swagger
- Docker + Docker Compose

## 5) Prerequisites

Required:

- JDK 21+
- Maven 3.9+

Optional (for PostgreSQL/Docker mode):

- Docker Desktop (or Docker Engine + Compose)

## 6) Quick Start (Local H2 Mode)

```bash
cd /Users/rahulsingh/Desktop/CourseWork/SPRING_2026/Side_Projects/distributed-job-queue-workflow-engine
mvn spring-boot:run
```

App starts on: `http://localhost:8080`

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## 7) Run with PostgreSQL (Production-like)

Option A: Run app + Postgres in containers

```bash
docker compose up --build
```

Option B: Run Postgres in Docker and app locally

```bash
docker compose up -d postgres
SPRING_PROFILES_ACTIVE=postgres mvn spring-boot:run
```

Default Postgres credentials used by compose:

- DB: `jobqueue`
- User: `jobqueue`
- Password: `jobqueue`
- Port: `5432`

## 8) Run Tests

```bash
mvn test
```

Current tests cover:

- Idempotent enqueue behavior
- Lease + ack lifecycle
- Visibility-timeout re-leasing
- DLQ move + requeue flow
- Concurrency safety (single job leased to only one worker)

## 9) API Endpoints

Base path: `/api/v1/jobs`

- `POST /enqueue`
- `POST /lease`
- `POST /{jobId}/ack`
- `POST /{jobId}/fail`
- `GET /{jobId}`
- `GET /dlq?queueName=...&page=0&size=20`
- `POST /{jobId}/dlq/requeue`

OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## 10) Sample Worker Client

A runnable polling worker is included:

- `src/main/java/com/rahulsingh/jobqueue/worker/SampleWorkerClient.java`

Run it:

```bash
mvn -Dexec.mainClass=com.rahulsingh.jobqueue.worker.SampleWorkerClient \
    -Dexec.args="http://localhost:8080 default 30 2" \
    exec:java
```

Arguments:

- `baseUrl` (default: `http://localhost:8080`)
- `queueName` (default: `default`)
- `visibilityTimeoutSeconds` (default: `30`)
- `pollIntervalSeconds` (default: `2`)

## 11) Additional Docs

- Concepts and implementation mapping: `docs/CONCEPTS_AND_SKILLS.md`
- Runbook with step-by-step workflows: `docs/RUNBOOK.md`
