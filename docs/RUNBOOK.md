# Runbook

## 1) Start in local mode (H2)

```bash
cd /Users/rahulsingh/Desktop/CourseWork/SPRING_2026/Side_Projects/distributed-job-queue-workflow-engine
mvn spring-boot:run
```

Check health manually:

- Open Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Open API JSON: `http://localhost:8080/v3/api-docs`

## 2) Start in PostgreSQL mode

All services in Docker:

```bash
docker compose up --build
```

Or PostgreSQL only + app local:

```bash
docker compose up -d postgres
SPRING_PROFILES_ACTIVE=postgres mvn spring-boot:run
```

## 3) Execute a full lifecycle with cURL

### Step A: enqueue

```bash
curl -X POST http://localhost:8080/api/v1/jobs/enqueue \
  -H "Content-Type: application/json" \
  -d '{
    "queueName": "email",
    "payload": "{\"to\":\"user@site.com\",\"template\":\"welcome\"}",
    "idempotencyKey": "email-job-1",
    "maxAttempts": 3
  }'
```

Save returned `job.id`.

### Step B: lease

```bash
curl -X POST http://localhost:8080/api/v1/jobs/lease \
  -H "Content-Type: application/json" \
  -d '{"queueName":"email","visibilityTimeoutSeconds":30}'
```

Save returned `job.leaseToken`.

### Step C1: ack success

```bash
curl -X POST http://localhost:8080/api/v1/jobs/<jobId>/ack \
  -H "Content-Type: application/json" \
  -d '{"leaseToken":"<leaseToken>"}'
```

### Step C2: fail and retry

```bash
curl -X POST http://localhost:8080/api/v1/jobs/<jobId>/fail \
  -H "Content-Type: application/json" \
  -d '{"leaseToken":"<leaseToken>","errorMessage":"temporary error","retryDelaySeconds":5}'
```

### Step D: check status

```bash
curl http://localhost:8080/api/v1/jobs/<jobId>
```

### Step E: inspect DLQ

```bash
curl "http://localhost:8080/api/v1/jobs/dlq?queueName=email&page=0&size=20"
```

### Step F: requeue from DLQ

```bash
curl -X POST http://localhost:8080/api/v1/jobs/<jobId>/dlq/requeue
```

## 4) Run sample worker

```bash
mvn -Dexec.mainClass=com.rahulsingh.jobqueue.worker.SampleWorkerClient \
    -Dexec.args="http://localhost:8080 email 30 2" \
    exec:java
```

To simulate failure behavior, enqueue payload containing `"forceFail":true`.

## 5) Run tests

```bash
mvn test
```

## 6) Concurrency demo (two workers leasing together)

1. Enqueue one job in queue `concurrency-demo`.
2. Open two terminals and run lease call at nearly same time:

```bash
curl -X POST http://localhost:8080/api/v1/jobs/lease \
  -H "Content-Type: application/json" \
  -d '{"queueName":"concurrency-demo","visibilityTimeoutSeconds":30}'
```

Expected behavior:

- One terminal gets `leased=true` with job details.
- Other terminal gets `leased=false` or a different job.
- This demonstrates row-level lock behavior with `FOR UPDATE SKIP LOCKED`.
