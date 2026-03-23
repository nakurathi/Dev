package com.navya.gcp.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Resilience4j configuration for circuit breakers and retries.
 *
 * <p>Circuit Breaker states:
 * <pre>
 *   CLOSED  ──(50% failure rate)──► OPEN ──(10s wait)──► HALF_OPEN ──(pass)──► CLOSED
 *                                                                     └──(fail)──► OPEN
 * </pre>
 *
 * <p>Named instances:
 * <ul>
 *   <li>{@code spannerCB}     — wraps Cloud Spanner calls</li>
 *   <li>{@code gcsCB}         — wraps GCS operations</li>
 *   <li>{@code secretMgrCB}   — wraps Secret Manager calls</li>
 *   <li>{@code kafkaRetry}    — retries transient Kafka publish failures</li>
 *   <li>{@code spannerRetry}  — retries transient Spanner errors</li>
 * </ul>
 */
@Slf4j
@Configuration
public class ResilienceConfig {

    // ── Circuit Breakers ──────────────────────────────────────────────────────

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)                        // open at 50% failures
                .slowCallRateThreshold(80)                       // open if 80% calls are slow
                .slowCallDurationThreshold(Duration.ofSeconds(3))
                .waitDurationInOpenState(Duration.ofSeconds(10)) // stay open for 10s
                .permittedNumberOfCallsInHalfOpenState(5)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(20)
                .minimumNumberOfCalls(5)
                .recordExceptions(IOException.class, TimeoutException.class, RuntimeException.class)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultConfig);

        // Register event listeners for observability
        registry.getEventPublisher()
                .onEntryAddedEvent(event -> {
                    CircuitBreaker cb = event.getAddedEntry();
                    cb.getEventPublisher()
                      .onStateTransition(e -> log.warn("[CircuitBreaker] {} transitioned {} -> {}",
                              cb.getName(),
                              e.getStateTransition().getFromState(),
                              e.getStateTransition().getToState()));
                });

        return registry;
    }

    @Bean
    public CircuitBreaker spannerCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("spannerCB");
    }

    @Bean
    public CircuitBreaker gcsCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("gcsCB");
    }

    @Bean
    public CircuitBreaker secretManagerCircuitBreaker(CircuitBreakerRegistry registry) {
        // Secret Manager: more lenient — it's read-only and critical for startup
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(70)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .minimumNumberOfCalls(3)
                .build();
        return registry.circuitBreaker("secretMgrCB", config);
    }

    // ── Retry Policies ────────────────────────────────────────────────────────

    @Bean
    public RetryRegistry retryRegistry() {
        return RetryRegistry.ofDefaults();
    }

    @Bean
    public Retry kafkaRetry(RetryRegistry registry) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryExceptions(IOException.class, RuntimeException.class)
                .ignoreExceptions(IllegalArgumentException.class, IllegalStateException.class)
                .build();
        return registry.retry("kafkaRetry", config);
    }

    @Bean
    public Retry spannerRetry(RetryRegistry registry) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(4)
                .intervalFunction(io.github.resilience4j.core.IntervalFunction.ofExponentialBackoff(
                        Duration.ofMillis(200), 2.0))   // 200ms, 400ms, 800ms
                .retryExceptions(IOException.class)
                .build();
        return registry.retry("spannerRetry", config);
    }
}
