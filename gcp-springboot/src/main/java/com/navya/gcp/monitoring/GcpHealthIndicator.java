package com.navya.gcp.monitoring;

import com.google.cloud.storage.Storage;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Composite health indicator that probes all GCP dependencies.
 * Exposed at: {@code GET /actuator/health/gcpServices}
 *
 * <p>Checks:
 * <ul>
 *   <li>GCS — list bucket operation (read-only, lightweight)</li>
 *   <li>Pub/Sub — template availability (no network call needed)</li>
 * </ul>
 *
 * <p>In production, wire this to alerting so GCP degradation is detected
 * before clients start seeing errors.
 */
@Slf4j
@Component("gcpServices")
@RequiredArgsConstructor
public class GcpHealthIndicator implements HealthIndicator {

    private final Storage storage;
    private final PubSubTemplate pubSubTemplate;

    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        boolean allHealthy = true;

        // ── GCS ──────────────────────────────────────────────────────────────
        try {
            storage.list();   // lightweight HEAD-equivalent
            builder.withDetail("gcs", "UP");
        } catch (Exception ex) {
            log.warn("GCS health check failed: {}", ex.getMessage());
            builder.withDetail("gcs", "DOWN: " + ex.getMessage());
            allHealthy = false;
        }

        // ── Pub/Sub ───────────────────────────────────────────────────────────
        try {
            // PubSubTemplate is Spring-managed — if it's null, context is broken
            boolean pubsubOk = pubSubTemplate != null;
            builder.withDetail("pubsub", pubsubOk ? "UP" : "DOWN");
            if (!pubsubOk) allHealthy = false;
        } catch (Exception ex) {
            builder.withDetail("pubsub", "DOWN: " + ex.getMessage());
            allHealthy = false;
        }

        return allHealthy ? builder.build() : builder.down().build();
    }
}
