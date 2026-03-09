package com.rahulsingh.jobqueue.dto;

import jakarta.validation.constraints.NotBlank;

public record AckRequest(@NotBlank String leaseToken) {
}
