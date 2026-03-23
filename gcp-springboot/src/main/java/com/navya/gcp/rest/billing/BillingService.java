package com.navya.gcp.rest.billing;

import com.navya.gcp.domain.dto.Dtos.*;
import com.navya.gcp.domain.event.DomainEvent;
import com.navya.gcp.domain.model.Billing;
import com.navya.gcp.kafka.producer.DomainEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Billing service handling invoice creation and payment processing.
 *
 * <p>On payment: persists status update to Spanner and publishes
 * {@link DomainEvent.PaymentReceived} to Kafka for downstream reconciliation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private final BillingRepository billingRepository;
    private final DomainEventProducer eventProducer;

    @Transactional
    public InvoiceResponse createInvoice(InvoiceRequest request) {
        Billing billing = Billing.builder()
                .invoiceId(UUID.randomUUID().toString())
                .patientId(request.patientId())
                .appointmentId(request.appointmentId())
                .amount(request.amount())
                .dueDate(request.dueDate())
                .status("PENDING")
                .createdAt(Instant.now())
                .build();

        Billing saved = billingRepository.save(billing);
        log.info("Invoice created: invoiceId={} patientId={} amount={}",
                saved.getInvoiceId(), saved.getPatientId(), saved.getAmount());

        DomainEvent event = new DomainEvent.InvoiceGenerated(
                UUID.randomUUID().toString(), Instant.now(),
                saved.getInvoiceId(), saved.getPatientId(),
                saved.getAmount(), saved.getDueDate());
        eventProducer.publish(event);

        return toResponse(saved);
    }

    @Transactional
    public InvoiceResponse processPayment(PaymentRequest request) {
        Billing billing = billingRepository.findById(request.invoiceId())
                .orElseThrow(() -> new InvoiceNotFoundException(request.invoiceId()));

        if ("PAID".equals(billing.getStatus())) {
            throw new IllegalStateException("Invoice already paid: " + request.invoiceId());
        }

        billing.setStatus("PAID");
        billing.setPaidAt(Instant.now());
        Billing updated = billingRepository.save(billing);

        DomainEvent event = new DomainEvent.PaymentReceived(
                UUID.randomUUID().toString(), Instant.now(),
                updated.getInvoiceId(), updated.getPatientId(),
                request.amountPaid(), updated.getPaidAt());
        eventProducer.publish(event);

        log.info("Payment processed: invoiceId={} amount={}", updated.getInvoiceId(), request.amountPaid());
        return toResponse(updated);
    }

    public Optional<InvoiceResponse> findById(String invoiceId) {
        return billingRepository.findById(invoiceId).map(this::toResponse);
    }

    public List<InvoiceResponse> findByPatient(String patientId) {
        return billingRepository.findByPatientId(patientId)
                .stream().map(this::toResponse).toList();
    }

    private InvoiceResponse toResponse(Billing b) {
        return new InvoiceResponse(b.getInvoiceId(), b.getPatientId(), b.getAppointmentId(),
                b.getAmount(), b.getStatus(), b.getDueDate(), b.getPaidAt(), b.getCreatedAt());
    }

    public static class InvoiceNotFoundException extends RuntimeException {
        public InvoiceNotFoundException(String id) { super("Invoice not found: " + id); }
    }
}
