package com.navya.gcp.rest;

import com.navya.gcp.domain.dto.Dtos.*;
import com.navya.gcp.domain.event.DomainEvent;
import com.navya.gcp.domain.model.Appointment;
import com.navya.gcp.kafka.producer.DomainEventProducer;
import com.navya.gcp.rest.appointment.AppointmentRepository;
import com.navya.gcp.rest.appointment.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock AppointmentRepository appointmentRepository;
    @Mock DomainEventProducer   eventProducer;

    @InjectMocks AppointmentService appointmentService;

    private AppointmentRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new AppointmentRequest(
                "patient-001", "doctor-001",
                Instant.now().plusSeconds(3600),
                "Annual checkup");
    }

    @Test
    @DisplayName("schedule() persists appointment and publishes AppointmentScheduled event")
    void schedule_persistsAndPublishesEvent() {
        Appointment saved = Appointment.builder()
                .appointmentId("appt-001")
                .patientId("patient-001")
                .doctorId("doctor-001")
                .scheduledAt(validRequest.scheduledAt())
                .status("SCHEDULED")
                .createdAt(Instant.now())
                .build();

        when(appointmentRepository.save(any(Appointment.class))).thenReturn(saved);

        AppointmentResponse response = appointmentService.schedule(validRequest);

        // Verify persistence
        verify(appointmentRepository).save(any(Appointment.class));

        // Verify Kafka event type and payload
        ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventProducer).publish(eventCaptor.capture());

        DomainEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent).isInstanceOf(DomainEvent.AppointmentScheduled.class);

        DomainEvent.AppointmentScheduled scheduled = (DomainEvent.AppointmentScheduled) publishedEvent;
        assertThat(scheduled.patientId()).isEqualTo("patient-001");
        assertThat(scheduled.appointmentId()).isEqualTo("appt-001");

        // Verify response DTO
        assertThat(response.appointmentId()).isEqualTo("appt-001");
        assertThat(response.status()).isEqualTo("SCHEDULED");
    }

    @Test
    @DisplayName("updateStatus() to CANCELLED publishes AppointmentCancelled event")
    void updateStatus_cancelled_publishesCancelledEvent() {
        Appointment existing = Appointment.builder()
                .appointmentId("appt-002")
                .patientId("patient-002")
                .status("SCHEDULED")
                .createdAt(Instant.now())
                .build();

        when(appointmentRepository.findById("appt-002")).thenReturn(Optional.of(existing));
        when(appointmentRepository.save(any())).thenReturn(existing);

        appointmentService.updateStatus("appt-002",
                new AppointmentStatusUpdate("CANCELLED", "Patient request"));

        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventProducer).publish(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(DomainEvent.AppointmentCancelled.class);
    }

    @Test
    @DisplayName("updateStatus() to CONFIRMED does NOT publish event")
    void updateStatus_confirmed_noEvent() {
        Appointment existing = Appointment.builder()
                .appointmentId("appt-003").patientId("p-003")
                .status("SCHEDULED").createdAt(Instant.now()).build();

        when(appointmentRepository.findById("appt-003")).thenReturn(Optional.of(existing));
        when(appointmentRepository.save(any())).thenReturn(existing);

        appointmentService.updateStatus("appt-003",
                new AppointmentStatusUpdate("CONFIRMED", null));

        verifyNoInteractions(eventProducer);
    }

    @Test
    @DisplayName("findById() returns empty Optional when not found")
    void findById_notFound_returnsEmpty() {
        when(appointmentRepository.findById("nonexistent")).thenReturn(Optional.empty());
        assertThat(appointmentService.findById("nonexistent")).isEmpty();
    }
}
