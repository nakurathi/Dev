package com.navya.gcp.integration;

import com.navya.gcp.domain.event.DomainEvent;
import com.navya.gcp.kafka.producer.DomainEventProducer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: Spring Boot context + embedded Kafka broker.
 *
 * <p>Uses {@code @EmbeddedKafka} to start a real in-process Kafka broker,
 * then publishes a domain event and asserts it is received by a test consumer.
 *
 * <p>Verifies the full Kafka pipeline:
 * Producer → Serialization → Broker → Deserialization → Consumer
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext
@EmbeddedKafka(
    partitions = 1,
    topics = {
        "healthcare.appointments.scheduled",
        "healthcare.appointments.cancelled",
        "healthcare.patients.registered",
        "healthcare.billing.invoices",
        "healthcare.billing.payments"
    }
)
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
    "spring.cloud.gcp.project-id=test-project",
    "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://mock-jwks"
})
class KafkaIntegrationTest {

    @Autowired
    private DomainEventProducer eventProducer;

    // Shared queue between the test listener and assertions
    private static final BlockingQueue<ConsumerRecord<String, DomainEvent>> received =
            new LinkedBlockingQueue<>();

    @KafkaListener(
        topics = "healthcare.appointments.scheduled",
        groupId = "integration-test-group"
    )
    public void testListener(ConsumerRecord<String, DomainEvent> record) {
        received.add(record);
    }

    @Test
    @DisplayName("Published AppointmentScheduled event is consumed from embedded Kafka")
    void publishAndConsume_appointmentScheduled() throws InterruptedException {
        received.clear();

        DomainEvent event = new DomainEvent.AppointmentScheduled(
                UUID.randomUUID().toString(),
                Instant.now(),
                "appt-integration-001",
                "patient-integration-001",
                "doctor-001",
                Instant.now().plusSeconds(3600)
        );

        eventProducer.publish(event);

        // Wait up to 5 seconds for the message to arrive
        ConsumerRecord<String, DomainEvent> record = received.poll(5, TimeUnit.SECONDS);

        assertThat(record).isNotNull();
        assertThat(record.topic()).isEqualTo("healthcare.appointments.scheduled");
        assertThat(record.key()).isEqualTo("patient-integration-001");

        DomainEvent consumed = record.value();
        assertThat(consumed).isInstanceOf(DomainEvent.AppointmentScheduled.class);

        DomainEvent.AppointmentScheduled scheduled = (DomainEvent.AppointmentScheduled) consumed;
        assertThat(scheduled.appointmentId()).isEqualTo("appt-integration-001");
        assertThat(scheduled.patientId()).isEqualTo("patient-integration-001");
    }
}
