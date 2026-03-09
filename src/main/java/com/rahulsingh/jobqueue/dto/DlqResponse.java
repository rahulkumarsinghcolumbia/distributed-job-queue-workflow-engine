package com.rahulsingh.jobqueue.dto;

import java.util.List;

public record DlqResponse(
        List<JobResponse> jobs,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
}
