package org.example.jobflow.worker.repository;

import org.example.jobflow.worker.entity.JobEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobEventRepository extends JpaRepository<JobEvent, Long> {
}