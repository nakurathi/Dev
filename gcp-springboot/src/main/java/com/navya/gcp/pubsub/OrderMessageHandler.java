package com.navya.gcp.pubsub;

import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handles incoming order messages from the Pub/Sub orders subscription.
 *
 * <p>Extend this class to add business logic such as:
 * <ul>
 *   <li>Deserializing JSON to domain objects</li>
 *   <li>Persisting to Spanner</li>
 *   <li>Triggering downstream events</li>
 * </ul>
 */
@Slf4j
@Component
public class OrderMessageHandler {

    /**
     * Processes an order payload received from Pub/Sub.
     *
     * <p>ACKs on success, NACKs on exception. The NACKed message will be
     * redelivered based on the subscription's ack deadline settings.
     *
     * @param payload raw message body
     * @param message the acknowledgeble Pub/Sub message wrapper
     */
    public void handle(String payload, BasicAcknowledgeablePubsubMessage message) {
        try {
            log.info("Processing order: {}", payload);

            // TODO: deserialize and process
            // Order order = objectMapper.readValue(payload, Order.class);
            // orderRepository.save(order);

            message.ack();
            log.info("Order processed and ACKed successfully.");

        } catch (Exception e) {
            log.error("Order processing error: {}", e.getMessage(), e);
            message.nack(); // message will be redelivered
        }
    }
}
