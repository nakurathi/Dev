package com.navya.gcp.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.navya.gcp.storage.GcsStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Append-only audit log writer that buffers audit entries in memory and
 * flushes them to GCS on a scheduled interval.
 *
 * <p>Pattern: Write-behind buffer → GCS NDJSON (Newline Delimited JSON) files.
 * Each flush creates a timestamped file under {@code audit/YYYY/MM/DD/}.
 *
 * <p>Useful for:
 * <ul>
 *   <li>HIPAA audit trail compliance (patient data access logs)</li>
 *   <li>Feeding BigQuery via GCS external table for analytics</li>
 *   <li>Long-term retention (GCS lifecycle to Coldline after 90 days)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final GcsStorageService gcsStorageService;
    private final ObjectMapper      objectMapper;

    @Value("${audit.gcs.prefix:audit}")
    private String auditPrefix;

    // Thread-safe buffer — CopyOnWriteArrayList is safe for concurrent appends
    private final CopyOnWriteArrayList<AuditEntry> buffer = new CopyOnWriteArrayList<>();

    private static final DateTimeFormatter PATH_FMT =
            DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC);

    /**
     * Record an audit event asynchronously (non-blocking to caller).
     *
     * @param action     the operation performed (e.g., APPOINTMENT_CREATED)
     * @param entityType domain entity type (e.g., PATIENT, APPOINTMENT)
     * @param entityId   ID of the affected entity
     * @param actorId    user or service account that performed the action
     */
    @Async
    public void record(String action, String entityType, String entityId, String actorId) {
        AuditEntry entry = new AuditEntry(
                UUID.randomUUID().toString(),
                Instant.now(),
                action,
                entityType,
                entityId,
                actorId
        );
        buffer.add(entry);
        log.debug("Audit buffered: action={} entity={}/{}", action, entityType, entityId);
    }

    /**
     * Flush buffered audit entries to GCS every 30 seconds.
     * Uses a drain-and-clear pattern to avoid losing entries added during flush.
     */
    @Scheduled(fixedDelayString = "${audit.flush.interval-ms:30000}")
    public void flushToGcs() {
        if (buffer.isEmpty()) return;

        List<AuditEntry> toFlush = List.copyOf(buffer);
        buffer.removeAll(toFlush);

        try {
            // Build NDJSON content (one JSON object per line)
            StringBuilder ndjson = new StringBuilder();
            for (AuditEntry entry : toFlush) {
                ndjson.append(objectMapper.writeValueAsString(entry)).append("\n");
            }

            String datePath  = PATH_FMT.format(Instant.now());
            String objectKey = String.format("%s/%s/%s.ndjson",
                    auditPrefix, datePath, Instant.now().toEpochMilli());

            gcsStorageService.uploadBytes(ndjson.toString().getBytes(), objectKey, "application/x-ndjson");
            log.info("Flushed {} audit entries to GCS: {}", toFlush.size(), objectKey);

        } catch (Exception ex) {
            // Re-add on failure — entries won't be lost
            buffer.addAll(toFlush);
            log.error("Audit GCS flush failed, {} entries re-queued: {}", toFlush.size(), ex.getMessage());
        }
    }

    /** Immutable audit entry — Java 17 record. */
    public record AuditEntry(
            String  auditId,
            Instant timestamp,
            String  action,
            String  entityType,
            String  entityId,
            String  actorId
    ) {}
}
