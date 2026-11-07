package org.example.jobflow.worker.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String topic;

    @Column(name = "message_key", nullable = false, length = 100)
    private String messageKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(UUID id, String topic, String messageKey,
                       JsonNode payload, Instant availableAt) {
        this.id = id;
        this.topic = topic;
        this.messageKey = messageKey;
        this.payload = payload.deepCopy();
        this.availableAt = availableAt;
        this.createdAt = Instant.now();
    }
}