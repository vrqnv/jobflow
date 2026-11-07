package org.example.jobflow.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.jobflow.common.enums.JobStatus;

import java.time.Instant;

public record JobEventResponse(
        Long id,
        JobStatus status,
        String message,
        JsonNode metadata,
        Instant createdAt
) {
}