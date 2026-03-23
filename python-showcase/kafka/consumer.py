"""
kafka/consumer.py
-----------------
Kafka consumer for healthcare events.

Demonstrates:
- kafka-python KafkaConsumer with JSON deserialization
- Manual offset commit for at-least-once delivery
- Graceful shutdown via signal handling
- Dead letter queue pattern for poison messages
- Pluggable handler dispatch by event_type
"""
import json
import logging
import signal
from typing import Callable
from kafka import KafkaConsumer
from kafka.errors import KafkaError

logger = logging.getLogger(__name__)

# Handler type: receives the event dict, returns nothing
EventHandler = Callable[[dict], None]


class HealthcareEventConsumer:
    """
    Kafka consumer that routes events to registered handlers by event_type.

    Usage:
        consumer = HealthcareEventConsumer(topics=["healthcare.patients"])
        consumer.register_handler("PATIENT_REGISTERED", handle_patient)
        consumer.start()   # blocks — runs until SIGTERM/SIGINT
    """

    def __init__(
        self,
        topics: list[str],
        bootstrap_servers: str  = "localhost:9092",
        group_id: str           = "healthcare-analytics-group",
    ):
        self._consumer = KafkaConsumer(
            *topics,
            bootstrap_servers  = bootstrap_servers,
            group_id           = group_id,
            # JSON deserialization
            value_deserializer = lambda b: json.loads(b.decode("utf-8")),
            key_deserializer   = lambda b: b.decode("utf-8") if b else None,
            # Manual offset commit — only commit after successful processing
            enable_auto_commit = False,
            auto_offset_reset  = "earliest",
            max_poll_records   = 50,
            session_timeout_ms = 30000,
        )
        self._handlers:  dict[str, EventHandler] = {}
        self._running    = False
        self._dlq:       list[dict] = []   # dead letter queue (in-memory for demo)

        # Graceful shutdown on SIGTERM / SIGINT
        signal.signal(signal.SIGTERM, self._shutdown)
        signal.signal(signal.SIGINT,  self._shutdown)

    def register_handler(self, event_type: str, handler: EventHandler) -> None:
        """Register a handler function for a specific event_type."""
        self._handlers[event_type] = handler
        logger.info("Registered handler for event_type=%s", event_type)

    def start(self) -> None:
        """
        Start polling loop. Blocks until stop() is called.
        Processes each message, commits offset on success, routes to DLQ on failure.
        """
        self._running = True
        logger.info("Consumer started — waiting for messages...")

        try:
            for message in self._consumer:
                if not self._running:
                    break

                event = message.value
                event_type = event.get("event_type", "UNKNOWN")
                logger.info(
                    "Received event_type=%s partition=%d offset=%d",
                    event_type, message.partition, message.offset,
                )

                try:
                    handler = self._handlers.get(event_type)
                    if handler:
                        handler(event)
                    else:
                        logger.warning("No handler registered for event_type=%s", event_type)

                    # Manual ACK — commit offset only after successful processing
                    self._consumer.commit()

                except Exception as exc:
                    logger.error(
                        "Handler failed for event_type=%s error=%s — routing to DLQ",
                        event_type, exc,
                    )
                    self._dlq.append({"event": event, "error": str(exc)})
                    # Still commit to avoid infinite retry on poison messages
                    self._consumer.commit()

        finally:
            self._consumer.close()
            logger.info("Consumer closed. DLQ size: %d", len(self._dlq))

    def stop(self) -> None:
        self._running = False

    def get_dlq(self) -> list[dict]:
        """Return dead-lettered messages for inspection."""
        return self._dlq.copy()

    def _shutdown(self, *_) -> None:
        logger.info("Shutdown signal received — stopping consumer gracefully")
        self.stop()


# ── Example handlers ──────────────────────────────────────────────────────────

def handle_patient_registered(event: dict) -> None:
    """Example handler — log and process new patient registration."""
    payload = event.get("payload", {})
    logger.info("New patient registered: %s %s", payload.get("first_name"), payload.get("last_name"))
    # TODO: trigger welcome email, create analytics record, etc.


def handle_appointment_scheduled(event: dict) -> None:
    """Example handler — send confirmation notification."""
    payload = event.get("payload", {})
    logger.info(
        "Appointment scheduled: patient=%s doctor=%s at=%s",
        payload.get("patient_id"), payload.get("doctor_id"), payload.get("scheduled_at"),
    )
    # TODO: send SMS/email confirmation via notification service


def handle_ml_prediction(event: dict) -> None:
    """Example handler — store prediction for reporting."""
    payload = event.get("payload", {})
    logger.info(
        "ML prediction received: patient=%s risk=%s score=%.2f",
        payload.get("patient_id"),
        payload.get("risk_level"),
        payload.get("risk_score", 0),
    )
    # TODO: store to BigQuery for reporting dashboard
