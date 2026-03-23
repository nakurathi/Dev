package com.navya.gcp.domain.model;

import com.google.cloud.spring.data.spanner.core.mapping.Column;
import com.google.cloud.spring.data.spanner.core.mapping.PrimaryKey;
import com.google.cloud.spring.data.spanner.core.mapping.Table;
import lombok.*;

import java.time.Instant;

/**
 * Cloud Spanner entity for an Appointment.
 *
 * DDL:
 * <pre>
 * CREATE TABLE appointments (
 *   appointment_id  STRING(36)  NOT NULL,
 *   patient_id      STRING(36)  NOT NULL,
 *   doctor_id       STRING(36)  NOT NULL,
 *   scheduled_at    TIMESTAMP   NOT NULL,
 *   status          STRING(20)  NOT NULL,
 *   notes           STRING(MAX),
 *   created_at      TIMESTAMP   NOT NULL,
 *   updated_at      TIMESTAMP
 * ) PRIMARY KEY (appointment_id);
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "appointments")
public class Appointment {

    @PrimaryKey
    @Column(name = "appointment_id")
    private String appointmentId;

    @Column(name = "patient_id")
    private String patientId;

    @Column(name = "doctor_id")
    private String doctorId;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    /** SCHEDULED | CONFIRMED | CANCELLED | COMPLETED */
    @Column(name = "status")
    private String status;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
