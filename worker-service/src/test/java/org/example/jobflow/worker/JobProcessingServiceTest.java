package org.example.jobflow.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.jobflow.common.enums.JobType;
import org.example.jobflow.worker.entity.Job;
import org.example.jobflow.worker.repository.JobRepository;
import org.example.jobflow.worker.service.JobProcessingService;
import org.example.jobflow.worker.service.JobStateService;
import org.example.jobflow.worker.service.UrlCheckService;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobProcessingServiceTest {

    @Test
    void shouldGenerateReport() throws Exception {
        UUID jobId = UUID.randomUUID();

        JobRepository jobRepository = mock(JobRepository.class);
        JobStateService jobStateService = mock(JobStateService.class);
        UrlCheckService urlCheckService = mock(UrlCheckService.class);
        ObjectMapper objectMapper = new ObjectMapper();

        Job job = mock(Job.class);

        JsonNode payload = objectMapper.readTree("""
                {
                    "reportName": "Monthly Sales",
                    "delaySeconds": 0
                }
                """);

        when(jobStateService.startProcessing(jobId)).thenReturn(true);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(job.getType()).thenReturn(JobType.GENERATE_REPORT);
        when(job.getPayload()).thenReturn(payload);

        JobProcessingService service = new JobProcessingService(
                jobRepository,
                jobStateService,
                urlCheckService,
                objectMapper
        );

        service.process(jobId);

        var resultCaptor =
                org.mockito.ArgumentCaptor.forClass(JsonNode.class);

        verify(jobStateService).complete(
                eq(jobId),
                resultCaptor.capture()
        );

        JsonNode result = resultCaptor.getValue();

        assertEquals("Monthly Sales", result.path("reportName").asText());
        assertEquals("GENERATED", result.path("status").asText());
        assertTrue(result.hasNonNull("generatedAt"));

        verify(jobStateService, never()).handleFailure(any(), any());
        verifyNoInteractions(urlCheckService);
    }
}
