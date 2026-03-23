package com.navya.gcp.rest.billing;

import com.navya.gcp.domain.dto.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Billing resource.
 *
 * <p>Endpoints:
 * <pre>
 *   POST  /api/v1/billing/invoices              — create invoice
 *   GET   /api/v1/billing/invoices/{id}         — get invoice by ID
 *   GET   /api/v1/billing/invoices?patientId=.. — list by patient
 *   POST  /api/v1/billing/payments              — process payment
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @PostMapping("/invoices")
    public ResponseEntity<ApiResponse<InvoiceResponse>> createInvoice(
            @Valid @RequestBody InvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Invoice created", billingService.createInvoice(request)));
    }

    @GetMapping("/invoices/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getById(@PathVariable String id) {
        return billingService.findById(id)
                .map(inv -> ResponseEntity.ok(ApiResponse.ok(inv)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Invoice not found: " + id)));
    }

    @GetMapping("/invoices")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> listByPatient(
            @RequestParam String patientId) {
        return ResponseEntity.ok(ApiResponse.ok(billingService.findByPatient(patientId)));
    }

    @PostMapping("/payments")
    public ResponseEntity<ApiResponse<InvoiceResponse>> processPayment(
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok("Payment processed", billingService.processPayment(request)));
    }
}
