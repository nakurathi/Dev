package com.navya.gcp.rest.appointment;

import com.navya.gcp.domain.model.Appointment;
import com.google.cloud.spring.data.spanner.repository.SpannerRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AppointmentRepository extends SpannerRepository<Appointment, String> {
    List<Appointment> findByPatientId(String patientId);
    List<Appointment> findByDoctorId(String doctorId);
    List<Appointment> findByStatus(String status);
    List<Appointment> findByPatientIdAndStatus(String patientId, String status);
}
