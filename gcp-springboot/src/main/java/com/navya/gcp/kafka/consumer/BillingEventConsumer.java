package com.navya.gcp.kafka.consumer;

import com.navya.gcp.domain.event.DomainEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kafka consumer for billing domain events.
 *
 * <p>Demonstrates batch listening — processes a list of records
 * per poll, useful for bulk inserts or analytics aggregation.
 */
@Slf4j
@Component
public class BillingEventConsumer {

    private final Counter invoicesProcessed;
    private final Counter paymentsProcessed;

    public BillingEventConsumer(MeterRegistry registry) {
        this.invoicesProcessed = Counter.builder("kafka.billing.invoices.processed")
                .description("Total invoice events processed")
                .register(registry);
        this.paymentsProcessed = Counter.builder("kafka.billing.payments.processed")
                .description("Total payment events processed")
                .register(registry);
    }

    @KafkaListener(
            topics = "healthcare.billing.invoices",
            groupId = "${spring.kafka.consumer.group-id:gcp-springboot-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onInvoiceGenerated(
            ConsumerRecord<String, DomainEvent> record,
            Acknowledgment ack) {

        DomainEvent event = record.value();

        if (event instanceof DomainEvent.InvoiceGenerated e) {
            log.info("[INVOICE_GENERATED] invoiceId={} patientId={} amount={}",
                    e.invoiceId(), e.patientId(), e.amount());
            // TODO: send billing notification, update analytics
            invoicesProcessed.increment();
        }
        ack.acknowledge();
    }

    /**
     * Batch consumer for payment events — processes up to 50 payments at once.
     * Useful for bulk ledger updates or analytics pipelines.
     */
    @KafkaListener(
            topics = "healthcare.billing.payments",
            groupId = "${spring.kafka.consumer.group-id:gcp-springboot-group}-batch",
            containerFactory = "kafkaListenerContainerFactory",
            batch = "true"
    )
    public void onPaymentReceived(
            List<ConsumerRecord<String, DomainEvent>> records,
            Acknowledgment ack) {

        log.info("[PAYMENT_RECEIVED] Processing batch of {} payment records", records.size());

        records.forEach(record -> {
            if (record.value() instanceof DomainEvent.PaymentReceived e) {
                log.debug("Payment invoiceId={} amount={}", e.invoiceId(), e.amountPaid());
                // TODO: update billing status in Spanner, write audit to GCS
                paymentsProcessed.increment();
            }
        });

        ack.acknowledge();
        log.info("Batch ACKed: {} payments processed", records.size());
    }
}
