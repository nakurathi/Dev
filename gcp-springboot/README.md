# GCP Spring Boot Integration

A production-ready Spring Boot 3.x project demonstrating four core **Google Cloud Platform** integrations — ideal for Senior Java Full Stack portfolios targeting fintech and healthcare domains.

---

## Modules

| Module | Description |
|--------|-------------|
| **Pub/Sub** | Async message publishing & subscribing with ACK/NACK handling |
| **Cloud Spanner** | Horizontally scalable relational DB with Spring Data repository |
| **Secret Manager** | Secure runtime secret injection via `sm://` property source |
| **GCS (Cloud Storage)** | File upload, download, signed URL generation, and listing |

---

## Tech Stack

- Java 17, Spring Boot 3.2
- Spring Cloud GCP 5.0
- Google Cloud Pub/Sub, Spanner, Secret Manager, Storage
- Lombok, JUnit 5, Mockito
- Maven

---

## Getting Started

### Prerequisites
- GCP project with billing enabled
- Service account JSON with roles: `pubsub.editor`, `spanner.databaseUser`, `secretmanager.secretAccessor`, `storage.objectAdmin`
- `GOOGLE_APPLICATION_CREDENTIALS` env var pointing to the JSON file

### Configuration

Copy and edit `src/main/resources/application.yml`:

```yaml
spring:
  cloud:
    gcp:
      project-id: your-gcp-project-id

gcs:
  bucket-name: your-gcs-bucket

pubsub:
  topic:
    orders: orders-topic
  subscription:
    orders: orders-subscription
```

### Run

```bash
mvn spring-boot:run
```

---

## API Reference

### Pub/Sub
```
POST /api/pubsub/publish/orders    — publish order event
POST /api/pubsub/publish/notify    — publish notification
```

### Cloud Spanner (Patients)
```
POST   /api/patients               — create patient
GET    /api/patients/{id}          — get by ID
GET    /api/patients?status=ACTIVE — list by status
PATCH  /api/patients/{id}/status   — update status
DELETE /api/patients/{id}          — delete patient
```

### GCS Storage
```
POST   /api/storage/upload              — upload file (multipart)
GET    /api/storage/download/{name}     — download file
GET    /api/storage/signed-url/{name}   — get signed URL
GET    /api/storage/list                — list objects
DELETE /api/storage/{name}              — delete object
```

---

## Project Structure

```
src/main/java/com/navya/gcp/
├── G