package org.example.jobflow.worker.repository;

import org.example.jobflow.common.enums.JobStatus;
import org.example.jobflow.worker.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findByIdAndStatus(UUID id, JobStatus status);
}
