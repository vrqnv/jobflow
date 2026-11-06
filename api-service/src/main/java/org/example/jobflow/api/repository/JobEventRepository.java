package org.example.jobflow.api.repository;

import org.example.jobflow.api.entity.JobEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobEventRepository extends JpaRepository<JobEvent, Long> {

    List<JobEvent> findByJob_IdOrderByCreatedAtAsc(UUID jobId);
}
