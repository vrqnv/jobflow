package org.example.jobflow.worker.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class JobRequestedConsumer {

    @KafkaListener(topics = "jobs")
    public void consume(String message) {
        System.out.println("Received job event: " + message);
    }
}