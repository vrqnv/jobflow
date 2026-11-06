package org.example.jobflow.api.repository;

import org.example.jobflow.api.entity.Job;
import org.example.jobflow.common.enums.JobStatus;
import org.example.jobflow.common.enums.JobType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findByIdempotencyKey(String idempotencyKey);

    @Query("""
            SELECT j FROM Job j
            WHERE (:status IS NULL OR j.status = :status)
              AND (:type IS NULL OR j.type = :type)
            """)
    Page<Job> search(
            @Param("status") JobStatus status,
            @Param("type") JobType type,
            Pageable pageable
    );

    long countByStatus(JobStatus status);
}
