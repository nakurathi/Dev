"""
gcp/gcp_services.py
--------------------
GCP service integrations: GCS, BigQuery, Pub/Sub.

Demonstrates:
- GCS: upload, download, generate signed URL, list objects
- BigQuery: parameterized queries, streaming inserts
- Pub/Sub: publish with attributes, subscribe with callback
"""
import json
import logging
import os
from datetime import datetime, timedelta
from typing import Any, Iterator

logger = logging.getLogger(__name__)

PROJECT_ID  = os.getenv("GCP_PROJECT_ID", "your-gcp-project")
BUCKET_NAME = os.getenv("GCS_BUCKET_NAME", "healthcare-analytics-bucket")
BQ_DATASET  = os.getenv("BIGQUERY_DATASET", "healthcare_analytics")


# ═══════════════════════════════════════════════════════════════════════════════
# GCS Storage Service
# ═══════════════════════════════════════════════════════════════════════════════

class GCSService:
    """
    Google Cloud Storage operations.
    Requires: GOOGLE_APPLICATION_CREDENTIALS env var pointing to service account key.
    """

    def __init__(self, bucket_name: str = BUCKET_NAME):
        from google.cloud import storage
        self._client      = storage.Client(project=PROJECT_ID)
        self._bucket_name = bucket_name
        self._bucket      = self._client.bucket(bucket_name)

    def upload_bytes(self, data: bytes, blob_name: str, content_type: str = "application/octet-stream") -> str:
        """Upload raw bytes to GCS. Returns the GCS URI."""
        blob = self._bucket.blob(blob_name)
        blob.upload_from_string(data, content_type=content_type)
        gcs_uri = f"gs://{self._bucket_name}/{blob_name}"
        logger.info("Uploaded to GCS: %s", gcs_uri)
        return gcs_uri

    def upload_json(self, data: dict | list, blob_name: str) -> str:
        """Serialize dict/list to JSON and upload to GCS."""
        payload = json.dumps(data, default=str, indent=2).encode("utf-8")
        return self.upload_bytes(payload, blob_name, content_type="application/json")

    def download_bytes(self, blob_name: str) -> bytes:
        """Download a GCS object as raw bytes."""
        blob = self._bucket.blob(blob_name)
        data = blob.download_as_bytes()
        logger.info("Downloaded from GCS: gs://%s/%s (%d bytes)", self._bucket_name, blob_name, len(data))
        return data

    def download_json(self, blob_name: str) -> dict | list:
        """Download and deserialize a JSON object from GCS."""
        return json.loads(self.download_bytes(blob_name))

    def generate_signed_url(self, blob_name: str, expiry_minutes: int = 15) -> str:
        """Generate a V4 signed URL for temporary browser access."""
        from google.cloud import storage
        blob       = self._bucket.blob(blob_name)
        expiration = timedelta(minutes=expiry_minutes)
        url        = blob.generate_signed_url(expiration=expiration, version="v4")
        logger.info("Signed URL generated for %s (expires %d min)", blob_name, expiry_minutes)
        return url

    def list_objects(self, prefix: str = "") -> list[str]:
        """List all objects under a GCS prefix (folder)."""
        blobs = self._client.list_blobs(self._bucket_name, prefix=prefix)
        return [blob.name for blob in blobs]

    def delete(self, blob_name: str) -> bool:
        """Delete a GCS object. Returns True if deleted, False if not found."""
        blob = self._bucket.blob(blob_name)
        if blob.exists():
            blob.delete()
            logger.info("Deleted GCS object: %s", blob_name)
            return True
        return False


# ═══════════════════════════════════════════════════════════════════════════════
# BigQuery Service
# ═══════════════════════════════════════════════════════════════════════════════

