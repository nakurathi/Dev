package com.navya.gcp.pubsub;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller exposing Pub/Sub publish endpoints.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>POST /api/pubsub/publish/orders   — publish an order message</li>
 *   <li>POST /api/pubsub/publish/notify   — publish a notification message</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/pubsub")
@RequiredArgsConstructor
public class PubSubController {

    private final PubSubPublisherService publisherService;

    @Value("${pubsub.topic.orders}")
    private String ordersTopic;

    @Value("${pubsub.topic.notifications}")
    private String notificationsTopic;

    @PostMapping("/publish/orders")
    public CompletableFuture<ResponseEntity<String>> publishOrder(
            @RequestBody String payload,
            @RequestParam(required = false, defaultValue = "api") String source) {

        return publisherService.publish(ordersTopic, payload, Map.of("source", source))
                .thenApply(msgId -> ResponseEntity.ok("Published order messageId=" + msgId));
    }

    @PostMapping("/publish/notify")
    public CompletableFuture<ResponseEntity<String>> publishNotification(
            @RequestBody String payload) {

        return publisherService.publish(notificationsTopic, payload)
                .thenApply(msgId -> ResponseEntity.ok("Published notification messageId=" + msgId));
    }
}
