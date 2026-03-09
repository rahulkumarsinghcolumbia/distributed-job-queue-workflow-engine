package com.rahulsingh.jobqueue;

import com.rahulsingh.jobqueue.entity.Job;
import com.rahulsingh.jobqueue.entity.JobStatus;
import com.rahulsingh.jobqueue.service.JobQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JobQueueServiceIntegrationTest {

    @Autowired
    private JobQueueService service;

    @BeforeEach
    void resetData(@Autowired com.rahulsingh.jobqueue.repository.JobRepository jobRepository) {
        jobRepository.deleteAll();
    }

    @Test
    void enqueueShouldBeIdempotentByKey() {
        JobQueueService.EnqueueResult first = service.enqueue("email", "{\"to\":\"a@test.com\"}", "idem-1", null);
        JobQueueService.EnqueueResult second = service.enqueue("email", "{\"to\":\"a@test.com\"}", "idem-1", null);

        assertThat(first.created()).isTrue();
        assertThat(second.created()).isFalse();
        assertThat(second.job().getId()).isEqualTo(first.job().getId());
    }

    @Test
    void shouldLeaseAndAckJob() {
        UUID jobId = service.enqueue("payments", "{\"orderId\":123}", null, null).job().getId();

        Job leased = service.lease("payments", 5).orElseThrow();
        assertThat(leased.getId()).isEqualTo(jobId);
        assertThat(leased.getStatus()).isEqualTo(JobStatus.LEASED);
        assertThat(leased.getLeaseToken()).isNotBlank();

        Job acked = service.ack(jobId, leased.getLeaseToken());
        assertThat(acked.getStatus()).isEqualTo(JobStatus.SUCCEEDED);
        assertThat(acked.getLeaseToken()).isNull();
    }

    @Test
    void expiredLeaseShouldBecomeAvailableAgain() throws Exception {
        UUID jobId = service.enqueue("video", "{\"task\":\"render\"}", null, null).job().getId();

        Job firstLease = service.lease("video", 1).orElseThrow();
        assertThat(firstLease.getId()).isEqualTo(jobId);

        Thread.sleep(1200);

        Job secondLease = service.lease("video", 5).orElseThrow();
        assertThat(secondLease.getId()).isEqualTo(jobId);
        assertThat(secondLease.getAttemptCount()).isEqualTo(2);
        assertThat(secondLease.getLeaseToken()).isNotEqualTo(firstLease.getLeaseToken());
    }

    @Test
    void shouldMoveToDlqAndAllowRequeue() {
        UUID jobId = service.enqueue("imports", "{\"file\":\"customers.csv\"}", null, 1).job().getId();

        Job leased = service.lease("imports", 5).orElseThrow();
        Job failed = service.fail(jobId, leased.getLeaseToken(), "parse error", 0);
        assertThat(failed.getStatus()).isEqualTo(JobStatus.DEAD_LETTER);

        assertThat(service.getDlq("imports", 0, 10).getTotalElements()).isEqualTo(1);

        Job requeued = service.requeueDlq(jobId);
        assertThat(requeued.getStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(requeued.getAttemptCount()).isZero();

        Optional<Job> leasedAgain = service.lease("imports", 5);
        assertThat(leasedAgain).isPresent();
        assertThat(leasedAgain.orElseThrow().getStatus()).isEqualTo(JobStatus.LEASED);
    }

    @Test
    void shouldLeaseJobToOnlyOneWorkerUnderConcurrency() throws Exception {
        UUID jobId = service.enqueue("concurrency", "{\"job\":1}", null, null).job().getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<Optional<Job>> worker1 = executor.submit(() -> {
            ready.countDown();
            start.await();
            return service.lease("concurrency", 10);
        });

        Future<Optional<Job>> worker2 = executor.submit(() -> {
            ready.countDown();
            start.await();
            return service.lease("concurrency", 10);
        });

        assertThat(ready.await(2, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        Optional<Job> lease1 = worker1.get();
        Optional<Job> lease2 = worker2.get();
        executor.shutdownNow();

        long leasedCount = (lease1.isPresent() ? 1 : 0) + (lease2.isPresent() ? 1 : 0);
        assertThat(leasedCount).isEqualTo(1);

        Job leasedJob = lease1.orElseGet(() -> lease2.orElseThrow());
        assertThat(leasedJob.getId()).isEqualTo(jobId);
    }
}
