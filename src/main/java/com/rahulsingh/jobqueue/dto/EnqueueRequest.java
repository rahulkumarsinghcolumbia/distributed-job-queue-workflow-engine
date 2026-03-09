package com.rahulsingh.jobqueue.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnqueueRequest(
        @NotBlank @Size(max = 100) String queueName,
        @NotBlank String payload,
        @Size(max = 128) String idempotencyKey,
        @Min(1) Integer maxAttempts
) {
}
