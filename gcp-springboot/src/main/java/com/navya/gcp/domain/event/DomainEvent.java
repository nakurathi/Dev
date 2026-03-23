package com.navya.gcp.domain.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.math.BigDecimal;

/**
 * Sealed hierarchy of domain events published to Kafka topics.
 *
 * Uses Java 17 sealed classes + records for:
 * - Compile-time exhaustive pattern matching
 * - Immutable, concise event payloads
 * - Type-safe Jackson polymorphic deserialization
 *
 * Usage in switch:
 * <pre>
 *   String topic = switch (event) {
 *       case AppointmentScheduled e -> "appointments.scheduled";
 *       case AppointmentCancelled e -> "appointments.cancelled";
 *       case PatientRegistered   e -> "patients.registered";
 *       case InvoiceGenerated    e -> "billing.invoices";
 *       case PaymentReceived     e -> "billing.payments";
 *   };
 * </pre>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = DomainEvent.AppointmentScheduled.class, name = "APPOINTMENT_SCHEDULED"),
    @JsonSubTypes.Type(value = DomainEvent.AppointmentCancelled.class, name = "APPOINTMENT_CANCELLED"),
    @JsonSubTypes.Type(value = DomainEvent.PatientRegistered.class,   name = "PATIENT_REGISTERED"),
    @JsonSubTypes.Type(value = DomainEvent.InvoiceGenerated.class,    name = "INVOICE_GENERATED"),
    @JsonSubTypes.Type(value = DomainEvent.PaymentReceived.class,     name = "PAYMENT_RECEIVED"),
})
public sealed interface DomainEvent
        permits DomainEvent.AppointmentScheduled,
                DomainEvent.AppointmentCancelled,
                DomainEvent.PatientRegistered,
                DomainEvent.InvoiceGenerated,
                DomainEvent.PaymentReceived {

    String eventId();
    Instant occurredAt();

    // ── Appointment events ────────────────────────────────────────────────────

    record AppointmentScheduled(
            String eventId,
            Instant occurredAt,
            String appointmentId,
            String patientId,
            String doctorId,
            Instant scheduledAt
    ) implements DomainEvent {}

    record AppointmentCancelled(
            String eventId,
            Instant occurredAt,
            String appointmentId,
            String patientId,
            String reason
    ) implements DomainEvent {}

    // ── Patient events ────────────────────────────────────────────────────────

    record PatientRegistered(
            String eventId,
            Instant occurredAt,
            String patientId,
            String firstName,
            String lastName,
            String email
    ) implements DomainEvent {}

    // ── Billing events ────────────────────────────────────────────────────────

    record InvoiceGenerated(
            String eventId,
            Instant occurredAt,
            String invoiceId,
            String patientId,
            BigDecimal amount,
            Instant dueDate
    ) implements DomainEvent {}

    record PaymentReceived(
            String eventId,
            Instant occurredAt,
            String invoiceId,
            String patientId,
            BigDecimal amountPaid,
            Instant paidAt
    ) implements DomainEvent {}
}
