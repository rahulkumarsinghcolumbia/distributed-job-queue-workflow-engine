package com.rahulsingh.jobqueue.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LeaseRequest(
        @NotBlank @Size(max = 100) String queueName,
        @Min(1) Integer visibilityTimeoutSeconds
) {
}
