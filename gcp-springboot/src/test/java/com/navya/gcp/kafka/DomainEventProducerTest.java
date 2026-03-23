package com.navya.gcp.kafka;

import com.navya.gcp.domain.event.DomainEvent;
import com.navya.gcp.kafka.producer.DomainEventProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DomainEventProducerTest {

    @Mock KafkaTemplate<String, DomainEvent> kafkaTemplate;
    @InjectMocks DomainEventProducer producer;

    @SuppressWarnings("unchecked")
    private void mockKafkaSuccess() {
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
    }

    // ── Topic routing tests ───────────────────────────────────────────────────

    static Stream<Arguments> eventTopicProvider() {
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        return Stream.of(
            Arguments.of(
                new DomainEvent.AppointmentScheduled(id, now, "appt-1", "p-1", "d-1", now),
                "healthcare.appointments.scheduled"),
            Arguments.of(
                new DomainEvent.AppointmentCancelled(id, now, "appt-1", "p-1", "reason"),
                "healthcare.appointments.cancelled"),
            Arguments.of(
                new DomainEvent.PatientRegistered(id, now, "p-1", "Jane", "Doe", "j@d.com"),
                "healthcare.patients.registered"),
            Arguments.of(
                new DomainEvent.InvoiceGenerated(id, now, "inv-1", "p-1", BigDecimal.TEN, now),
                "healthcare.billing.invoices"),
            Arguments.of(
                new DomainEvent.PaymentReceived(id, now, "inv-1", "p-1", BigDecimal.TEN, now),
                "healthcare.billing.payments")
        );
    }

    @ParameterizedTest(name = "{0} → topic: {1}")
    @MethodSource("eventTopicProvider")
    @DisplayName("publish() routes each event type to correct Kafka topic")
    @SuppressWarnings("unchecked")
    void publish_routesToCorrectTopic(DomainEvent event, String expectedTopic) {
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        producer.publish(event);

        ArgumentCaptor<ProducerRecord<String, DomainEvent>> captor =
                ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());

        assertThat(captor.getValue().topic()).isEqualTo(expectedTopic);
    }

    @Test
    @DisplayName("publish() uses patientId as partition key for ordering")
    @SuppressWarnings("unchecked")
    void publish_usesPatientIdAsKey() {
        DomainEvent event = new DomainEvent.AppointmentScheduled(
                UUID.randomUUID().toString(), Instant.now(),
                "appt-1", "patient-XYZ", "doc-1", Instant.now());

        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        producer.publish(event);

        ArgumentCaptor<ProducerRecord<String, DomainEvent>> captor =
                ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());

        assertThat(captor.getValue().key()).isEqualTo("patient-XYZ");
    }

    // Helper to avoid cast warning in mock
    private static <T> T mock(Class<T> cls) {
        return org.mockito.Mockito.mock(cls);
    }
}
