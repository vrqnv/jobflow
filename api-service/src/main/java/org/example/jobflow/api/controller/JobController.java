package org.example.jobflow.api.controller;

import jakarta.validation.Valid;
import org.example.jobflow.api.dto.CreateJobRequest;
import org.example.jobflow.api.dto.JobEventResponse;
import org.example.jobflow.api.dto.JobPageResponse;
import org.example.jobflow.api.dto.JobResponse;
import org.example.jobflow.api.service.JobService;
import org.example.jobflow.common.enums.JobStatus;
import org.example.jobflow.common.enums.JobType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponse> createJob(
            @Valid @RequestBody CreateJobRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false)
            String idempotencyKey
    ) {
        JobResponse response = jobService.createJob(request, idempotencyKey);

        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJob(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(jobService.getJob(id));
    }

    @GetMapping
    public ResponseEntity<JobPageResponse> getJobs(
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) JobType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                jobService.getJobs(status, type, page, size)
        );
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<List<JobEventResponse>> getJobEvents(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(jobService.getJobEvents(id));
    }
}
