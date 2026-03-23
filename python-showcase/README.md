# Python Showcase — Navya Akurathi
**Senior Java Full Stack Developer | 10+ Years Experience | United States**

Intermediate Python skills demonstrated across four domains:

| Module | Tech | What it shows |
|---|---|---|
| `api/` | FastAPI, Pydantic v2 | REST endpoints, request validation, async handlers |
| `data/` | pandas, NumPy | Data cleaning, transformation, aggregation |
| `ml/` | scikit-learn, joblib | Train/predict pipeline, model persistence |
| `kafka/` | kafka-python | Producer/consumer with JSON serialization |
| `gcp/` | GCS, BigQuery, Pub/Sub | Cloud storage, analytics queries, messaging |
| `tests/` | pytest, httpx | Unit + API tests with fixtures |

---

## Run Locally

```bash
cd python-showcase
python -m venv venv
source venv/bin/activate        # Windows: venv\Scripts\activate
pip install -r requirements.txt

# Start the API
uvicorn api.main:app --reload --port 8001

# Run tests
pytest tests/ -v
```

## Environment Variables (.env)
```
GCP_PROJECT_ID=your-project-id
GCS_BUCKET_NAME=your-bucket
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```
