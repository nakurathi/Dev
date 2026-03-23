"""
tests/test_ml.py
----------------
Unit tests for the ReadmissionModel.
Demonstrates: fixture for trained model, boundary testing, prediction correctness.
"""
import pytest
from api.models import ReadmissionFeatures, RiskLevel
from ml.readmission_model import ReadmissionModel


@pytest.fixture(scope="module")
def trained_model():
    """Train once and reuse across all tests in this module."""
    model = ReadmissionModel()
    model.train()
    return model


def test_model_trains_successfully(trained_model):
    assert trained_model.is_trained is True


def test_train_returns_metrics():
    model   = ReadmissionModel()
    metrics = model.train()
    assert "accuracy" in metrics
    assert "feature_importances" in metrics
    assert metrics["accuracy"] > 0.5    # better than random
    assert metrics["training_samples"] > 0


def test_predict_low_risk(trained_model):
    """Young healthy patient should have low risk."""
    features = ReadmissionFeatures(
        age=25, num_prior_admissions=0, num_medications=1,
        num_diagnoses=1, length_of_stay_days=2,
        has_diabetes=False, has_hypertension=False, has_heart_disease=False,
    )
    prediction = trained_model.predict("P001", features)
    assert 0.0 <= prediction.risk_score <= 1.0
    assert prediction.risk_level in [RiskLevel.LOW, RiskLevel.MEDIUM]
    assert len(prediction.top_risk_factors) == 3


def test_predict_high_risk(trained_model):
    """Elderly patient with multiple conditions should have higher risk."""
    features = ReadmissionFeatures(
        age=82, num_prior_admissions=5, num_medications=15,
        num_diagnoses=8, length_of_stay_days=18,
        has_diabetes=True, has_hypertension=True, has_heart_disease=True,
    )
    prediction = trained_model.predict("P002", features)
    assert prediction.risk_score > 0.3    # should be higher risk


def test_predict_without_training_raises():
    model = ReadmissionModel.__new__(ReadmissionModel)
    model._pipeline      = None
    model._model_version = "v0.0"
    features = ReadmissionFeatures(
        age=50, num_prior_admissions=1, num_medications=3,
        num_diagnoses=2, length_of_stay_days=5,
    )
    with pytest.raises(RuntimeError, match="not trained"):
        model.predict("P003", features)


def test_risk_score_in_range(trained_model):
    features = ReadmissionFeatures(
        age=60, num_prior_admissions=2, num_medications=8,
        num_diagnoses=4, length_of_stay_days=7, has_diabetes=True,
    )
    pred = trained_model.predict("P004", features)
    assert 0.0 <= pred.risk_score <= 1.0
