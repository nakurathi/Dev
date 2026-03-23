package com.navya.gcp.spanner;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Cloud Spanner Patient CRUD operations.
 *
 * <p>Endpoints:
 * <pre>
 *   POST   /api/patients            — create patient
 *   GET    /api/patients/{id}       — get by ID
 *   GET    /api/patients?status=... — list by status
 *   PATCH  /api/patients/{id}/status — update status
 *   DELETE /api/patients/{id}       — delete patient
 * </pre>
 */
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    public ResponseEntity<Patient> create(@Valid @RequestBody Patient patient) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(patientService.createPatient(patient));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Patient> getById(@PathVariable String id) {
        return patientService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<Patient> listByStatus(
            @RequestParam(defaultValue = "ACTIVE") String status) {
        return patientService.findActivePatients();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Patient> updateStatus(
            @PathVariable String id,
            @RequestParam String status) {
        return ResponseEntity.ok(patientService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }
}
