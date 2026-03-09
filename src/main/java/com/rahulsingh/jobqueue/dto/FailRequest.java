package com.rahulsingh.jobqueue.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record FailRequest(
        @NotBlank String leaseToken,
        @NotBlank String errorMessage,
        @Min(0) Integer retryDelaySeconds
) {
}
