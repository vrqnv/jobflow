package org.example.jobflow.worker.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.jobflow.common.event.JobRequestedEvent;
import org.example.jobflow.worker.service.JobProcessingService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class JobRequestedConsumer {

    private final ObjectMapper objectMapper;
    private final JobProcessingService jobProcessingService;

    public JobRequestedConsumer(
            ObjectMapper objectMapper,
            JobProcessingService jobProcessingService
    ) {
        this.objectMapper = objectMapper;
        this.jobProcessingService = jobProcessingService;
    }

    @KafkaListener(topics = "jobs")
    public void consume(String message) throws Exception {
        JobRequestedEvent event = objectMapper.readValue(
                message,
                JobRequestedEvent.class
        );

        jobProcessingService.process(event.jobId());
    }
}