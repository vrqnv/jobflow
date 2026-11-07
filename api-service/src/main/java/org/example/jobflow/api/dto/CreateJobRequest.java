package org.example.jobflow.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import org.example.jobflow.common.enums.JobType;

public record CreateJobRequest(
        @NotNull JobType type,
        @NotNull JsonNode payload
) {
}