
package org.example.jobflow.worker.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.example.jobflow.common.enums.JobStatus;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "job_events")
public class JobEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status;

    @Column(nullable = false)
    private String message;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected JobEvent() {
    }

    public JobEvent(
            Job job,
            JobStatus status,
            String message,
            JsonNode metadata
    ) {
        this.job = job;
        this.status = status;
        this.message = message;
        this.metadata = metadata;
        this.createdAt = Instant.now();
    }
}
