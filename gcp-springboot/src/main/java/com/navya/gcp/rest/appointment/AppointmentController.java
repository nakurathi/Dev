package com.navya.gcp.rest.appointment;

import com.navya.gcp.domain.dto.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Appointment resource.
 *
 * <p>Endpoints:
 * <pre>
 *   POST   /api/v1/appointments                    — schedule appointment
 *   GET    /api/v1/appointments/{id}               — get by ID
 *   GET    /api/v1/appointments?patientId=...       — list by patient
 *   PATCH  /api/v1/appointments/{id}/status        — update status
 *   DELETE /api/v1/appointments/{id}               — cancel
 * </pre>
 *
 * All responses wrapped in {@link ApiResponse} for consistent structure.
 */
@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponse>> schedule(
            @Valid @RequestBody AppointmentRequest request) {

        AppointmentResponse response = appointmentService.schedule(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Appointment scheduled successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getById(@PathVariable String id) {
        return appointmentService.findById(id)
                .map(appt -> ResponseEntity.ok(ApiResponse.ok(appt)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Appointment not found: " + id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> listByPatient(
            @RequestParam String patientId) {

        List<AppointmentResponse> appointments = appointmentService.findByPatient(patientId);
        return ResponseEntity.ok(ApiResponse.ok(appointments));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody AppointmentStatusUpdate update) {

        AppointmentResponse updated = appointmentService.updateStatus(id, update);
        return ResponseEntity.ok(ApiResponse.ok("Status updated to " + update.status(), updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable String id) {
        appointmentService.updateStatus(id,
                new AppointmentStatusUpdate("CANCELLED", "Cancelled via API"));
        return ResponseEntity.ok(ApiResponse.ok("Appointment cancelled", null));
    }
}
