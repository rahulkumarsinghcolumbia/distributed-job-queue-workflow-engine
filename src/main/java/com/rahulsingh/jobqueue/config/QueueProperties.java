package com.rahulsingh.jobqueue.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "queue")
public class QueueProperties {

    @Min(1)
    private int defaultVisibilityTimeoutSeconds = 60;

    @Min(1)
    private int maxAttempts = 5;

    public int getDefaultVisibilityTimeoutSeconds() {
        return defaultVisibilityTimeoutSeconds;
    }

    public void setDefaultVisibilityTimeoutSeconds(int defaultVisibilityTimeoutSeconds) {
        this.defaultVisibilityTimeoutSeconds = defaultVisibilityTimeoutSeconds;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
}
