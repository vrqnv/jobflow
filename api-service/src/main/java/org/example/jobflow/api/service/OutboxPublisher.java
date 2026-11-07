package org.example.jobflow.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.jobflow.api.entity.OutboxEvent;
import org.example.jobflow.api.repository.OutboxRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(
            OutboxRepository outboxRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxRepository
                .findByPublishedAtIsNullAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                        Instant.now(),
                        PageRequest.of(0, 50)
                );

        for (OutboxEvent event : events) {
            try {
                String payload = objectMapper.writeValueAsString(event.getPayload());

                kafkaTemplate.send(
                        event.getTopic(),
                        event.getMessageKey(),
                        payload
                ).get(10, TimeUnit.SECONDS);

                event.markPublished();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                event.markFailed("Kafka publish interrupted");
                break;

            } catch (Exception e) {
                event.markFailed(e.getMessage());
            }
        }
    }
}
