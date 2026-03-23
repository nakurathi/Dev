package com.navya.gcp.spanner;

import com.google.cloud.spring.data.spanner.core.SpannerTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service layer for Cloud Spanner Patient operations.
 *
 * <p>Demonstrates:
 * <ul>
 *   <li>Standard CRUD via Spring Data {@link PatientRepository}</li>
 *   <li>Transactional writes using {@code @Transactional}</li>
 *   <li>Direct SpannerTemplate usage for custom SQL</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final SpannerTemplate spannerTemplate;

    /** Create a new patient, auto-generating a UUID primary key. */
    @Transactional
    public Patient createPatient(Patient patient) {
        if (patient.getPatientId() == null || patient.getPatientId().isBlank()) {
            patient.setPatientId(UUID.randomUUID().toString());
        }
        Patient saved = patientRepository.save(patient);
        log.info("Created patient patientId={}", saved.getPatientId());
        return saved;
    }

    /** Retrieve a patient by ID; returns empty if not found. */
    public Optional<Patient> findById(String patientId) {
        return patientRepository.findById(patientId);
    }

    /** Return all ACTIVE patients. */
    public List<Patient> findActivePatients() {
        return patientRepository.findByStatus("ACTIVE");
    }

    /** Update patient status (e.g., ACTIVE → INACTIVE). */
    @Transactional
    public Patient updateStatus(String patientId, String newStatus) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));
        patient.setStatus(newStatus);
        Patient updated = patientRepository.save(patient);
        log.info("Updated patient={} status={}", patientId, newStatus);
        return updated;
    }

    /** Delete patient by ID. */
    @Transactional
    public void deletePatient(String patientId) {
        patientRepository.deleteById(patientId);
        log.info("Deleted patient patientId={}", patientId);
    }

    /**
     * Example of raw SQL via SpannerTemplate for analytics queries
     * (not possible with standard Spring Data methods).
     */
    public long countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM patients WHERE status = @status";
        Long count = spannerTemplate.query(
                rs -> rs.getLong(0),
                com.google.cloud.spanner.Statement.newBuilder(sql)
                        .bind("status").to(status)
                        .build(),
                null
        ).stream().findFirst().orElse(0L);
        log.debug("countByStatus={} result={}", status, count);
        return count;
    }
}
