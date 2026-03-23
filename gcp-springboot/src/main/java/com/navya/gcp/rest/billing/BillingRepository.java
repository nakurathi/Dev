package com.navya.gcp.rest.billing;

import com.navya.gcp.domain.model.Billing;
import com.google.cloud.spring.data.spanner.repository.SpannerRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingRepository extends SpannerRepository<Billing, String> {
    List<Billing> findByPatientId(String patientId);
    List<Billing> findByStatus(String status);
    List<Billing> findByPatientIdAndStatus(String patientId, String status);
}
