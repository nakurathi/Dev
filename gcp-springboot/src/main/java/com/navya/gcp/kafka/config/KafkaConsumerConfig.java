package com.navya.gcp.kafka.config;

import com.navya.gcp.domain.event.DomainEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Consumer configuration.
 *
 * <p>Features:
 * <ul>
 *   <li>Manual offset commit (MANUAL_IMMEDIATE) — commit after processing</li>
 *   <li>3 retries with 1s backoff before routing to Dead Letter Topic (DLT)</li>
 *   <li>DLT pattern: original topic name + {@code .DLT}</li>
 *   <li>Concurrency=3 — 3 threads per consumer (tune to partition count)</li>
 *   <li>Type-safe JsonDeserializer with trusted packages</li>
 * </ul>
 */
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:gcp-springboot-group}")
    private String groupId;

    @Value("${spring.kafka.consumer.concurrency:3}")
    private int concurrency;

    @Bean
    public ConsumerFactory<String, DomainEvent> domainEventConsumerFactory() {
        JsonDeserializer<DomainEvent> deserializer = new JsonDeserializer<>(DomainEvent.class);
        deserializer.addTrustedPackages("com.navya.gcp.domain.event");
        deserializer.setUseTypeMapperForKey(false);

        return new DefaultKafkaConsumerFactory<>(
                consumerProps(),
                new StringDeserializer(),
                deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DomainEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, DomainEvent> consumerFactory,
            KafkaTemplate<String, DomainEvent> kafkaTemplate) {

        var factory = new ConcurrentKafkaListenerContainerFactory<String, DomainEvent>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(concurrency);
        factory.setObservationEnabled(true);  // Micrometer tracing

        // Manual offset commit — only ACK after successful processing
        factory.getContainerProperties()
               .setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Dead Letter Topic recovery: 3 retries, 1s apart, then -> topic.DLT
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        var errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    private Map<String, Object> consumerProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // Start from earliest on new consumer group
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Manual commits — disable auto commit
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // Fetch tuning
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300_000);  // 5 min

        // Session & heartbeat
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG,   30_000);
        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10_000);

        return props;
    }
}
