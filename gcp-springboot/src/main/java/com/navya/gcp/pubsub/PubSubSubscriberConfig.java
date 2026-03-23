package com.navya.gcp.pubsub;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures GCP Pub/Sub subscribers using push-based message listeners.
 *
 * <p>Each subscriber:
 * <ul>
 *   <li>Pulls messages from the configured subscription</li>
 *   <li>Processes and explicitly ACKs or NACKs</li>
 *   <li>Logs failures without crashing the listener thread</li>
 * </ul>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class PubSubSubscriberConfig {

    private final PubSubTemplate pubSubTemplate;

    @Value("${pubsub.subscription.orders}")
    private String ordersSubscription;

    @Value("${pubsub.subscription.notifications}")
    private String notificationsSubscription;

    /**
     * Subscribes to the orders subscription and delegates to OrderMessageHandler.
     */
    @Bean
    public Object ordersSubscriber(OrderMessageHandler handler) {
        pubSubTemplate.subscribe(ordersSubscription, message -> {
            try {
                String payload = message.getPubsubMessage().getData().toStringUtf8();
                log.info("[orders] Received: {}", payload);
                handler.handle(payload, message);
            } catch (Exception e) {
                log.error("[orders] Processing failed, NACKing message: {}", e.getMessage(), e);
                message.nack();
            }
        });
        log.info("Subscribed to orders subscription: {}", ordersSubscription);
        return new Object(); // placeholder bean
    }

    /**
     * Subscribes to the notifications subscription with simple ACK-on-receive.
     */
    @Bean
    public Object notificationsSubscriber() {
        pubSubTemplate.subscribe(notificationsSubscription, message -> {
            String payload = message.getPubsubMessage().getData().toStringUtf8();
            log.info("[notifications] Received: {}", payload);
            // TODO: plug in notification dispatch logic here
            message.ack();
        });
        log.info("Subscribed to notifications subscription: {}", notificationsSubscription);
        return new Object();
    }
}
