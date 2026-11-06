package org.example.jobflow.common.event;

import org.example.jobflow.common.enums.JobType;

import java.time.Instant;
import java.util.UUID;

public record JobRequestedEvent(
        UUID messageId,
        UUID jobId,
        JobType jobType,
        int attempt,
        Instant occurredAt
) {
}