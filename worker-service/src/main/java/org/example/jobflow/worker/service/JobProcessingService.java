package org.example.jobflow.worker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.jobflow.common.enums.JobType;
import org.example.jobflow.worker.repository.JobRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class JobProcessingService {

    private final JobRepository jobRepository;
    private final JobStateService jobStateService;
    private final UrlCheckService urlCheckService;
    private final ObjectMapper objectMapper;

    public JobProcessingService(
            JobRepository jobRepository,
            JobStateService jobStateService,
            UrlCheckService urlCheckService,
            ObjectMapper objectMapper
    ) {
        this.jobRepository = jobRepository;
        this.jobStateService = jobStateService;
        this.urlCheckService = urlCheckService;
        this.objectMapper = objectMapper;
    }

    public void process(UUID jobId) throws Exception {
        if (!jobStateService.startProcessing(jobId)) {
            return;
        }

        var job = jobRepository.findById(jobId).orElseThrow();

        try {
            if (job.getType() != JobType.CHECK_URL) {
                throw new IllegalArgumentException("Unsupported job type");
            }

            String url = job.getPayload().path("url").asText();
            int statusCode = urlCheckService.check(url);

            var result = objectMapper.createObjectNode();
            result.put("url", url);
            result.put("statusCode", statusCode);

            jobStateService.complete(jobId, result);
        } catch (Exception e) {
            jobStateService.handleFailure(jobId, e.getMessage());
        }
    }
}