package org.example.jobflow.api.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.jobflow.api.entity.Job;
import org.example.jobflow.api.entity.JobEvent;
import org.example.jobflow.api.entity.OutboxEvent;
import org.example.jobflow.common.enums.JobStatus;
import org.example.jobflow.common.enums.JobType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryIntegrationTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobEventRepository jobEventRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSaveAndFindJob() throws Exception {
        UUID id = UUID.randomUUID();

        Job job = new Job(
                id,
                JobType.CHECK_URL,
                objectMapper.readTree("""
                        {"url": "https://example.com"}
                        """),
                3,
                "test-" + id
        );

        jobRepository.saveAndFlush(job);

        Job saved = jobRepository.findById(id).orElseThrow();

        assertThat(saved.getId()).isEqualTo(id);
        assertThat(saved.getType()).isEqualTo(JobType.CHECK_URL);
        assertThat(saved.getStatus()).isEqualTo(JobStatus.QUEUED);
        assertThat(saved.getPayload().get("url").asText())
                .isEqualTo("https://example.com");
    }

    @Test
    void shouldSearchJobsByStatusAndType() throws Exception {
        Job job = new Job(
                UUID.randomUUID(),
                JobType.CHECK_URL,
                objectMapper.readTree("""
                        {"url": "https://example.com"}
                        """),
                3,
                null
        );

        jobRepository.saveAndFlush(job);

        var result = jobRepository.search(
                JobStatus.QUEUED,
                JobType.CHECK_URL,
                PageRequest.of(0, 10)
        );

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getId())
                .isEqualTo(job.getId());
    }

    @Test
    void shouldSaveAndFindJobEvents() throws Exception {
        Job job = new Job(
                UUID.randomUUID(),
                JobType.CHECK_URL,
                objectMapper.readTree("""
                        {"url": "https://example.com"}
                        """),
                3,
                null
        );

        jobRepository.saveAndFlush(job);

        JobEvent event = new JobEvent(
                job,
                JobStatus.QUEUED,
                "Job created",
                objectMapper.readTree("{}")
        );

        jobEventRepository.saveAndFlush(event);

        var events = jobEventRepository
                .findByJob_IdOrderByCreatedAtAsc(job.getId());

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getStatus())
                .isEqualTo(JobStatus.QUEUED);
        assertThat(events.get(0).getMessage())
                .isEqualTo("Job created");
    }

    @Test
    void shouldFindPendingOutboxEvents() throws Exception {
        Instant now = Instant.now();

        UUID readyId = UUID.randomUUID();
        UUID futureId = UUID.randomUUID();

        OutboxEvent ready = new OutboxEvent(
                readyId,
                "jobs",
                UUID.randomUUID().toString(),
                objectMapper.readTree("""
                        {"message": "ready"}
                        """),
                now.minusSeconds(10)
        );

        OutboxEvent future = new OutboxEvent(
                futureId,
                "jobs-retry",
                UUID.randomUUID().toString(),
                objectMapper.readTree("""
                        {"message": "future"}
                        """),
                now.plusSeconds(60)
        );

        outboxRepository.saveAllAndFlush(
                java.util.List.of(ready, future)
        );

        var pending = outboxRepository
                .findByPublishedAtIsNullAndAvailableAtLessThanEqualOrderByCreatedAtAsc(
                        now,
                        PageRequest.of(0, 10)
                );

        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).getId()).isEqualTo(readyId);
        assertThat(outboxRepository.countByPublishedAtIsNull())
                .isEqualTo(2);
    }
}
