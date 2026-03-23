package com.navya.gcp.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navya.gcp.domain.dto.Dtos.*;
import com.navya.gcp.rest.appointment.AppointmentController;
import com.navya.gcp.rest.appointment.AppointmentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Spring MVC slice test for AppointmentController.
 *
 * <p>Uses {@code @WebMvcTest} — loads only the web layer (no Kafka, no GCP),
 * with {@code @MockBean} for the service. Fast and isolated.
 */
@WebMvcTest(AppointmentController.class)
class AppointmentControllerTest {

    @Autowired MockMvc          mockMvc;
    @Autowired ObjectMapper     objectMapper;
    @MockBean  AppointmentService appointmentService;

    private AppointmentResponse sampleResponse() {
        return new AppointmentResponse(
                "appt-001", "patient-001", "doctor-001",
                Instant.now().plusSeconds(3600), "SCHEDULED",
                "Annual checkup", Instant.now());
    }

    @Test
    @DisplayName("POST /api/v1/appointments returns 201 with appointment details")
    @WithMockUser(roles = "CLINICIAN")
    void schedule_returns201() throws Exception {
        when(appointmentService.schedule(any())).thenReturn(sampleResponse());

        AppointmentRequest request = new AppointmentRequest(
                "patient-001", "doctor-001",
                Instant.now().plusSeconds(7200), "Annual checkup");

        mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.appointmentId").value("appt-001"))
                .andExpect(jsonPath("$.data.status").value("SCHEDULED"));
    }

    @Test
    @DisplayName("GET /api/v1/appointments/{id} returns 200 when found")
    @WithMockUser
    void getById_found_returns200() throws Exception {
        when(appointmentService.findById("appt-001")).thenReturn(Optional.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/appointments/appt-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointmentId").value("appt-001"));
    }

    @Test
    @DisplayName("GET /api/v1/appointments/{id} returns 404 when not found")
    @WithMockUser
    void getById_notFound_returns404() throws Exception {
        when(appointmentService.findById("nonexistent")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/appointments/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/appointments?patientId returns list")
    @WithMockUser
    void listByPatient_returnsList() throws Exception {
        when(appointmentService.findByPatient("patient-001"))
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/appointments").param("patientId", "patient-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].patientId").value("patient-001"));
    }

    @Test
    @DisplayName("POST /api/v1/appointments with missing patientId returns 400")
    @WithMockUser(roles = "CLINICIAN")
    void schedule_missingPatientId_returns400() throws Exception {
        // Missing patientId in request
        String badJson = "{\"doctorId\":\"d-1\",\"scheduledAt\":\"" +
                Instant.now().plusSeconds(3600) + "\"}";

        mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(badJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/appointments without auth returns 401")
    void schedule_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
