package org.example.jobflow.worker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.jobflow.common.enums.JobStatus;
import org.example.jobflow.common.event.JobRequestedEvent;
import org.example.jobflow.worker.entity.Job;
import org.example.jobflow.worker.entity.JobEvent;
import org.example.jobflow.worker.entity.OutboxEvent;
import org.example.jobflow.worker.repository.JobEventRepository;
import org.example.jobflow.worker.repository.JobRepository;
import org.example.jobflow.worker.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class JobStateService {

    private final JobRepository jobRepository;
    private final JobEventRepository jobEventRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public JobStateService(
            JobRepository jobRepository,
            JobEventRepository jobEventRepository,
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper
    ) {
        this.jobRepository = jobRepository;
        this.jobEventRepository = jobEventRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public boolean startProcessing(UUID jobId) {
        Job job = jobRepository.findById(jobId).orElse(null);

        if (job == null || job.getStatus() != JobStatus.QUEUED) {
            return false;
        }

        job.markProcessing();

        jobEventRepository.save(new JobEvent(
                job,
                JobStatus.PROCESSING,
                "Job processing started",
                null
        ));

        return true;
    }

    @Transactional
    public void complete(UUID jobId, JsonNode result) {
        Job job = jobRepository.findById(jobId).orElseThrow();

        job.markDone(result);

        jobEventRepository.save(new JobEvent(
                job,
                JobStatus.DONE,
                "Job completed",
                result
        ));
    }

    @Transactional
    public void handleFailure(UUID jobId, String error) {
        Job job = jobRepository.findById(jobId).orElseThrow();

        String message = error == null ? "Unknown error" : error;

        if (job.getAttempts() + 1 < job.getMaxAttempts()) {
            Instant retryAt = Instant.now().plusSeconds(5);
            job.markRetry(message, retryAt);

            jobEventRepository.save(new JobEvent(
                    job,
                    JobStatus.QUEUED,
                    "Job scheduled for retry",
                    null
            ));

            JobRequestedEvent event = new JobRequestedEvent(
                    UUID.randomUUID(),
                    jobId,
                    job.getType(),
                    job.getAttempts(),
                    retryAt
            );

            outboxEventRepository.save(new OutboxEvent(
                    event.messageId(),
                    "jobs",
                    jobId.toString(),
                    objectMapper.valueToTree(event),
                    retryAt
            ));
        } else {
            job.markFailed(message);

            jobEventRepository.save(new JobEvent(
                    job,
                    JobStatus.FAILED,
                    "Job failed",
                    null
            ));
        }
    }
}