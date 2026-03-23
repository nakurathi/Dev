package com.navya.gcp.pubsub;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for publishing messages to GCP Pub/Sub topics.
 *
 * <p>Usage pattern:
 * <pre>
 *   publisher.publish("orders-topic", payload, Map.of("source", "checkout"));
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PubSubPublisherService {

    private final PubSubTemplate pubSubTemplate;

    /**
     * Publishes a plain text message to a Pub/Sub topic.
     *
     * @param topicName  the Pub/Sub topic name
     * @param payload    the message body as a string
     * @return message ID assigned by GCP
     */
    public CompletableFuture<String> publish(String topicName, String payload) {
        return publish(topicName, payload, Map.of());
    }

    /**
     * Publishes a message with custom attributes to a Pub/Sub topic.
     *
     * @param topicName  the Pub/Sub topic name
     * @param payload    the message body as a string
     * @param attributes key-value metadata attached to the message
     * @return message ID assigned by GCP
     */
    public CompletableFuture<String> publish(String topicName,
                                              String payload,
                                              Map<String, String> attributes) {
        log.debug("Publishing to topic={} payload={} attributes={}", topicName, payload, attributes);

        ListenableFuture<String> future = pubSubTemplate.publish(topicName, payload, attributes);

        return future.completable()
                .whenComplete((msgId, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish to topic={}: {}", topicName, ex.getMessage(), ex);
                    } else {
                        log.info("Published messageId={} to topic={}", msgId, topicName);
                    }
                });
    }
}
