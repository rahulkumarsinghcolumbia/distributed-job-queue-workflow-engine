package com.rahulsingh.jobqueue.controller;

import com.rahulsingh.jobqueue.dto.AckRequest;
import com.rahulsingh.jobqueue.dto.DlqResponse;
import com.rahulsingh.jobqueue.dto.EnqueueRequest;
import com.rahulsingh.jobqueue.dto.EnqueueResponse;
import com.rahulsingh.jobqueue.dto.FailRequest;
import com.rahulsingh.jobqueue.dto.JobResponse;
import com.rahulsingh.jobqueue.dto.LeaseRequest;
import com.rahulsingh.jobqueue.dto.LeaseResponse;
import com.rahulsingh.jobqueue.entity.Job;
import com.rahulsingh.jobqueue.service.JobQueueService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@Validated
public class JobController {

    private final JobQueueService jobQueueService;

    public JobController(JobQueueService jobQueueService) {
        this.jobQueueService = jobQueueService;
    }

    @PostMapping("/enqueue")
    public ResponseEntity<EnqueueResponse> enqueue(@Valid @RequestBody EnqueueRequest request) {
        JobQueueService.EnqueueResult result = jobQueueService.enqueue(
                request.queueName(),
                request.payload(),
                request.idempotencyKey(),
                request.maxAttempts()
        );

        EnqueueResponse response = new EnqueueResponse(result.created(), JobResponse.from(result.job()));
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/lease")
    public LeaseResponse lease(@Valid @RequestBody LeaseRequest request) {
        return jobQueueService.lease(request.queueName(), request.visibilityTimeoutSeconds())
                .map(job -> new LeaseResponse(true, JobResponse.from(job)))
                .orElseGet(() -> new LeaseResponse(false, null));
    }

    @PostMapping("/{jobId}/ack")
    public JobResponse ack(@PathVariable UUID jobId, @Valid @RequestBody AckRequest request) {
        Job job = jobQueueService.ack(jobId, request.leaseToken());
        return JobResponse.from(job);
    }

    @PostMapping("/{jobId}/fail")
    public JobResponse fail(@PathVariable UUID jobId, @Valid @RequestBody FailRequest request) {
        Job job = jobQueueService.fail(
                jobId,
                request.leaseToken(),
                request.errorMessage(),
                request.retryDelaySeconds()
        );
        return JobResponse.from(job);
    }

    @GetMapping("/{jobId}")
    public JobResponse status(@PathVariable UUID jobId) {
        return JobResponse.from(jobQueueService.getStatus(jobId));
    }

    @GetMapping("/dlq")
    public DlqResponse dlq(
            @RequestParam @NotBlank String queueName,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size
    ) {
        Page<Job> result = jobQueueService.getDlq(queueName, page, size);
        List<JobResponse> jobs = result.getContent().stream().map(JobResponse::from).toList();
        return new DlqResponse(jobs, result.getTotalElements(), result.getTotalPages(), page, size);
    }

    @PostMapping("/{jobId}/dlq/requeue")
    public JobResponse requeueDlq(@PathVariable UUID jobId) {
        return JobResponse.from(jobQueueService.requeueDlq(jobId));
    }
}
