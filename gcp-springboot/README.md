# GCP Spring Boot Integration

A production-ready Spring Boot 3 application demonstrating all four major GCP integrations:
**Pub/Sub · Cloud Spanner · Secret Manager · Cloud Storage (GCS)**

---

## Project Structure

```
gcp-springboot/
├── src/main/java/com/navya/gcp/
│   ├── pubsub/          # Publisher, Subscriber, OrderMessageHandler, Controller
│   ├── spanner/         # Patient entity, Repository, Service, Controller
│   ├── secretmanager/   # SecretManagerService, SecretManagerConfig
│   ├── storage/         # GcsStorageService, GcsController
│   └── config/          # GlobalExceptionHandler
├── k8s/
│   ├── configmap.yml
│   ├── deployment.yml
│   └── service.yml
├── Dockerfile
└── pom.xml
```

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.9+ |
| Docker | 24+ |
| kubectl | 1.28+ |
| gcloud CLI | latest |

---

## 1 — GCP Project Setup

```bash
gcloud config set project YOUR_PROJECT_ID

gcloud services enable \
  pubsub.googleapis.com \
  spanner.googleapis.com \
  secretmanager.googleapis.com \
  storage.googleapis.com \
  container.googleapis.com \
  containerregistry.googleapis.com
```

---

## 2 — Service Account and Credentials

```bash
gcloud iam service-accounts create gcp-springboot-sa \
  --display-name="GCP Spring Boot SA"

SA="gcp-springboot-sa@YOUR_PROJECT_ID.iam.gserviceaccount.com"

for ROLE in roles/pubsub.editor roles/spanner.databaseUser \
            roles/secretmanager.secretAccessor roles/storage.objectAdmin; do
  gcloud projects add-iam-policy-binding YOUR_PROJECT_ID \
    --member="serviceAccount:$SA" --role="$ROLE"
done

# Local dev only — use Workload Identity in prod
gcloud iam service-accounts keys create sa-key.json --iam-account=$SA
```

---

## 3 — Local Development

```bash
export GCP_PROJECT_ID=your-project-id
export GOOGLE_APPLICATION_CREDENTIALS=/path/to/sa-key.json
export SPANNER_INSTANCE_ID=my-instance
export SPANNER_DATABASE=my-database
export GCS_BUCKET_NAME=my-gcs-bucket

mvn spring-boot:run
```

### Pub/Sub Emulator (offline dev)

```bash
gcloud beta emulators pubsub start --project=YOUR_PROJECT_ID
export PUBSUB_EMULATOR_HOST=localhost:8085
mvn spring-boot:run
```

---

## 4 — Cloud Spanner Setup

```bash
gcloud spanner instances create my-instance \
  --config=regional-us-central1 --nodes=1

gcloud spanner databases create my-database --instance=my-instance

gcloud spanner databases ddl update my-database --instance=my-instance \
  --ddl="CREATE TABLE patients (
    patient_id STRING(36) NOT NULL,
    first_name STRING(100) NOT NULL,
    last_name  STRING(100) NOT NULL,
    email      STRING(200),
    date_of_birth DATE,
    status     STRING(20) NOT NULL
  ) PRIMARY KEY (patient_id);"
```

---

## 5 — GCS Bucket Setup

```bash
gsutil mb -p YOUR_PROJECT_ID -l US-CENTRAL1 gs://my-gcs-bucket
gsutil iam ch serviceAccount:$SA:objectAdmin gs://my-gcs-bucket
```

---

## 6 — Pub/Sub Topics and Subscriptions

```bash
gcloud pubsub topics create orders-topic
gcloud pubsub topics create notifications-topic
gcloud pubsub subscriptions create orders-subscription --topic=orders-topic
gcloud pubsub subscriptions create notifications-subscription --topic=notifications-topic
```

---

## 7 — GitHub Actions Secrets

Go to **GitHub repo > Settings > Secrets and variables > Actions** and add:

| Secret | Description |
|---|---|
| `GCP_PROJECT_ID` | Your GCP project ID |
| `GCP_SA_KEY` | Full contents of sa-key.json |
| `GCP_REGION` | e.g. us-central1 |
| `GKE_CLUSTER_NAME` | Your GKE cluster name |
| `SPANNER_INSTANCE_ID` | Spanner instance name |
| `SPANNER_DATABASE` | Spanner database name |
| `GCS_BUCKET_NAME` | GCS bucket name |

---

## 8 — GKE Deployment

```bash
gcloud container clusters create my-cluster \
  --region=us-central1 --num-nodes=2 --machine-type=e2-standard-2

gcloud container clusters get-credentials my-cluster --region=us-central1

kubectl create secret generic gcp-sa-key-secret --from-file=key.json=./sa-key.json

kubectl apply -f k8s/
kubectl rollout status deployment/gcp-springboot
```

---

## 9 — API Endpoints

**Pub/Sub**
- `POST /api/pubsub/publish/orders`
- `POST /api/pubsub/publish/notify`

**Patients (Spanner)**
- `POST   /api/patients`
- `GET    /api/patients/{id}`
- `GET    /api/patients?status=ACTIVE`
- `PATCH  /api/patients/{id}/status`
- `DELETE /api/patients/{id}`

**GCS Storage**
- `POST   /api/storage/upload`
- `GET    /api/storage/download/{name}`
- `GET    /api/storage/signed-url/{name}`
- `GET    /api/storage/list`
- `DELETE /api/storage/{name}`

---

## 10 — Run Tests

```bash
mvn test
```

---

## Security Notes

- Never commit `sa-key.json` — add it to `.gitignore`
- For production GKE, prefer **Workload Identity** over mounted SA keys
- Inject all secrets via GCP Secret Manager, not plain environment variables
