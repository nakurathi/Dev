package com.navya.gcp.kafka.consumer;

import com.navya.gcp.domain.event.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for appointment domain events.
 *
 * <p>Uses {@code @RetryableTopic} for non-blocking retries:
 * <ul>
 *   <li>Retry attempt 1 → {@code healthcare.appointments.scheduled-retry-0}</li>
 *   <li>Retry attempt 2 → {@code healthcare.appointments.scheduled-retry-1}</li>
 *   <li>All retries exhausted → {@code healthcare.appointments.scheduled-dlt}</li>
 * </ul>
 *
 * <p>Non-blocking retries avoid pausing the main partition — other messages
 * continue to be processed while this one is retried on the retry topic.
 */
@Slf4j
@Component
public class AppointmentEventConsumer {

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            autoCreateTopics = "true",
            retryTopicSuffix = "-retry",
            dltTopicSuffix   = "-dlt"
    )
    @KafkaListener(
            topics = "healthcare.appointments.scheduled",
            groupId = "${spring.kafka.consumer.group-id:gcp-springboot-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onAppointmentScheduled(
            ConsumerRecord<String, DomainEvent> record,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        DomainEvent event = record.value();
        log.info("[APPOINTMENT_SCHEDULED] partition={} offset={} eventId={}",
                partition, offset, event.eventId());

        try {
            if (event instanceof DomainEvent.AppointmentScheduled e) {
                processScheduled(e);
            }
            ack.acknowledge();
            log.debug("ACKed appointment scheduled eventId={}", event.eventId());

        } catch (Exception ex) {
            log.error("Processing failed for eventId={}: {}", event.eventId(), ex.getMessage(), ex);
            throw ex; // Let @RetryableTopic handle retries
        }
    }

    @KafkaListener(
            topics = "healthcare.appointments.cancelled",
            groupId = "${spring.kafka.consumer.group-id:gcp-springboot-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onAppointmentCancelled(
            ConsumerRecord<String, DomainEvent> record,
            Acknowledgment ack) {

        DomainEvent event = record.value();
        log.info("[APPOINTMENT_CANCELLED] eventId={}", event.eventId());

        if (event instanceof DomainEvent.AppointmentCancelled e) {
            processCancelled(e);
        }
        ack.acknowledge();
    }

    /**
     * DLT listener — receives messages that exhausted all retries.
     * Log, alert, and store for manual intervention.
     */
    @KafkaListener(
            topics = "healthcare.appointments.scheduled-dlt",
            groupId = "${spring.kafka.consumer.group-id:gcp-springboot-group}-dlt"
    )
    public void onDeadLetter(ConsumerRecord<String, DomainEvent> record) {
        log.error("[DLT] Unrecoverable appointment event — manual intervention required. " +
                  "eventId={} key={}", record.value().eventId(), record.key());
        // TODO: store to GCS or alert PagerDuty
    }

    // ── Processing helpers ────────────────────────────────────────────────────

    private void processScheduled(DomainEvent.AppointmentScheduled event) {
        log.info("Sending confirmation for appointmentId={} patientId={}",
                event.appointmentId(), event.patientId());
        // TODO: trigger notification service (email/SMS via Pub/Sub)
    }

    private void processCancelled(DomainEvent.AppointmentCancelled event) {
        log.info("Processing cancellation appointmentId={} reason={}",
                event.appointmentId(), event.reason());
        // TODO: release doctor slot, trigger refund if billing attached
    }
}
