package com.navya.gcp.pubsub;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.concurrent.SettableListenableFuture;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PubSubPublisherServiceTest {

    @Mock
    private PubSubTemplate pubSubTemplate;

    @InjectMocks
    private PubSubPublisherService publisherService;

    private SettableListenableFuture<String> future;

    @BeforeEach
    void setUp() {
        future = new SettableListenableFuture<>();
    }

    @Test
    @DisplayName("publish() resolves with message ID on success")
    void publish_success() throws Exception {
        future.set("msg-id-123");
        when(pubSubTemplate.publish(eq("test-topic"), eq("hello"), anyMap()))
                .thenReturn(future);

        CompletableFuture<String> result = publisherService.publish("test-topic", "hello");
        String msgId = result.get();

        assertThat(msgId).isEqualTo("msg-id-123");
        verify(pubSubTemplate).publish("test-topic", "hello", Map.of());
    }

    @Test
    @DisplayName("publish() with attributes passes them to PubSubTemplate")
    void publish_withAttributes() throws Exception {
        future.set("msg-id-456");
        Map<String, String> attrs = Map.of("source", "checkout");
        when(pubSubTemplate.publish("orders-topic", "{}", attrs)).thenReturn(future);

        CompletableFuture<String> result = publisherService.publish("orders-topic", "{}", attrs);

        assertThat(result.get()).isEqualTo("msg-id-456");
        verify(pubSubTemplate).publish("orders-topic", "{}", attrs);
    }

    @Test
    @DisplayName("publish() completes exceptionally on template failure")
    void publish_failure() {
        future.setException(new RuntimeException("GCP unavailable"));
        when(pubSubTemplate.publish(anyString(), anyString(), anyMap()))
                .thenReturn(future);

        CompletableFuture<String> result = publisherService.publish("topic", "msg");

        assertThat(result).isCompletedExceptionally();
    }
}