class BigQueryService:
    """
    BigQuery query execution and streaming inserts.
    Uses parameterized queries to prevent SQL injection.
    """

    def __init__(self, dataset: str = BQ_DATASET):
        from google.cloud import bigquery
        self._client  = bigquery.Client(project=PROJECT_ID)
        self._dataset = dataset

    def query(self, sql: str, params: dict[str, Any] | None = None) -> list[dict]:
        """
        Execute a parameterized BigQuery SQL query.

        Example:
            rows = bq.query(
                "SELECT * FROM `healthcare_analytics.patients` WHERE status = @status LIMIT @limit",
                params={"status": "ACTIVE", "limit": 100}
            )
        """
        from google.cloud import bigquery

        job_config = bigquery.QueryJobConfig()
        if params:
            job_config.query_parameters = [
                bigquery.ScalarQueryParameter(k, self._bq_type(v), v)
                for k, v in params.items()
            ]

        logger.info("Running BigQuery: %s", sql[:100])
        job    = self._client.query(sql, job_config=job_config)
        result = job.result()
        return [dict(row) for row in result]

    def stream_insert(self, table_id: str, rows: list[dict]) -> None:
        """
        Stream rows into a BigQuery table using the streaming insert API.
        Best for near-real-time ingestion (latency ~1s, available immediately for queries).
        """
        table_ref = f"{PROJECT_ID}.{self._dataset}.{table_id}"
        errors    = self._client.insert_rows_json(table_ref, rows)

        if errors:
            logger.error("BigQuery stream insert errors: %s", errors)
            raise RuntimeError(f"BigQuery insert failed: {errors}")

        logger.info("Inserted %d rows into %s", len(rows), table_ref)

    def get_patient_stats(self) -> list[dict]:
        """Example analytics query — patient count by status and risk level."""
        sql = f"""
            SELECT
                status,
                risk_level,
                COUNT(*) AS patient_count,
                AVG(age)  AS avg_age
            FROM `{PROJECT_ID}.{self._dataset}.patients`
            GROUP BY status, risk_level
            ORDER BY patient_count DESC
        """
        return self.query(sql)

    def get_readmission_trend(self, days: int = 30) -> list[dict]:
        """Readmission rate trend over the last N days."""
        sql = f"""
            SELECT
                DATE(admitted_at)  AS admission_date,
                COUNT(*)           AS total_admissions,
                COUNTIF(readmitted) AS readmissions,
                ROUND(COUNTIF(readmitted) / COUNT(*) * 100, 2) AS readmission_rate_pct
            FROM `{PROJECT_ID}.{self._dataset}.admissions`
            WHERE admitted_at >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL @days DAY)
            GROUP BY admission_date
            ORDER BY admission_date
        """
        return self.query(sql, params={"days": days})

    def _bq_type(self, value: Any) -> str:
        """Map Python types to BigQuery scalar types."""
        if isinstance(value, bool):   return "BOOL"
        if isinstance(value, int):    return "INT64"
        if isinstance(value, float):  return "FLOAT64"
        if isinstance(value, datetime): return "TIMESTAMP"
        return "STRING"


# ═══════════════════════════════════════════════════════════════════════════════
# Pub/Sub Service
# ═══════════════════════════════════════════════════════════════════════════════

class PubSubService:
    """
    GCP Pub/Sub publisher and subscriber.
    Pub/Sub is GCP's managed message bus — similar to Kafka but fully managed.
    """

    def __init__(self):
        from google.cloud import pubsub_v1
        self._publisher  = pubsub_v1.PublisherClient()
        self._subscriber = pubsub_v1.SubscriberClient()

    def publish(self, topic_id: str, data: dict, attributes: dict[str, str] | None = None) -> str:
        """
        Publish a JSON message to a Pub/Sub topic.
        Attributes are key-value metadata for filtering (e.g. event_type=PATIENT_REGISTERED).
        Returns the message ID assigned by GCP.
        """
        topic_path = self._publisher.topic_path(PROJECT_ID, topic_id)
        payload    = json.dumps(data, default=str).encode("utf-8")

        future = self._publisher.publish(
            topic_path,
            data=payload,
            **(attributes or {}),
        )
        msg_id = future.result()
        logger.info("Published to Pub/Sub topic=%s messageId=%s", topic_id, msg_id)
        return msg_id

    def subscribe(
        self,
        subscription_id: str,
        callback,
        timeout_seconds: float = 30.0,
    ) -> None:
        """
        Pull messages from a subscription with a callback.
        callback(message) must call message.ack() on success or message.nack() on failure.
        """
        from google.cloud import pubsub_v1
        subscription_path = self._subscriber.subscription_path(PROJECT_ID, subscription_id)

        with self._subscriber as subscriber:
            future = subscriber.subscribe(subscription_path, callback=callback)
            logger.info("Listening on subscription: %s", subscription_path)
            try:
                future.result(timeout=timeout_seconds)
            except Exception:
                future.cancel()
                logger.info("Pub/Sub subscription stopped")

    @staticmethod
    def make_ack_callback(handler):
        """
        Wrap a handler function as a Pub/Sub callback that auto-ACKs on success
        and NACKs on exception.
        """
        def callback(message):
            try:
                data = json.loads(message.data.decode("utf-8"))
                handler(data)
                message.ack()
            except Exception as exc:
                logger.error("Pub/Sub handler failed: %s — NACKing", exc)
                message.nack()
        return callback
