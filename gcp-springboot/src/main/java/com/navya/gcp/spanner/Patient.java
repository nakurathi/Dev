package com.navya.gcp.spanner;

import com.google.cloud.spring.data.spanner.core.mapping.Column;
import com.google.cloud.spring.data.spanner.core.mapping.PrimaryKey;
import com.google.cloud.spring.data.spanner.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Cloud Spanner entity representing a Patient record.
 *
 * <p>DDL (run once via Spanner console or migration tool):
 * <pre>
 * CREATE TABLE patients (
 *   patient_id   STRING(36)  NOT NULL,
 *   first_name   STRING(100) NOT NULL,
 *   last_name    STRING(100) NOT NULL,
 *   email        STRING(200),
 *   date_of_birth DATE,
 *   status       STRING(20)  NOT NULL
 * ) PRIMARY KEY (patient_id);
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "patients")
public class Patient {

    @PrimaryKey
    @Column(name = "patient_id")
    private String patientId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "email")
    private String email;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "status")
    private String status;  // ACTIVE, INACTIVE, ARCHIVED
}
