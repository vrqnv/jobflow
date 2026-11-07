package org.example.jobflow.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.jobflow.api.dto.JobEventResponse;
import org.example.jobflow.api.dto.JobPageResponse;
import org.example.jobflow.api.dto.JobResponse;
import org.example.jobflow.api.dto.CreateJobRequest;
import org.example.jobflow.api.entity.Job;
import org.example.jobflow.api.entity.JobEvent;
import org.example.jobflow.api.entity.OutboxEvent;
import org.example.jobflow.api.repository.JobEventRepository;
import org.example.jobflow.api.repository.JobRepository;
import org.example.jobflow.api.repository.OutboxRepository;
import org.example.jobflow.common.enums.JobStatus;
import org.example.jobflow.common.enums.JobType;
import org.example.jobflow.common.event.JobRequestedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final JobEventRepository jobEventRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final int maxAttempts;

    public JobService(
            JobRepository jobRepository,
            JobEventRepository jobEventRepository,
            OutboxRepository outboxRepository,
            ObjectMapper objectMapper,
            @Value("${jobflow.jobs.max-attempts:3}") int maxAttempts
    ) {
        this.jobRepository = jobRepository;
        this.jobEventRepository = jobEventRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.maxAttempts = maxAttempts;
    }

    @Transactional
    public JobResponse createJob(
            CreateJobRequest request,
            String idempotencyKey
    ) {
        validatePayload(request);

        if (maxAttempts < 1) {
            throw new IllegalStateException("Maximum attempts must be positive");
        }

        String key = normalizeIdempotencyKey(idempotencyKey);

        if (key != null) {
            var existing = jobRepository.findByIdempotencyKey(key);

            if (existing.isPresent()) {
                Job job = existing.get();

                if (job.getType() != request.type()
                        || !job.getPayload().equals(request.payload())) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Idempotency-Key was already used with another request"
                    );
                }

                return toResponse(job);
            }
        }

        UUID jobId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        Instant now = Instant.now();

        Job job = new Job(
                jobId,
                request.type(),
                request.payload(),
                maxAttempts,
                key
        );

        jobRepository.save(job);

        JobEvent jobEvent = new JobEvent(
                job,
                JobStatus.QUEUED,
                "Job created",
                objectMapper.createObjectNode()
        );

        jobEventRepository.save(jobEvent);

        JobRequestedEvent event = new JobRequestedEvent(
                messageId,
                jobId,
                request.type(),
                0,
                now
        );

        OutboxEvent outboxEvent = new OutboxEvent(
                messageId,
                "jobs",
                jobId.toString(),
                objectMapper.valueToTree(event),
                now
        );

        outboxRepository.save(outboxEvent);

        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(UUID id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException("Job not found: " + id)
                );

        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public JobPageResponse getJobs(
            JobStatus status,
            JobType type,
            int page,
            int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Invalid pagination parameters");
        }

        Page<Job> result = jobRepository.search(
                status,
                type,
                PageRequest.of(page, size)
        );

        List<JobResponse> jobs = result.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new JobPageResponse(
                jobs,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public List<JobEventResponse> getJobEvents(UUID jobId) {
        if (!jobRepository.existsById(jobId)) {
            throw new NoSuchElementException("Job not found: " + jobId);
        }

        return jobEventRepository
                .findByJob_IdOrderByCreatedAtAsc(jobId)
                .stream()
                .map(event -> new JobEventResponse(
                        event.getId(),
                        event.getStatus(),
                        event.getMessage(),
                        event.getMetadata(),
                        event.getCreatedAt()
                ))
                .toList();
    }

    private void validatePayload(CreateJobRequest request) {
        if (request == null || request.type() == null) {
            throw new IllegalArgumentException("Job type is required");
        }

        JsonNode payload = request.payload();

        if (payload == null || !payload.isObject()) {
            throw new IllegalArgumentException("Payload must be a JSON object");
        }

        switch (request.type()) {
            case CHECK_URL -> validateUrlPayload(payload);
            case GENERATE_REPORT -> validateReportPayload(payload);
        }
    }

    private void validateUrlPayload(JsonNode payload) {
        JsonNode urlNode = payload.get("url");

        if (urlNode == null || !urlNode.isTextual()
                || urlNode.asText().isBlank()) {
            throw unprocessable("A valid URL is required");
        }

        URI uri;

        try {
            uri = URI.create(urlNode.asText());
        } catch (IllegalArgumentException ex) {
            throw unprocessable("Invalid URL");
        }

        if (uri.getHost() == null
                || !("http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme()))) {
            throw unprocessable("Only HTTP and HTTPS URLs are supported");
        }
    }

    private void validateReportPayload(JsonNode payload) {
        JsonNode reportName = payload.get("reportName");

        if (reportName == null || !reportName.isTextual()
                || reportName.asText().isBlank()) {
            throw unprocessable("Report name is required");
        }

        JsonNode delay = payload.get("delaySeconds");

        if (delay != null && (
                !delay.isIntegralNumber()
                        || !delay.canConvertToInt()
                        || delay.intValue() < 0
                        || delay.intValue() > 30
        )) {
            throw unprocessable("delaySeconds must be between 0 and 30");
        }
    }

    private String normalizeIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }

        String normalized = key.trim();

        if (normalized.length() > 128) {
            throw new IllegalArgumentException(
                    "Idempotency-Key cannot exceed 128 characters"
            );
        }

        return normalized;
    }

    private JobResponse toResponse(Job job) {
        return new JobResponse(
                job.getId(),
                job.getType(),
                job.getStatus(),
                job.getPayload(),
                job.getResult(),
                job.getErrorMessage(),
                job.getAttempts(),
                job.getMaxAttempts(),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }

    private ResponseStatusException unprocessable(String message) {
        return new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                message
        );
    }
}
