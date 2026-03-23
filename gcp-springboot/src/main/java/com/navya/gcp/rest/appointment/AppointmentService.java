package com.navya.gcp.rest.appointment;

import com.navya.gcp.domain.dto.Dtos.*;
import com.navya.gcp.domain.event.DomainEvent;
import com.navya.gcp.domain.model.Appointment;
import com.navya.gcp.kafka.producer.DomainEventProducer;
import com.google.cloud.spring.data.spanner.repository.SpannerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Appointment service — orchestrates:
 * <ol>
 *   <li>Persist appointment to Cloud Spanner</li>
 *   <li>Publish {@link DomainEvent} to Kafka for downstream services</li>
 * </ol>
 *
 * <p>Demonstrates the outbox-lite pattern: write + publish in same method,
 * relying on Kafka's idempotent producer for at-least-once delivery.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DomainEventProducer eventProducer;

    @Transactional
    public AppointmentResponse schedule(AppointmentRequest request) {
        Appointment appointment = Appointment.builder()
                .appointmentId(UUID.randomUUID().toString())
                .patientId(request.patientId())
                .doctorId(request.doctorId())
                .scheduledAt(request.scheduledAt())
                .notes(request.notes())
                .status("SCHEDULED")
                .createdAt(Instant.now())
                .build();

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Appointment scheduled: {}", saved.getAppointmentId());

        // Publish domain event to Kafka
        DomainEvent event = new DomainEvent.AppointmentScheduled(
                UUID.randomUUID().toString(),
                Instant.now(),
                saved.getAppointmentId(),
                saved.getPatientId(),
                saved.getDoctorId(),
                saved.getScheduledAt()
        );
        eventProducer.publish(event);

        return toResponse(saved);
    }

    @Transactional
    public AppointmentResponse updateStatus(String appointmentId, AppointmentStatusUpdate update) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));

        appt.setStatus(update.status());
        appt.setUpdatedAt(Instant.now());
        Appointment updated = appointmentRepository.save(appt);

        if ("CANCELLED".equals(update.status())) {
            DomainEvent event = new DomainEvent.AppointmentCancelled(
                    UUID.randomUUID().toString(),
                    Instant.now(),
                    updated.getAppointmentId(),
                    updated.getPatientId(),
                    update.reason()
            );
            eventProducer.publish(event);
        }
        return toResponse(updated);
    }

    public Optional<AppointmentResponse> findById(String appointmentId) {
        return appointmentRepository.findById(appointmentId).map(this::toResponse);
    }

    public List<AppointmentResponse> findByPatient(String patientId) {
        return appointmentRepository.findByPatientId(patientId)
                .stream().map(this::toResponse).toList();
    }

    private AppointmentResponse toResponse(Appointment a) {
        return new AppointmentResponse(
                a.getAppointmentId(), a.getPatientId(), a.getDoctorId(),
                a.getScheduledAt(), a.getStatus(), a.getNotes(), a.getCreatedAt());
    }

    public static class AppointmentNotFoundException extends RuntimeException {
        public AppointmentNotFoundException(String id) {
            super("Appointment not found: " + id);
        }
    }
}
