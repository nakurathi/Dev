package com.navya.gcp.domain.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Java 17 records as DTOs — immutable, concise, no boilerplate.
 * Validation annotations work on record components.
 */
public final class Dtos {

    private Dtos() {}

    // ── Appointment DTOs ──────────────────────────────────────────────────────

    public record AppointmentRequest(
            @NotBlank(message = "patientId is required")
            String patientId,

            @NotBlank(message = "doctorId is required")
            String doctorId,

            @NotNull(message = "scheduledAt is required")
            @Future(message = "scheduledAt must be in the future")
            Instant scheduledAt,

            String notes
    ) {}

    public record AppointmentResponse(
            String appointmentId,
            String patientId,
            String doctorId,
            Instant scheduledAt,
            String status,
            String notes,
            Instant createdAt
    ) {}

    public record AppointmentStatusUpdate(
            @NotBlank
            @Pattern(regexp = "CONFIRMED|CANCELLED|COMPLETED",
                     message = "status must be CONFIRMED, CANCELLED, or COMPLETED")
            String status,
            String reason
    ) {}

    // ── Billing DTOs ──────────────────────────────────────────────────────────

    public record InvoiceRequest(
            @NotBlank String patientId,
            String appointmentId,

            @NotNull @DecimalMin(value = "0.01", message = "amount must be positive")
            BigDecimal amount,

            @NotNull Instant dueDate
    ) {}

    public record InvoiceResponse(
            String invoiceId,
            String patientId,
            String appointmentId,
            BigDecimal amount,
            String status,
            Instant dueDate,
            Instant paidAt,
            Instant createdAt
    ) {}

    public record PaymentRequest(
            @NotBlank String invoiceId,
            @NotNull @DecimalMin("0.01") BigDecimal amountPaid
    ) {}

    // ── Generic API responses ─────────────────────────────────────────────────

    public record ApiResponse<T>(
            boolean success,
            String message,
            T data
    ) {
        public static <T> ApiResponse<T> ok(T data) {
            return new ApiResponse<>(true, "OK", data);
        }

        public static <T> ApiResponse<T> ok(String message, T data) {
            return new ApiResponse<>(true, message, data);
        }

        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, message, null);
        }
    }

    public record PagedResponse<T>(
            java.util.List<T> content,
            int page,
            int size,
            long totalElements
    ) {}

    // ── Kafka event DTOs ──────────────────────────────────────────────────────

    public record KafkaMessageMeta(
            String topic,
            int partition,
            long offset,
            Instant timestamp
    ) {}
}
