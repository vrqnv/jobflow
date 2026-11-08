
package org.example.jobflow.worker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.jobflow.common.enums.JobType;
import org.example.jobflow.worker.repository.JobRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
            var result = switch (job.getType()) {
                case CHECK_URL -> {
                    String url = job.getPayload().path("url").asText();
                    int statusCode = urlCheckService.check(url);

                    var node = objectMapper.createObjectNode();
                    node.put("url", url);
                    node.put("statusCode", statusCode);
                    yield node;
                }
                case GENERATE_REPORT -> {
                    String reportName = job.getPayload()
                            .path("reportName").asText();

                    int delaySeconds = job.getPayload()
                            .path("delaySeconds").asInt(0);

                    Thread.sleep(delaySeconds * 1000L);

                    var node = objectMapper.createObjectNode();
                    node.put("reportName", reportName);
                    node.put("generatedAt", Instant.now().toString());
                    node.put("status", "GENERATED");
                    yield node;
                }
            };

            jobStateService.complete(jobId, result);
        } catch (Exception e) {
            jobStateService.handleFailure(jobId, e.getMessage());
        }
    }
}
