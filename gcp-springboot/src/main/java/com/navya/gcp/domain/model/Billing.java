package com.navya.gcp.domain.model;

import com.google.cloud.spring.data.spanner.core.mapping.Column;
import com.google.cloud.spring.data.spanner.core.mapping.PrimaryKey;
import com.google.cloud.spring.data.spanner.core.mapping.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Cloud Spanner entity for a Billing record.
 *
 * DDL:
 * <pre>
 * CREATE TABLE billing (
 *   invoice_id      STRING(36)   NOT NULL,
 *   patient_id      STRING(36)   NOT NULL,
 *   appointment_id  STRING(36),
 *   amount          NUMERIC      NOT NULL,
 *   status          STRING(20)   NOT NULL,
 *   due_date        TIMESTAMP    NOT NULL,
 *   paid_at         TIMESTAMP,
 *   created_at      TIMESTAMP    NOT NULL
 * ) PRIMARY KEY (invoice_id);
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "billing")
public class Billing {

    @PrimaryKey
    @Column(name = "invoice_id")
    private String invoiceId;

    @Column(name = "patient_id")
    private String patientId;

    @Column(name = "appointment_id")
    private String appointmentId;

    @Column(name = "amount")
    private BigDecimal amount;

    /** PENDING | PAID | OVERDUE | CANCELLED */
    @Column(name = "status")
    private String status;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at")
    private Instant createdAt;
}
