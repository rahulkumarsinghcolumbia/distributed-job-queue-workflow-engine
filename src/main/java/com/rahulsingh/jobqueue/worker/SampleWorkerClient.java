package com.rahulsingh.jobqueue.worker;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class SampleWorkerClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        String baseUrl = args.length > 0 ? args[0] : "http://localhost:8080";
        String queueName = args.length > 1 ? args[1] : "default";
        int visibilityTimeoutSeconds = args.length > 2 ? Integer.parseInt(args[2]) : 30;
        int pollIntervalSeconds = args.length > 3 ? Integer.parseInt(args[3]) : 2;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        AtomicBoolean running = new AtomicBoolean(true);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Stopping sample worker...");
            running.set(false);
        }));

        System.out.printf(
                "Sample worker started. baseUrl=%s queue=%s visibility=%ss poll=%ss%n",
                baseUrl,
                queueName,
                visibilityTimeoutSeconds,
                pollIntervalSeconds
        );

        while (running.get()) {
            try {
                LeaseResponse lease = postJson(
                        httpClient,
                        baseUrl + "/api/v1/jobs/lease",
                        new LeaseRequest(queueName, visibilityTimeoutSeconds),
                        LeaseResponse.class
                );

                if (lease == null || !lease.leased || lease.job == null) {
                    Thread.sleep(Duration.ofSeconds(pollIntervalSeconds));
                    continue;
                }

                JobView job = lease.job;
                System.out.printf("Leased job id=%s attempt=%d/%d%n", job.id, job.attemptCount, job.maxAttempts);

                try {
                    processJob(job);
                    postJson(
                            httpClient,
                            baseUrl + "/api/v1/jobs/" + job.id + "/ack",
                            new AckRequest(job.leaseToken),
                            JobView.class
                    );
                    System.out.printf("Acked job id=%s%n", job.id);
                } catch (Exception processingError) {
                    postJson(
                            httpClient,
                            baseUrl + "/api/v1/jobs/" + job.id + "/fail",
                            new FailRequest(job.leaseToken, processingError.getMessage(), 5),
                            JobView.class
                    );
                    System.out.printf("Failed job id=%s reason=%s%n", job.id, processingError.getMessage());
                }
            } catch (Exception ex) {
                System.err.printf("Worker loop error: %s%n", ex.getMessage());
                Thread.sleep(Duration.ofSeconds(pollIntervalSeconds));
            }
        }
    }

    private static void processJob(JobView job) throws Exception {
        // Simulated work: if payload includes forceFail=true, send fail to exercise retry/DLQ behavior.
        if (job.payload != null && job.payload.contains("\"forceFail\":true")) {
            throw new RuntimeException("Simulated processing failure (forceFail=true)");
        }
        Thread.sleep(500);
    }

    private static <T> T postJson(HttpClient client, String url, Object requestBody, Class<T> responseType) throws Exception {
        String body = MAPPER.writeValueAsString(requestBody);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("HTTP " + response.statusCode() + " response=" + response.body());
        }

        if (responseType == Void.class || response.body() == null || response.body().isBlank()) {
            return null;
        }

        return MAPPER.readValue(response.body(), responseType);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LeaseResponse(boolean leased, JobView job) {
    }

    private record LeaseRequest(String queueName, int visibilityTimeoutSeconds) {
    }

    private record AckRequest(String leaseToken) {
    }

    private record FailRequest(String leaseToken, String errorMessage, Integer retryDelaySeconds) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record JobView(
            UUID id,
            String payload,
            String leaseToken,
            int attemptCount,
            int maxAttempts
    ) {
    }
}
