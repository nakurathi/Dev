"""
api/routes/analytics.py
------------------------
Analytics endpoints powered by the data and ML modules.
Demonstrates: calling pandas pipelines from FastAPI,
returning aggregated stats, ML prediction endpoint.
"""
from fastapi import APIRouter, HTTPException
from api.models import ReadmissionFeatures, ReadmissionPrediction, ApiResponse
from data.patient_analytics import PatientAnalytics
from ml.readmission_model import ReadmissionModel

router   = APIRouter()
_model   = ReadmissionModel()
_analytics = PatientAnalytics()


@router.get("/dashboard", response_model=ApiResponse[dict])
async def dashboard_stats():
    """Return aggregated dashboard stats using pandas."""
    stats = _analytics.get_summary_stats()
    return ApiResponse.ok(stats, "Dashboard stats loaded")


@router.get("/risk-distribution", response_model=ApiResponse[dict])
async def risk_distribution():
    """Return patient risk level distribution using pandas groupby."""
    dist = _analytics.get_risk_distribution()
    return ApiResponse.ok(dist, "Risk distribution loaded")


@router.get("/admission-trends", response_model=ApiResponse[list])
async def admission_trends(days: int = 30):
    """Return daily admission counts for the last N days."""
    trends = _analytics.get_admission_trends(days=days)
    return ApiResponse.ok(trends, f"Trends for last {days} days")


@router.post("/predict/readmission", response_model=ApiResponse[ReadmissionPrediction])
async def predict_readmission(patient_id: str, features: ReadmissionFeatures):
    """
    Predict 30-day hospital readmission risk.
    Uses a trained RandomForestClassifier under the hood.
    """
    if not _model.is_trained:
        # Auto-train on synthetic data if not yet trained
        _model.train()

    prediction = _model.predict(patient_id=patient_id, features=features)
    return ApiResponse.ok(prediction, "Prediction generated successfully")


@router.post("/model/train", response_model=ApiResponse[dict])
async def train_model():
    """Trigger model retraining on synthetic data."""
    metrics = _model.train()
    return ApiResponse.ok(metrics, "Model trained successfully")
