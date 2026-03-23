package com.navya.gcp.spanner;

import com.google.cloud.spring.data.spanner.repository.SpannerRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data repository for Cloud Spanner Patient table.
 *
 * <p>Inherits standard CRUD from SpannerRepository. Add custom SPQL
 * queries as needed using {@code @Query} annotation.
 */
@Repository
public interface PatientRepository extends SpannerRepository<Patient, String> {

    /**
     * Find all patients by status (e.g., "ACTIVE").
     */
    List<Patient> findByStatus(String status);

    /**
     * Find patients matching last name (case-sensitive in Spanner).
     */
    List<Patient> findByLastName(String lastName);

    /**
     * Find patients by email address (expect 0 or 1 result).
     */
    List<Patient> findByEmail(String email);
}
