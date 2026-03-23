package com.navya.gcp.batch;

import com.navya.gcp.pubsub.PubSubPublisherService;
import com.navya.gcp.rest.billing.BillingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Scheduled batch job that scans for overdue invoices and:
 * <ol>
 *   <li>Updates their status from PENDING → OVERDUE in Cloud Spanner</li>
 *   <li>Publishes an alert to GCP Pub/Sub for the notification service</li>
 * </ol>
 *
 * <p>Runs every day at 02:00 UTC via cron expression.
 * Designed to be idempotent — safe to re-run if it fails partway through.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueInvoiceJob {

    private final BillingRepository      billingRepository;
    private final PubSubPublisherService pubSubPublisher;

    @Value("${pubsub.topic.notifications}")
    private String notificationsTopic;

    /**
     * Runs daily at 02:00 UTC.
     * Cron format: second minute hour day-of-month month day-of-week
     */
    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    public void markOverdueInvoices() {
        log.info("[OverdueInvoiceJob] Starting overdue invoice scan at {}", Instant.now());

        try {
            // Fetch all PENDING invoices (Spanner query)
            var pending = billingRepository.findByStatus("PENDING");
            Instant now = Instant.now();

            long overdueCount = pending.stream()
                    .filter(invoice -> invoice.getDueDate() != null &&
                                       invoice.getDueDate().isBefore(now))
                    .peek(invoice -> {
                        invoice.setStatus("OVERDUE");
                        billingRepository.save(invoice);

                        // Notify via Pub/Sub
                        String alertPayload = String.format(
                                "{\"invoiceId\":\"%s\",\"patientId\":\"%s\",\"dueDate\":\"%s\"}",
                                invoice.getInvoiceId(), invoice.getPatientId(), invoice.getDueDate());

                        pubSubPublisher.publish(notificationsTopic, alertPayload,
                                Map.of("alertType", "OVERDUE_INVOICE", "severity", "MEDIUM"));

                        log.info("Marked OVERDUE: invoiceId={} patientId={}",
                                invoice.getInvoiceId(), invoice.getPatientId());
                    })
                    .count();

            log.info("[OverdueInvoiceJob] Completed: {} invoices marked OVERDUE", overdueCount);

        } catch (Exception ex) {
            log.error("[OverdueInvoiceJob] Failed: {}", ex.getMessage(), ex);
        }
    }
}
