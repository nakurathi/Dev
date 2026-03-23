package com.navya.gcp.monitoring;

import io.micrometer.core.instrument.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom Micrometer metrics exported to Cloud Monitoring (via Prometheus scrape
 * or the GCP Micrometer registry).
 *
 * <p>Registers:
 * <ul>
 *   <li>{@code app.appointments.scheduled.total}   — counter</li>
 *   <li>{@code app.appointments.cancelled.total}   — counter</li>
 *   <li>{@code app.billing.invoices.generated}     — counter</li>
 *   <li>{@code app.billing.payments.processed}     — counter</li>
 *   <li>{@code app.kafka.publish.duration.seconds} — timer</li>
 *   <li>{@code app.kafka.consumer.lag}             — gauge (simulated)</li>
 *   <li>{@code app.gcs.upload.bytes}               — distribution summary</li>
 * </ul>
 */
@Slf4j
@Component
public class AppMetrics implements HealthIndicator {

    // Counters
    private final Counter appointmentsScheduled;
    private final Counter appointmentsCancelled;
    private final Counter invoicesGenerated;
    private final Counter paymentsProcessed;
    private final Counter kafkaPublishErrors;

    // Timer — Kafka publish latency
    private final Timer kafkaPublishTimer;

    // Distribution summary — GCS upload sizes
    private final DistributionSummary gcsUploadBytes;

    // Gauge backing values
    private final AtomicInteger activeAppointments = new AtomicInteger(0);
    private final AtomicLong    kafkaConsumerLag   = new AtomicLong(0L);

    public AppMetrics(MeterRegistry registry) {

        appointmentsScheduled = Counter.builder("app.appointments.scheduled.total")
                .description("Total appointments scheduled")
                .tag("service", "appointment")
                .register(registry);

        appointmentsCancelled = Counter.builder("app.appointments.cancelled.total")
                .description("Total appointments cancelled")
                .tag("service", "appointment")
                .register(registry);

        invoicesGenerated = Counter.builder("app.billing.invoices.generated")
                .description("Total invoices generated")
                .tag("service", "billing")
                .register(registry);

        paymentsProcessed = Counter.builder("app.billing.payments.processed")
                .description("Total payments processed")
                .tag("service", "billing")
                .register(registry);

        kafkaPublishErrors = Counter.builder("app.kafka.publish.errors.total")
                .description("Total Kafka publish failures")
                .tag("component", "kafka")
                .register(registry);

        kafkaPublishTimer = Timer.builder("app.kafka.publish.duration.seconds")
                .description("Kafka domain event publish latency")
                .publishPercentiles(0.5, 0.95, 0.99)
                .tag("component", "kafka")
                .register(registry);

        gcsUploadBytes = DistributionSummary.builder("app.gcs.upload.bytes")
                .description("GCS upload payload sizes in bytes")
                .baseUnit("bytes")
                .publishPercentiles(0.5, 0.95)
                .register(registry);

        // Gauges — observe AtomicInteger/Long directly
        Gauge.builder("app.appointments.active", activeAppointments, AtomicInteger::get)
                .description("Currently active (SCHEDULED/CONFIRMED) appointments")
                .register(registry);

        Gauge.builder("app.kafka.consumer.lag", kafkaConsumerLag, AtomicLong::get)
                .description("Estimated Kafka consumer lag")
                .tag("topic", "healthcare.appointments.scheduled")
                .register(registry);
    }

    // ── Public recording methods ──────────────────────────────────────────────

    public void recordAppointmentScheduled()  { appointmentsScheduled.increment(); activeAppointments.incrementAndGet(); }
    public void recordAppointmentCancelled()  { appointmentsCancelled.increment(); activeAppointments.decrementAndGet(); }
    public void recordInvoiceGenerated()      { invoicesGenerated.increment(); }
    public void recordPaymentProcessed()      { paymentsProcessed.increment(); }
    public void recordKafkaPublishError()     { kafkaPublishErrors.increment(); }
    public void recordKafkaPublishTime(Runnable action) { kafkaPublishTimer.record(action); }
    public void recordGcsUpload(long bytes)   { gcsUploadBytes.record(bytes); }
    public void updateConsumerLag(long lag)   { kafkaConsumerLag.set(lag); }

    // ── Health indicator — exposed at /actuator/health/appMetrics ────────────

    @Override
    public Health health() {
        double errorRate = kafkaPublishErrors.count();
        if (errorRate > 100) {
            return Health.down()
                    .withDetail("kafkaPublishErrors", errorRate)
                    .withDetail("reason", "High Kafka publish error rate")
                    .build();
        }
        return Health.up()
                .withDetail("appointmentsScheduled", appointmentsScheduled.count())
                .withDetail("invoicesGenerated",     invoicesGenerated.count())
                .withDetail("kafkaPublishErrors",    kafkaPublishErrors.count())
                .build();
    }
}
