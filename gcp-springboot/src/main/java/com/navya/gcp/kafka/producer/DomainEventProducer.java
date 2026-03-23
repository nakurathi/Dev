package com.navya.gcp.kafka.producer;

import com.navya.gcp.domain.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Publishes {@link DomainEvent} instances to appropriate Kafka topics.
 *
 * <p>Uses Java 17 sealed class + switch expression to route events
 * to topics without a single if/else chain.
 *
 * <p>Key strategy: partition by patientId so all events for a patient
 * are ordered on the same partition.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DomainEventProducer {

    private final KafkaTemplate<String, DomainEvent> kafkaTemplate;

    /**
     * Publishes a domain event to its corresponding Kafka topic.
     *
     * @param event the domain event to publish
     * @return CompletableFuture resolving to SendResult metadata
     */
    public CompletableFuture<SendResult<String, DomainEvent>> publish(DomainEvent event) {

        // Java 17 sealed switch — exhaustive, compiler-checked routing
        String topic = switch (event) {
            case DomainEvent.AppointmentScheduled e -> "healthcare.appointments.scheduled";
            case DomainEvent.AppointmentCancelled e -> "healthcare.appointments.cancelled";
            case DomainEvent.PatientRegistered    e -> "healthcare.patients.registered";
            case DomainEvent.InvoiceGenerated     e -> "healthcare.billing.invoices";
            case DomainEvent.PaymentReceived      e -> "healthcare.billing.payments";
        };

        // Partition key — all events for same patient go to same partition (ordering)
        String partitionKey = switch (event) {
            case DomainEvent.AppointmentScheduled e -> e.patientId();
            case DomainEvent.AppointmentCancelled e -> e.patientId();
            case DomainEvent.PatientRegistered    e -> e.patientId();
            case DomainEvent.InvoiceGenerated     e -> e.patientId();
            case DomainEvent.PaymentReceived      e -> e.patientId();
        };

        Headers headers = new RecordHeaders()
                .add(new RecordHeader("eventId",   event.eventId().getBytes(StandardCharsets.UTF_8)))
                .add(new RecordHeader("eventType", event.getClass().getSimpleName().getBytes(StandardCharsets.UTF_8)))
                .add(new RecordHeader("source",    "gcp-springboot".getBytes(StandardCharsets.UTF_8)));

        ProducerRecord<String, DomainEvent> record =
                new ProducerRecord<>(topic, null, event.occurredAt().toEpochMilli(),
                        partitionKey, event, headers);

        log.debug("Publishing event={} eventId={} topic={} key={}",
                event.getClass().getSimpleName(), event.eventId(), topic, partitionKey);

        return kafkaTemplate.send(record)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish eventId={} topic={}: {}",
                                event.eventId(), topic, ex.getMessage(), ex);
                    } else {
                        log.info("Published eventId={} topic={} partition={} offset={}",
                                event.eventId(), topic,
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
