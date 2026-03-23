"""
kafka/producer.py
-----------------
Kafka producer for healthcare domain events.

Demonstrates:
- kafka-python KafkaProducer with JSON serialization
- Key-based partitioning (patient_id) for ordered delivery
- Error callbacks and delivery confirmation
- Context manager pattern for safe resource cleanup
"""
import json
import logging
from datetime import datetime
from typing import Any, Callable
from kafka import KafkaProducer
from kafka.errors import KafkaError

logger = logging.getLogger(__name__)

# ── Topic constants ───────────────────────────────────────────────────────────
TOPIC_PATIENTS     = "healthcare.patients"
TOPIC_APPOINTMENTS = "healthcare.appointments"
TOPIC_PREDICTIONS  = "healthcare.ml.predictions"


class HealthcareEventProducer:
    """
    Kafka producer that publishes healthcare domain events as JSON.

    All events include:
    - event_id: unique UUID per message
    - event_type: string discriminator (e.g. PATIENT_REGISTERED)
    - occurred_at: ISO timestamp
    - payload: event-specific data dict
    """

    def __init__(self, bootstrap_servers: str = "localhost:9092"):
        self._producer = KafkaProducer(
            bootstrap_servers=bootstrap_servers,
            # Serialize keys and values as UTF-8 JSON
            key_serializer   = lambda k: k.encode("utf-8") if k else None,
            value_serializer = lambda v: json.dumps(v, default=str).encode("utf-8"),
            # Reliability settings
            acks             = "all",          # wait for all replicas
            retries          = 3,
            retry_backoff_ms = 300,
            # Performance
            compression_type = "gzip",
            batch_size       = 16384,
            linger_ms        = 5,
        )

    # ── Public API ─────────────────────────────────────────────────────────────

    def publish_patient_registered(self, patient_id: str, patient_data: dict) -> None:
        """Publish PATIENT_REGISTERED event partitioned by patient_id."""
        event = self._build_event("PATIENT_REGISTERED", patient_id, patient_data)
        self._send(TOPIC_PATIENTS, key=patient_id, event=event)

    def publish_appointment_scheduled(self, appointment_id: str, patient_id: str, data: dict) -> None:
        """Publish APPOINTMENT_SCHEDULED event. Partition key = patient_id for ordering."""
        event = self._build_event("APPOINTMENT_SCHEDULED", patient_id, data)
        self._send(TOPIC_APPOINTMENTS, key=patient_id, event=event)

    def publish_ml_prediction(self, patient_id: str, prediction: dict) -> None:
        """Publish ML prediction result for downstream analytics consumers."""
        event = self._build_event("READMISSION_PREDICTION", patient_id, prediction)
        self._send(TOPIC_PREDICTIONS, key=patient_id, event=event)

    def flush(self) -> None:
        """Block until all buffered messages are delivered."""
        self._producer.flush()

    def close(self) -> None:
        self._producer.close()

    # ── Context manager support ────────────────────────────────────────────────

    def __enter__(self):
        return self

    def __exit__(self, *_):
        self.flush()
        self.close()

    # ── Helpers ────────────────────────────────────────────────────────────────

    def _build_event(self, event_type: str, entity_id: str, payload: dict) -> dict:
        import uuid
        return {
            "event_id":    str(uuid.uuid4()),
            "event_type":  event_type,
            "entity_id":   entity_id,
            "occurred_at": datetime.utcnow().isoformat(),
            "payload":     payload,
        }

    def _send(self, topic: str, key: str, event: dict) -> None:
        def on_success(metadata):
            logger.info(
                "Kafka delivered event=%s topic=%s partition=%d offset=%d",
                event["event_type"], topic, metadata.partition, metadata.offset,
            )

        def on_error(exc: KafkaError):
            logger.error("Kafka delivery failed event=%s: %s", event["event_type"], exc)

        self._producer.send(topic, key=key, value=event) \
                      .add_callback(on_success) \
                      .add_errback(on_error)
