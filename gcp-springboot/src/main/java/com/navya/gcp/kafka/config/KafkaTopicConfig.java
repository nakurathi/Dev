package com.navya.gcp.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declarative Kafka topic provisioning.
 *
 * <p>Spring Boot auto-creates topics via {@link NewTopic} beans if
 * {@code spring.kafka.admin.auto-create} is enabled (default: true).
 *
 * <p>Topics follow the convention: {@code domain.entity.event}
 * Corresponding DLTs are auto-named:  {@code domain.entity.event.DLT}
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${kafka.topics.partitions:3}")
    private int partitions;

    @Value("${kafka.topics.replicas:3}")
    private short replicas;

    // ── Appointment topics ────────────────────────────────────────────────────

    @Bean
    public NewTopic appointmentsScheduled() {
        return TopicBuilder.name("healthcare.appointments.scheduled")
                .partitions(partitions).replicas(replicas)
                .config("retention.ms", "604800000")   // 7 days
                .build();
    }

    @Bean
    public NewTopic appointmentsCancelled() {
        return TopicBuilder.name("healthcare.appointments.cancelled")
                .partitions(partitions).replicas(replicas).build();
    }

    // ── Patient topics ────────────────────────────────────────────────────────

    @Bean
    public NewTopic patientsRegistered() {
        return TopicBuilder.name("healthcare.patients.registered")
                .partitions(partitions).replicas(replicas).build();
    }

    // ── Billing topics ────────────────────────────────────────────────────────

    @Bean
    public NewTopic billingInvoices() {
        return TopicBuilder.name("healthcare.billing.invoices")
                .partitions(partitions).replicas(replicas)
                .config("retention.ms", "2592000000")  // 30 days
                .build();
    }

    @Bean
    public NewTopic billingPayments() {
        return TopicBuilder.name("healthcare.billing.payments")
                .partitions(partitions).replicas(replicas)
                .config("retention.ms", "2592000000")  // 30 days
                .build();
    }

    // ── Audit / notification topics ───────────────────────────────────────────

    @Bean
    public NewTopic auditEvents() {
        return TopicBuilder.name("healthcare.audit.events")
                .partitions(partitions).replicas(replicas)
                .config("retention.ms", "7776000000")  // 90 days
                .build();
    }
}
