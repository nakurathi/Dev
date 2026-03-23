package com.navya.gcp.kafka.config;

import com.navya.gcp.domain.event.DomainEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Producer configuration.
 *
 * <p>Tuned for production on GCP (Cloud Managed Kafka / Confluent):
 * <ul>
 *   <li>idempotent producer — exactly-once delivery guarantees</li>
 *   <li>acks=all — all in-sync replicas must confirm</li>
 *   <li>retries + backoff — resilient to transient broker failures</li>
 *   <li>compression — snappy for throughput on high-volume topics</li>
 *   <li>batch + linger — optimized for batching without sacrificing latency</li>
 * </ul>
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.producer.client-id:gcp-springboot-producer}")
    private String clientId;

    @Bean
    public ProducerFactory<String, DomainEvent> domainEventProducerFactory() {
        return new DefaultKafkaProducerFactory<>(producerProps());
    }

    @Bean
    public KafkaTemplate<String, DomainEvent> kafkaTemplate(
            ProducerFactory<String, DomainEvent> factory) {
        KafkaTemplate<String, DomainEvent> template = new KafkaTemplate<>(factory);
        template.setObservationEnabled(true);  // Micrometer tracing
        return template;
    }

    private Map<String, Object> producerProps() {
        Map<String, Object> props = new HashMap<>();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);

        // Serializers
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Reliability — idempotent exactly-once
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // Backoff between retries
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 300L);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120_000);  // 2 min

        // Performance tuning
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 32 * 1024);         // 32 KB batch
        props.put(ProducerConfig.LINGER_MS_CONFIG, 5);                  // wait 5ms to fill batch
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 64 * 1024 * 1024L); // 64 MB buffer

        // Metadata refresh
        props.put(ProducerConfig.METADATA_MAX_AGE_CONFIG, 300_000);

        return props;
    }
}
