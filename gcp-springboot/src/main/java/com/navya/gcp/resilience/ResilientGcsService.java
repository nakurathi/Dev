package com.navya.gcp.resilience;

import com.navya.gcp.storage.GcsStorageService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

/**
 * Resilient wrapper around {@link GcsStorageService}.
 *
 * <p>Decorates every GCS call with:
 * <ol>
 *   <li>Circuit Breaker — stops cascading failures when GCS is unhealthy</li>
 *   <li>Retry — retries transient network errors up to 3 times</li>
 * </ol>
 *
 * <p>Uses Resilience4j's functional {@link Decorators} builder pattern —
 * no AOP proxy required, fully explicit and testable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResilientGcsService {

    private final GcsStorageService gcsStorageService;
    private final CircuitBreaker    gcsCircuitBreaker;
    private final Retry             spannerRetry;       // reuse retry config for GCS too

    public String upload(MultipartFile file, String objectName) throws IOException {
        Supplier<String> decorated = Decorators
                .ofSupplier(() -> {
                    try {
                        return gcsStorageService.upload(file, objectName);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .withCircuitBreaker(gcsCircuitBreaker)
                .withRetry(spannerRetry)
                .withFallback(
                    List.of(RuntimeException.class),
                    ex -> {
                        log.error("GCS upload failed after retries: {}", ex.getMessage());
                        return "UPLOAD_FAILED_FALLBACK";
                    })
                .decorate();

        return decorated.get();
    }

    public byte[] download(String objectName) {
        Supplier<byte[]> decorated = Decorators
                .ofSupplier(() -> gcsStorageService.download(objectName))
                .withCircuitBreaker(gcsCircuitBreaker)
                .withRetry(spannerRetry)
                .withFallback(
                    List.of(RuntimeException.class),
                    ex -> {
                        log.warn("GCS download fallback for {}: {}", objectName, ex.getMessage());
                        return new byte[0];
                    })
                .decorate();

        return decorated.get();
    }

    public List<String> listObjects(String prefix) {
        Supplier<List<String>> decorated = Decorators
                .ofSupplier(() -> gcsStorageService.listObjects(prefix))
                .withCircuitBreaker(gcsCircuitBreaker)
                .withFallback(
                    List.of(RuntimeException.class),
                    ex -> {
                        log.warn("GCS list fallback: {}", ex.getMessage());
                        return List.of();
                    })
                .decorate();

        return decorated.get();
    }
}
