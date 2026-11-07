package org.example.jobflow.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.jobflow.common.enums.JobStatus;
import org.example.jobflow.common.enums.JobType;

import java.time.Instant;
import java.util.UUID;

public record JobResponse(
        UUID id,
        JobType type,
        JobStatus status,
        JsonNode payload,
        JsonNode result,
        String errorMessage,
        int attempts,
        int maxAttempts,
        Instant createdAt,
        Instant updatedAt
) {
}