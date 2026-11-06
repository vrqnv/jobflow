package org.example.jobflow.api.repository;

import org.example.jobflow.api.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findByPublishedAtIsNullAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
            Instant now,
            Pageable pageable
    );

    long countByPublishedAtIsNull();
}
