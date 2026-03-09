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

## 12) Resume/Interview One-Liner

"Built a distributed job queue and workflow engine in Java/Spring Boot with worker leasing, visibility timeout, idempotent enqueue, retry/DLQ lifecycle, and operational recovery APIs for safe concurrent async processing."
