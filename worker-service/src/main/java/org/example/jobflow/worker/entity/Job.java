package org.example.jobflow.worker.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.example.jobflow.common.enums.JobStatus;
import org.example.jobflow.common.enums.JobType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode result;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Job() {
    }

    public UUID getId() {
        return id;
    }

    public JobType getType() {
        return type;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public JobStatus getStatus() {
        return status;
    }

    public JsonNode getResult() {
        return result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void markProcessing() {
        this.status = JobStatus.PROCESSING;
    }

    public void markDone(JsonNode result) {
        this.status = JobStatus.DONE;
        this.result = result;
        this.errorMessage = null;
        this.nextAttemptAt = null;
    }

    public void markRetry(String errorMessage, Instant nextAttemptAt) {
        this.status = JobStatus.QUEUED;
        this.attempts++;
        this.errorMessage = errorMessage;
        this.nextAttemptAt = nextAttemptAt;
    }

    public void markFailed(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.attempts++;
        this.errorMessage = errorMessage;
        this.nextAttemptAt = null;
    }
}
